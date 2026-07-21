/* Copyright (c) 2009 Christoph Studer <chstuder@gmail.com>
 * Copyright (c) 2010 Jan Berkel <jan.berkel@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zegoggles.smssync.service;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.zegoggles.smssync.mail.DataType;
import com.zegoggles.smssync.preferences.Preferences;

import java.util.concurrent.TimeUnit;

import static com.zegoggles.smssync.App.LOCAL_LOGV;
import static com.zegoggles.smssync.App.TAG;
import static com.zegoggles.smssync.Consts.CALLLOG_PROVIDER;
import static com.zegoggles.smssync.Consts.MMS_PROVIDER;
import static com.zegoggles.smssync.Consts.SMS_PROVIDER;
import static com.zegoggles.smssync.service.BackupType.BROADCAST_INTENT;
import static com.zegoggles.smssync.service.BackupType.INCOMING;
import static com.zegoggles.smssync.service.BackupType.REGULAR;

/**
 * Schedules backups using {@link WorkManager}.
 */
public class BackupJobs {
    private static final int BOOT_BACKUP_DELAY = 60;
    // initial backoff, exponential: [ 30, 60, 120, 240, ... ] seconds
    private static final long BACKOFF_DELAY_SECONDS = 30;

    static final String CONTENT_TRIGGER_TAG = "contentTrigger";
    static final String DATA_BACKUP_TYPE = "backup_type";
    static final String DATA_CONTENT_TRIGGER = "content_trigger";

    private final Context context;
    private final Preferences preferences;

    public BackupJobs(Context context) {
        this(context, new Preferences(context));
    }

    BackupJobs(Context context, Preferences preferences) {
        this.context = context.getApplicationContext();
        this.preferences = preferences;
    }

    private WorkManager workManager() {
        // Lazy so constructing BackupJobs during Application#onCreate does not force init
        return WorkManager.getInstance(context);
    }

    public void scheduleIncoming() {
        schedule(preferences.getIncomingTimeoutSecs(), INCOMING, false);
    }

    public void scheduleRegular() {
        schedule(preferences.getRegularTimeoutSecs(), REGULAR, false);
    }

    public void scheduleBootup() {
        if (!preferences.isAutoBackupEnabled()) {
            Log.d(TAG, "auto backup no longer enabled, canceling all jobs");
            cancelAll();
        } else {
            schedule(BOOT_BACKUP_DELAY, REGULAR, false);
        }
    }

    public void scheduleImmediate() {
        schedule(-1, BROADCAST_INTENT, true);
    }

    public void scheduleContentTriggerJob() {
        scheduleContentTriggerJob(ExistingWorkPolicy.REPLACE);
    }

    /**
     * Ensure regular + ContentUriTrigger work exist without cancelling armed observers.
     * Used on process start and after backups finish so a cold start cannot wipe a
     * waiting ContentUriTrigger (which drops the SMS/MMS change that woke the app).
     */
    public void ensureAutoBackupJobs() {
        if (!preferences.isAutoBackupEnabled()) {
            Log.d(TAG, "auto backup disabled, canceling scheduled jobs");
            cancelAll();
            return;
        }
        schedule(preferences.getRegularTimeoutSecs(), REGULAR, false, ExistingWorkPolicy.KEEP);
        if (preferences.getIncomingTimeoutSecs() > 0) {
            scheduleContentTriggerJob(ExistingWorkPolicy.KEEP);
        }
    }

