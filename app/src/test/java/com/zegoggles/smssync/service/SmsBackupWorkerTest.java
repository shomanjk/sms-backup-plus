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
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ListenableWorker.Result;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;
import androidx.work.testing.TestListenableWorkerBuilder;
import androidx.work.testing.WorkManagerTestInitHelper;

import com.google.common.util.concurrent.ListenableFuture;
import com.squareup.otto.Subscribe;
import com.zegoggles.smssync.App;
import com.zegoggles.smssync.service.exception.MissingPermissionException;
import com.zegoggles.smssync.service.state.BackupState;
import com.zegoggles.smssync.service.state.SmsSyncState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.zegoggles.smssync.service.BackupJobs.CONTENT_TRIGGER_TAG;
import static com.zegoggles.smssync.service.BackupJobs.DATA_BACKUP_TYPE;
import static com.zegoggles.smssync.service.BackupJobs.DATA_CONTENT_TRIGGER;
import static com.zegoggles.smssync.service.BackupType.INCOMING;
import static com.zegoggles.smssync.service.BackupType.REGULAR;
import static com.zegoggles.smssync.service.state.SmsSyncState.ERROR;
import static com.zegoggles.smssync.service.state.SmsSyncState.FINISHED_BACKUP;

@RunWith(RobolectricTestRunner.class)
public class SmsBackupWorkerTest {
    private WorkManager workManager;

    @Before public void before() {
        WorkManagerTestInitHelper.initializeTestWorkManager(RuntimeEnvironment.getApplication());
        workManager = WorkManager.getInstance(RuntimeEnvironment.getApplication());
    }

    @Test @Config(sdk = Build.VERSION_CODES.N)
    public void shouldRearmContentTriggerAndSucceedForContentTriggerRun() throws Exception {
        SmsBackupWorker worker = buildWorker(new Data.Builder()
            .putString(DATA_BACKUP_TYPE, INCOMING.name())
            .putBoolean(DATA_CONTENT_TRIGGER, true)
            .build());

        Result result = worker.startWork().get();

        assertThat(result).isEqualTo(Result.success());
        assertScheduled(CONTENT_TRIGGER_TAG);
    }

    @Test public void shouldSkipRegularBackupWhenAutoBackupDisabled() throws Exception {
        SmsBackupWorker worker = buildWorker(new Data.Builder()
            .putString(DATA_BACKUP_TYPE, REGULAR.name())
            .build());

        Result result = worker.startWork().get();

        assertThat(result).isEqualTo(Result.success());
        assertNotScheduled(REGULAR.name());
    }

    @Test public void shouldPostSystemCancelEventWhenStopped() {
        SmsBackupWorker worker = buildWorker(new Data.Builder()
            .putString(DATA_BACKUP_TYPE, REGULAR.name())
            .build());
        RecordingListener listener = new RecordingListener();
        App.register(listener);
        try {
            worker.onStopped();
        } finally {
            App.unregister(listener);
        }

        assertThat(listener.cancelEvent).isNotNull();
        assertThat(listener.cancelEvent.mayInterruptIfRunning()).isTrue();
    }

    @Test public void shouldIgnoreBackupStateBeforeWorkStarted() {
        SmsBackupWorker worker = buildWorker(new Data.Builder()
            .putString(DATA_BACKUP_TYPE, REGULAR.name())
            .build());

        worker.backupStateChanged(new BackupState());
        worker.backupStateChanged(state(FINISHED_BACKUP, INCOMING, null));
    }

    @Test public void shouldSucceedWhenBackupFinishesSuccessfully() throws Exception {
        TestWorker worker = startedWorker(INCOMING);

        worker.backupStateChanged(state(FINISHED_BACKUP, INCOMING, null));

        assertThat(worker.future.isDone()).isTrue();
        assertThat(worker.future.get()).isEqualTo(Result.success());
    }

    @Test public void shouldRetryWhenBackupFinishesWithError() throws Exception {
        TestWorker worker = startedWorker(INCOMING);

        worker.backupStateChanged(state(ERROR, INCOMING, new RuntimeException("boom")));

        assertThat(worker.future.get()).isEqualTo(Result.retry());
    }

    @Test public void shouldNotRetryWhenBackupFailsWithMissingPermission() throws Exception {
        TestWorker worker = startedWorker(INCOMING);

        worker.backupStateChanged(state(ERROR, INCOMING,
            new MissingPermissionException(Collections.<String>emptySet())));

        assertThat(worker.future.get()).isEqualTo(Result.success());
    }

    @Test public void shouldStayPendingForUnrelatedBackupState() {
        TestWorker worker = startedWorker(INCOMING);

        worker.backupStateChanged(state(FINISHED_BACKUP, REGULAR, null));
        worker.backupStateChanged(new BackupState());

        assertThat(worker.future.isDone()).isFalse();
    }

    private SmsBackupWorker buildWorker(Data inputData) {
        return TestListenableWorkerBuilder
            .from(RuntimeEnvironment.getApplication(), SmsBackupWorker.class)
            .setInputData(inputData)
            .build();
    }

    private TestWorker startedWorker(BackupType backupType) {
        TestWorker worker = TestListenableWorkerBuilder
            .from(RuntimeEnvironment.getApplication(), TestWorker.class)
            .setInputData(new Data.Builder().putString(DATA_BACKUP_TYPE, backupType.name()).build())
            .build();
        worker.future = worker.startWork();
        return worker;
    }

    private BackupState state(SmsSyncState syncState, BackupType type, Exception exception) {
        return new BackupState(syncState, 0, 0, type, null, exception);
    }

    private void assertScheduled(String uniqueName) throws Exception {
        final List<WorkInfo> infos = workManager.getWorkInfosForUniqueWork(uniqueName).get();
        assertThat(infos).hasSize(1);
        assertThat(infos.get(0).getState()).isNotEqualTo(WorkInfo.State.CANCELLED);
        assertThat(infos.get(0).getTags()).contains(uniqueName);
    }

    private void assertNotScheduled(String uniqueName) throws Exception {
        final List<WorkInfo> infos = workManager.getWorkInfosForUniqueWork(uniqueName).get();
        for (WorkInfo info : infos) {
            assertThat(info.getState()).isNotEqualTo(WorkInfo.State.ENQUEUED);
        }
    }

    private static class RecordingListener {
        CancelEvent cancelEvent;

        @Subscribe public void onCancel(CancelEvent event) {
            cancelEvent = event;
        }
    }

    public static class TestWorker extends SmsBackupWorker {
        ListenableFuture<Result> future;

        public TestWorker(@NonNull Context context, @NonNull WorkerParameters params) {
            super(context, params);
        }

        @Override protected void startBackup(String backupType) {
            // leave future pending until a BackupState is delivered
        }
    }
}
