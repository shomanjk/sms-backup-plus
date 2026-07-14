package com.zegoggles.smssync.preferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import androidx.preference.PreferenceManager;

import com.zegoggles.smssync.App;

import static com.google.common.truth.Truth.assertThat;
import static com.zegoggles.smssync.mail.DataType.CALLLOG;
import static com.zegoggles.smssync.mail.DataType.MMS;
import static com.zegoggles.smssync.mail.DataType.SMS;
import static com.zegoggles.smssync.preferences.Preferences.Keys.LAST_VERSION_CODE;
import static com.zegoggles.smssync.preferences.Preferences.VersionDialogKind.FIRST_INSTALL;
import static com.zegoggles.smssync.preferences.Preferences.VersionDialogKind.NONE;
import static com.zegoggles.smssync.preferences.Preferences.VersionDialogKind.UPGRADE;

@RunWith(RobolectricTestRunner.class)
public class PreferencesTest {
    Preferences preferences;

    @Before public void before() {
        PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)
                .edit()
                .remove(LAST_VERSION_CODE.key)
                .commit();
        preferences = new Preferences(RuntimeEnvironment.application);
    }

    @Test public void shouldTestForFirstUse() throws Exception {
        assertThat(preferences.isFirstUse()).isTrue();
        assertThat(preferences.isFirstUse()).isFalse();
    }
    @Test public void shouldTestForFirstBackup() throws Exception {
        assertThat(preferences.isFirstBackup()).isTrue();
    }

    @Test public void shouldTestForFirstBackupSMS() throws Exception {
        preferences.getDataTypePreferences().setMaxSyncedDate(SMS, 1234);
        assertThat(preferences.isFirstBackup()).isFalse();
    }

    @Test public void shouldTestForFirstBackupMMS() throws Exception {
        preferences.getDataTypePreferences().setMaxSyncedDate(MMS, 1234);
        assertThat(preferences.isFirstBackup()).isFalse();
    }

    @Test public void shouldTestForFirstBackupCallLog() throws Exception {
        preferences.getDataTypePreferences().setMaxSyncedDate(CALLLOG, 1234);
        assertThat(preferences.isFirstBackup()).isFalse();
    }

    @Test public void consumeVersionDialogShowsWelcomeOnFirstInstall() {
        assertThat(preferences.consumeVersionDialog()).isEqualTo(FIRST_INSTALL);
        assertThat(preferences.consumeVersionDialog()).isEqualTo(NONE);
    }

    @Test public void consumeVersionDialogShowsUpgradeAfterPriorVersion() {
        final int current = App.getVersionCode(RuntimeEnvironment.application);
        PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)
                .edit()
                .putInt(LAST_VERSION_CODE.key, current - 1)
                .commit();
        preferences = new Preferences(RuntimeEnvironment.application);

        assertThat(preferences.consumeVersionDialog()).isEqualTo(UPGRADE);
        assertThat(preferences.consumeVersionDialog()).isEqualTo(NONE);
    }
}
