package com.zegoggles.smssync.mail;

import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Collections;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;

import static com.zegoggles.smssync.App.TAG;

/**
 * Applies TLS SNI using public {@link SSLParameters} APIs.
 * Avoids K9's {@code SSLSocket#setHostname} reflection, which is blocked on modern targetSdk.
 */
final class SniSslSocketHelper {
    private SniSslSocketHelper() {}

    static void applySni(SSLSocket socket, @Nullable String host) {
        if (socket == null || host == null || host.isEmpty()) {
            return;
        }
        try {
            SNIHostName serverName = new SNIHostName(host);
            SSLParameters parameters = socket.getSSLParameters();
            parameters.setServerNames(Collections.singletonList(serverName));
            socket.setSSLParameters(parameters);
        } catch (IllegalArgumentException e) {
            // Host is not a valid SNI hostname (e.g. bare IP); omit SNI.
            Log.d(TAG, "Skipping SNI for host: " + host);
        } catch (RuntimeException e) {
            Log.w(TAG, "Unable to apply SNI for host: " + host, e);
        }
    }
}
