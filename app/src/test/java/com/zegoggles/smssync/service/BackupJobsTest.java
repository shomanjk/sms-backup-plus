package com.zegoggles.smssync.service;

import android.os.Build;

import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.testing.WorkManagerTestInitHelper;

import com.zegoggles.smssync.preferences.DataTypePreferences;
import com.zegoggles.smssync.preferences.Preferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class)
public class BackupJobsTest {
    private BackupJobs subject;
    private WorkManager workManager;

    @Mock private Preferences preferences;
    @Mock private DataTypePreferences dataTypePreferences;

    @Before public void before() {
        initMocks(this);
        WorkManagerTestInitHelper.initializeTestWorkManager(RuntimeEnvironment.getApplication());
        workManager = WorkManager.getInstance(RuntimeEnvironment.getApplication());
        subject = new BackupJobs(RuntimeEnvironment.getApplication(), preferences);
        when(preferences.getDataTypePreferences()).thenReturn(dataTypePreferences);
    }

    @Test public void shouldScheduleImmediate() throws Exception {
        subject.scheduleImmediate();
        assertScheduled(BackupType.BROADCAST_INTENT.name());
    }

    @Test public void shouldScheduleRegular() throws Exception {
        when(preferences.isAutoBackupEnabled()).thenReturn(true);
        when(preferences.getRegularTimeoutSecs()).thenReturn(2000);
        subject.scheduleRegular();
        assertScheduled(BackupType.REGULAR.name());
    }

    @Test public void shouldScheduleIncoming() throws Exception {
        when(preferences.isAutoBackupEnabled()).thenReturn(true);
        when(preferences.getIncomingTimeoutSecs()).thenReturn(2000);
        subject.scheduleIncoming();
        assertScheduled(BackupType.INCOMING.name());
    }

    @Test public void shouldKeepExistingIncomingWhenRescheduled() throws Exception {
        when(preferences.isAutoBackupEnabled()).thenReturn(true);
        when(preferences.getIncomingTimeoutSecs()).thenReturn(2000);
        subject.scheduleIncoming();
        final List<WorkInfo> first = workManager.getWorkInfosForUniqueWork(BackupType.INCOMING.name()).get();
        assertThat(first).hasSize(1);
        final java.util.UUID firstId = first.get(0).getId();

        // A second schedule (e.g. another SMS while backup is pending/running) must not
        // REPLACE/cancel the existing unique work.
        subject.scheduleIncoming();
        final List<WorkInfo> second = workManager.getWorkInfosForUniqueWork(BackupType.INCOMING.name()).get();
        assertThat(second).hasSize(1);
        assertThat(second.get(0).getId()).isEqualTo(firstId);
        assertThat(second.get(0).getState()).isNotEqualTo(WorkInfo.State.CANCELLED);
    }

    @Test @Config(sdk = Build.VERSION_CODES.N)
    public void shouldScheduleContentTriggerJob() throws Exception {
        subject.scheduleContentTriggerJob();
        assertScheduled(BackupJobs.CONTENT_TRIGGER_TAG);
        final List<WorkInfo> infos =
            workManager.getWorkInfosForUniqueWork(BackupJobs.CONTENT_TRIGGER_TAG).get();
        assertThat(infos.get(0).getConstraints().getRequiredNetworkType())
            .isEqualTo(androidx.work.NetworkType.NOT_REQUIRED);
    }

    @Test public void shouldScheduleIncomingWithoutNetworkConstraint() throws Exception {
        when(preferences.isAutoBackupEnabled()).thenReturn(true);
        when(preferences.getIncomingTimeoutSecs()).thenReturn(60);
        subject.scheduleIncoming();
        final List<WorkInfo> infos =
            workManager.getWorkInfosForUniqueWork(BackupType.INCOMING.name()).get();
        assertThat(infos).hasSize(1);
        assertThat(infos.get(0).getConstraints().getRequiredNetworkType())
            .isEqualTo(androidx.work.NetworkType.NOT_REQUIRED);
    }

    @Test public void shouldScheduleRegularJobAfterBoot() throws Exception {
        when(preferences.isAutoBackupEnabled()).thenReturn(true);
        when(preferences.getRegularTimeoutSecs()).thenReturn(2000);
        subject.scheduleBootup();
        assertScheduled(BackupType.REGULAR.name());
    }

    @Test public void shouldCancelAllJobsAfterBootIfAutoBackupDisabled() throws Exception {
        when(preferences.isAutoBackupEnabled()).thenReturn(false);
        subject.scheduleBootup();
        assertNotScheduled(BackupType.REGULAR.name());
    }

    @Test public void shouldNotScheduleRegularBackupIfAutoBackupIsDisabled() throws Exception {
        when(preferences.isAutoBackupEnabled()).thenReturn(false);
        subject.scheduleRegular();
        assertNotScheduled(BackupType.REGULAR.name());
    }

    @Test public void shouldEnsureAutoBackupJobsWithoutReplacingArmedContentTrigger() throws Exception {
        when(preferences.isAutoBackupEnabled()).thenReturn(true);
        when(preferences.getRegularTimeoutSecs()).thenReturn(7200);
        when(preferences.getIncomingTimeoutSecs()).thenReturn(60);
        subject.scheduleContentTriggerJob();
        final List<WorkInfo> first =
            workManager.getWorkInfosForUniqueWork(BackupJobs.CONTENT_TRIGGER_TAG).get();
        assertThat(first).hasSize(1);
        final java.util.UUID firstId = first.get(0).getId();

        subject.ensureAutoBackupJobs();
        final List<WorkInfo> second =
            workManager.getWorkInfosForUniqueWork(BackupJobs.CONTENT_TRIGGER_TAG).get();
        assertThat(second).hasSize(1);
        assertThat(second.get(0).getId()).isEqualTo(firstId);
        assertThat(second.get(0).getState()).isEqualTo(WorkInfo.State.ENQUEUED);
        assertScheduled(BackupType.REGULAR.name());
    }

    @Test public void shouldCancelWhenEnsuringWhileAutoBackupDisabled() throws Exception {
        when(preferences.isAutoBackupEnabled()).thenReturn(true);
        when(preferences.getRegularTimeoutSecs()).thenReturn(7200);
        when(preferences.getIncomingTimeoutSecs()).thenReturn(60);
        subject.ensureAutoBackupJobs();
        assertScheduled(BackupType.REGULAR.name());

        when(preferences.isAutoBackupEnabled()).thenReturn(false);
        subject.ensureAutoBackupJobs();
        assertNotScheduled(BackupType.REGULAR.name());
        assertNotScheduled(BackupJobs.CONTENT_TRIGGER_TAG);
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
}
