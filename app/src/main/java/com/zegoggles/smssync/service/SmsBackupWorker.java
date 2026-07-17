/* Copyright (c) 2017 Jan Berkel <jan.berkel@gmail.com>
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
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;
import com.squareup.otto.Subscribe;
import com.zegoggles.smssync.App;
import com.zegoggles.smssync.preferences.Preferences;
import com.zegoggles.smssync.service.state.BackupState;

import static com.zegoggles.smssync.App.LOCAL_LOGV;
import static com.zegoggles.smssync.App.TAG;
import static com.zegoggles.smssync.service.BackupJobs.DATA_BACKUP_TYPE;
import static com.zegoggles.smssync.service.BackupJobs.DATA_CONTENT_TRIGGER;
import static com.zegoggles.smssync.service.BackupType.REGULAR;
import static com.zegoggles.smssync.service.CancelEvent.Origin.SYSTEM;

/**
 * Runs a backup scheduled by {@link BackupJobs} via {@link androidx.work.WorkManager}.
 *
 * <p>The actual work is performed by {@link SmsBackupService}, which reports progress
 * asynchronously through the application event bus. We bridge that callback to the
 * {@link ListenableFuture} returned by {@link #startWork()} so WorkManager keeps the
 * worker alive until the backup has finished.
 */
public class SmsBackupWorker extends ListenableWorker {
    @Nullable private CallbackToFutureAdapter.Completer<Result> completer;
    @Nullable private String backupType;
    @Nullable private SmsBackupService backupService;

    public SmsBackupWorker(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
        super(context, workerParameters);
    }

    @NonNull @Override
    public ListenableFuture<Result> startWork() {
        backupType = getInputData().getString(DATA_BACKUP_TYPE);
        final boolean contentTrigger = getInputData().getBoolean(DATA_CONTENT_TRIGGER, false);
        if (LOCAL_LOGV) {
            Log.v(TAG, "startWork(backupType=" + backupType + ", contentTrigger=" + contentTrigger + ")");
        }

        return CallbackToFutureAdapter.getFuture(completer -> {
            this.completer = completer;
            if (contentTrigger) {
                // Content URI observers are single-shot; re-arm and debounce via incoming delay
                getBackupJobs().scheduleContentTriggerJob();
                getBackupJobs().scheduleIncoming();
                complete(Result.success());
            } else if (shouldRun()) {
                // Register the worker before handleIntent so synchronous FINISHED/ERROR
                // transitions are not missed. SmsBackupService.onCreate still registers the
                // service for AppLog + scheduleNextBackup; destroy is deferred in complete()
                // so the service can handle this same bus event first.
                App.register(this);
                startBackup(backupType);
            } else {
                Log.d(TAG, "skipping run");
                complete(Result.success());
            }
            return "SmsBackupWorker:" + backupType;
        });
    }

    /**
     * Starts the actual backup. Since API level 26, an app in the background cannot start a
     * background service, so the service is instantiated manually.
     * https://developer.android.com/about/versions/oreo/background.html#services
     */
    protected void startBackup(String backupType) {
        SmsBackupService service = new SmsBackupService();
        service.attachBaseContext(getApplicationContext());
        service.onCreate();
        backupService = service;
        service.handleIntent(new Intent(backupType));
    }

    /**
     * Called when WorkManager interrupts the running work, most likely because the runtime
     * constraints are no longer satisfied. The backup must stop execution.
     */
    @Override
    public void onStopped() {
        if (LOCAL_LOGV) Log.v(TAG, "onStopped()");
        // WorkManager may call this off the main thread; Otto's default bus rejects that.
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override public void run() {
                App.post(new CancelEvent(SYSTEM));
            }
        });
    }

    @Subscribe
    public void backupStateChanged(BackupState state) {
        if (!state.isFinished() || state.backupType == null || !state.backupType.name().equals(backupType)) {
            return;
        }
        final boolean needsReschedule = state.isError() && !state.isPermissionException();
        if (LOCAL_LOGV) {
            Log.v(TAG, "backupStateChanged finished(isError=" + state.isError() + ", needsReschedule=" + needsReschedule + ")");
        }
        complete(needsReschedule ? Result.retry() : Result.success());
    }

    private void complete(Result result) {
        App.unregister(this);
        if (completer != null) {
            completer.set(result);
            completer = null;
        }
        // Defer onDestroy until after the current Otto dispatch so SmsBackupService can
        // still handle this FINISHED event (AppLog, scheduleNextBackup).
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override public void run() {
                destroyBackupService();
            }
        });
    }

    private void destroyBackupService() {
        if (backupService == null) {
            return;
        }
        try {
            // stopSelf() is a no-op when the service was never started by the system.
            backupService.onDestroy();
        } catch (RuntimeException e) {
            Log.w(TAG, "error destroying SmsBackupService", e);
        }
        backupService = null;
    }

    private boolean shouldRun() {
        if (BackupType.fromName(backupType) == REGULAR) {
            final boolean autoBackupEnabled = new Preferences(getApplicationContext()).isAutoBackupEnabled();
            if (!autoBackupEnabled) {
                getBackupJobs().cancelRegular();
            }
            return autoBackupEnabled;
        }
        return true;
    }

    private BackupJobs getBackupJobs() {
        return new BackupJobs(getApplicationContext());
    }
}