    private void scheduleContentTriggerJob(ExistingWorkPolicy policy) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            if (LOCAL_LOGV) Log.v(TAG, "content uri triggers not supported on this platform");
            return;
        }
        final OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SmsBackupWorker.class)
            .addTag(CONTENT_TRIGGER_TAG)
            .setConstraints(contentTriggerConstraints())
            .setInputData(new Data.Builder()
                .putString(DATA_BACKUP_TYPE, INCOMING.name())
                .putBoolean(DATA_CONTENT_TRIGGER, true)
                .build())
            .build();
        enqueue(CONTENT_TRIGGER_TAG, request, policy);
    }

    public void cancelAll() {
        cancelRegular();
        cancelContentUriTrigger();
    }

    public void cancelRegular() {
        cancel(REGULAR.name());
    }

    private void cancelContentUriTrigger() {
        cancel(CONTENT_TRIGGER_TAG);
    }

    private void cancel(String uniqueName) {
        if (LOCAL_LOGV) Log.v(TAG, "cancel(" + uniqueName + ")");
        workManager().cancelUniqueWork(uniqueName);
    }

    private void schedule(int inSeconds, BackupType backupType, boolean force) {
        schedule(inSeconds, backupType, force, existingWorkPolicy(backupType.name()));
    }

    private void schedule(int inSeconds, BackupType backupType, boolean force,
                          ExistingWorkPolicy policy) {
        if (LOCAL_LOGV) {
            Log.v(TAG, "scheduleBackup(" + inSeconds + ", " + backupType + ", " + force + ")");
        }

        if (force || (preferences.isAutoBackupEnabled() && inSeconds > 0)) {
            enqueue(backupType.name(), createRequest(inSeconds, backupType), policy);
            if (LOCAL_LOGV) {
                Log.v(TAG, "Scheduled backup job " + backupType + " due " +
                        (inSeconds > 0 ? "in " + inSeconds + " seconds" : "now"));
            }
        } else {
            if (LOCAL_LOGV) Log.v(TAG, "Not scheduling backup because auto backup is disabled.");
        }
    }

    private void enqueue(String uniqueName, OneTimeWorkRequest request) {
        enqueue(uniqueName, request, existingWorkPolicy(uniqueName));
    }

    private void enqueue(String uniqueName, OneTimeWorkRequest request,
                         ExistingWorkPolicy policy) {
        // INCOMING must use KEEP: REPLACE cancels an in-progress backup whenever another
        // SMS/MMS arrives (broadcast or ContentUriTrigger re-arm), so large backlogs never
        // finish. KEEP ignores the new request while work is enqueued/running; once it
        // finishes, the next message schedules a fresh delay. REGULAR and contentTrigger
        // still REPLACE by default so timers / one-shot URI observers can be re-armed;
        // ensureAutoBackupJobs() uses KEEP so process start does not wipe an armed observer.
        workManager().enqueueUniqueWork(uniqueName, policy, request);
    }

    private static ExistingWorkPolicy existingWorkPolicy(String uniqueName) {
        return INCOMING.name().equals(uniqueName)
                ? ExistingWorkPolicy.KEEP
                : ExistingWorkPolicy.REPLACE;
    }

    private @NonNull OneTimeWorkRequest createRequest(int inSeconds, BackupType backupType) {
        final OneTimeWorkRequest.Builder builder = new OneTimeWorkRequest.Builder(SmsBackupWorker.class)
            .addTag(backupType.name())
            .setConstraints(jobConstraints(backupType))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_SECONDS, TimeUnit.SECONDS)
            .setInputData(new Data.Builder().putString(DATA_BACKUP_TYPE, backupType.name()).build());

        if (inSeconds > 0) {
            builder.setInitialDelay(inSeconds, TimeUnit.SECONDS);
        }
        return builder.build();
    }

    private Constraints jobConstraints(BackupType backupType) {
        // Do not attach JobScheduler/WorkManager network constraints. On Samsung (and
        // some Android 14/15 builds) CONNECTIVITY stays unsatisfied for background UIDs
        // even on Wi‑Fi, and "Force batch connectivity jobs" defers network work for
        // minutes/hours — so incoming triggers queue but never run until the app is
        // opened. Connectivity / wifi-only is enforced in SmsBackupService instead.
        return Constraints.NONE;
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private Constraints contentTriggerConstraints() {
        // Watch both SMS and MMS: RCS (Google Messages et al.) often lands in
        // content://mms and would never wake a SMS-only ContentUriTrigger.
        // No network constraint: this job only re-arms and calls scheduleIncoming().
        final Constraints.Builder builder = new Constraints.Builder()
            .addContentUriTrigger(SMS_PROVIDER, true)
            .addContentUriTrigger(MMS_PROVIDER, true);

        if (preferences.getDataTypePreferences().isBackupEnabled(DataType.CALLLOG)
                && preferences.isCallLogBackupAfterCallEnabled()) {
            builder.addContentUriTrigger(CALLLOG_PROVIDER, true);
        }
        return builder.build();
    }
}
