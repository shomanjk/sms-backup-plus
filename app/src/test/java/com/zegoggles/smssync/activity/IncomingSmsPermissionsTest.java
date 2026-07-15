package com.zegoggles.smssync.activity;

import android.Manifest;
import android.app.Activity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowApplication;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class)
public class IncomingSmsPermissionsTest {

    @Test public void missingReportsReadAndReceiveUntilGranted() {
        ShadowApplication shadowApp = Shadows.shadowOf(RuntimeEnvironment.application);
        shadowApp.denyPermissions(
                Manifest.permission.READ_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.RECEIVE_MMS);

        assertThat(IncomingSmsPermissions.missing(RuntimeEnvironment.application))
                .asList()
                .containsExactly(
                        Manifest.permission.READ_SMS,
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.RECEIVE_MMS);

        shadowApp.grantPermissions(
                Manifest.permission.READ_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.RECEIVE_MMS);

        assertThat(IncomingSmsPermissions.missing(RuntimeEnvironment.application)).isEmpty();
    }

    @Test public void canShowSystemPromptBeforeFirstRequest() {
        Activity activity = Robolectric.setupActivity(Activity.class);
        assertThat(IncomingSmsPermissions.wasSmsPermissionsRequested(activity)).isFalse();
        assertThat(IncomingSmsPermissions.canShowSystemPrompt(
                activity, IncomingSmsPermissions.required())).isTrue();
    }

    @Test public void cannotShowSystemPromptAfterRequestWithoutRationale() {
        Activity activity = Robolectric.setupActivity(Activity.class);
        IncomingSmsPermissions.markSmsPermissionsRequested(activity);
        assertThat(IncomingSmsPermissions.canShowSystemPrompt(
                activity, IncomingSmsPermissions.required())).isFalse();
    }
}
