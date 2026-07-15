package com.zegoggles.smssync.compat;

import android.content.Intent;
import android.os.Build;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class)
public class SmsReceiverTest {
    private SmsReceiver subject;

    @Before
    public void setUp() throws Exception {
        subject = new SmsReceiver();
    }

    // minSdk is 24; KitKat-era Config SDKs are unavailable in Robolectric 4.14
    @Test @Config(sdk = Build.VERSION_CODES.N)
    public void testOnReceive() {
        subject.onReceive(RuntimeEnvironment.getApplication(), new Intent());
    }

    @Test @Config(sdk = Build.VERSION_CODES.N)
    public void testIsSmsBackupDefaultSmsApp() {
        assertThat(SmsReceiver.isSmsBackupDefaultSmsApp(RuntimeEnvironment.getApplication())).isFalse();
    }
}
