package com.zegoggles.smssync.mail;

import android.content.Context;
import android.text.TextUtils;

import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory;
import com.fsck.k9.mail.ssl.TrustManagerFactory;

import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

/**
 * Drop-in replacement for {@link DefaultTrustedSocketFactory} that sets SNI via
 * public {@link javax.net.ssl.SSLParameters} APIs instead of blocked Conscrypt reflection.
 *
 * <p>Client-certificate aliases fall back to the parent implementation (rare for this app).
 */
public class SniAwareTrustedSocketFactory extends DefaultTrustedSocketFactory {
    public SniAwareTrustedSocketFactory(Context context) {
        super(context);
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, String clientCertificateAlias)
            throws NoSuchAlgorithmException, KeyManagementException, MessagingException, IOException {
        if (!TextUtils.isEmpty(clientCertificateAlias)) {
            // Parent uses package-private KeyChainKeyManager; keep that path, then overlay SNI.
            Socket trusted = super.createSocket(socket, host, port, clientCertificateAlias);
            if (trusted instanceof SSLSocket) {
                SniSslSocketHelper.applySni((SSLSocket) trusted, host);
            }
            return trusted;
        }

        TrustManager[] trustManagers = new TrustManager[]{TrustManagerFactory.get(host, port)};
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagers, null);
        SSLSocketFactory socketFactory = sslContext.getSocketFactory();

        Socket trustedSocket;
        if (socket == null) {
            trustedSocket = socketFactory.createSocket();
        } else {
            trustedSocket = socketFactory.createSocket(socket, host, port, true);
        }

        SSLSocket sslSocket = (SSLSocket) trustedSocket;
        hardenSocket(sslSocket);
        SniSslSocketHelper.applySni(sslSocket, host);
        return trustedSocket;
    }

    private static void hardenSocket(SSLSocket sslSocket) {
        if (ENABLED_CIPHERS != null) {
            sslSocket.setEnabledCipherSuites(ENABLED_CIPHERS);
        }
        if (ENABLED_PROTOCOLS != null) {
            sslSocket.setEnabledProtocols(ENABLED_PROTOCOLS);
        }
    }
}
