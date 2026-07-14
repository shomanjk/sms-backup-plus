package com.zegoggles.smssync.mail;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.Socket;

import javax.net.ssl.SSLSocket;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Device/emulator probe for SNI socket creation. Pair with logcat:
 * before (DefaultTrustedSocketFactory) should spam setHostname; after (SniAware) should not.
 */
@RunWith(AndroidJUnit4.class)
public class SniSocketFactoryInstrumentedTest {
    private static final String HOST = "imap.gmail.com";
    private static final int PORT = 993;

    @Test
    public void sniAwareFactoryCreatesSslSocketWithoutCrashing() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TrustedSocketFactory factory = new SniAwareTrustedSocketFactory(context);
        Socket socket = factory.createSocket(null, HOST, PORT, null);
        assertNotNull(socket);
        assertTrue(socket instanceof SSLSocket);
        socket.close();
    }

    @Test
    public void defaultFactoryCreateSocketProbeForLogcatBaseline() throws Exception {
        // Intentionally uses stock K9 factory so logcat can show the old reflection path.
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TrustedSocketFactory factory = new DefaultTrustedSocketFactory(context);
        Socket socket = factory.createSocket(null, HOST, PORT, null);
        assertNotNull(socket);
        assertTrue(socket instanceof SSLSocket);
        socket.close();
    }
}
