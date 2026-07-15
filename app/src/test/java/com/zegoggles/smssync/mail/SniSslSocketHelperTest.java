package com.zegoggles.smssync.mail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class)
public class SniSslSocketHelperTest {

    @Test public void applySniSetsServerNameOnSocket() throws Exception {
        // Robolectric's platform SSLSocket may not persist SNI; use a stub that does.
        RecordingSslSocket socket = new RecordingSslSocket();
        SniSslSocketHelper.applySni(socket, "imap.gmail.com");

        List<SNIServerName> names = socket.getSSLParameters().getServerNames();
        assertThat(names).isNotNull();
        assertThat(names).isNotEmpty();
        assertThat(names.get(0)).isInstanceOf(SNIHostName.class);
        assertThat(((SNIHostName) names.get(0)).getAsciiName()).isEqualTo("imap.gmail.com");
    }

    @Test public void applySniIgnoresInvalidHostWithoutThrowing() throws Exception {
        RecordingSslSocket socket = new RecordingSslSocket();
        SniSslSocketHelper.applySni(socket, "not a valid hostname!!!");
        // No exception; SNI may be absent for invalid hosts.
        assertThat(socket.getSSLParameters().getServerNames()).isNull();
    }

    @Test public void applySniIgnoresNullAndEmptyHost() throws Exception {
        RecordingSslSocket socket = new RecordingSslSocket();
        SniSslSocketHelper.applySni(socket, null);
        SniSslSocketHelper.applySni(socket, "");
        assertThat(socket.getSSLParameters().getServerNames()).isNull();
    }

    /** Minimal SSLSocket that records setSSLParameters for unit tests. */
    private static final class RecordingSslSocket extends SSLSocket {
        private SSLParameters parameters = new SSLParameters();

        @Override public SSLParameters getSSLParameters() {
            return parameters;
        }

        @Override public void setSSLParameters(SSLParameters params) {
            this.parameters = params;
        }

        @Override public String[] getSupportedCipherSuites() { return new String[0]; }
        @Override public String[] getEnabledCipherSuites() { return new String[0]; }
        @Override public void setEnabledCipherSuites(String[] suites) {}
        @Override public String[] getSupportedProtocols() { return new String[0]; }
        @Override public String[] getEnabledProtocols() { return new String[0]; }
        @Override public void setEnabledProtocols(String[] protocols) {}
        @Override public SSLSession getSession() { return null; }
        @Override public void addHandshakeCompletedListener(
                javax.net.ssl.HandshakeCompletedListener listener) {}
        @Override public void removeHandshakeCompletedListener(
                javax.net.ssl.HandshakeCompletedListener listener) {}
        @Override public void startHandshake() {}
        @Override public void setUseClientMode(boolean mode) {}
        @Override public boolean getUseClientMode() { return true; }
        @Override public void setNeedClientAuth(boolean need) {}
        @Override public boolean getNeedClientAuth() { return false; }
        @Override public void setWantClientAuth(boolean want) {}
        @Override public boolean getWantClientAuth() { return false; }
        @Override public void setEnableSessionCreation(boolean flag) {}
        @Override public boolean getEnableSessionCreation() { return true; }
        @Override public InputStream getInputStream() throws IOException {
            throw new IOException("unused");
        }
        @Override public OutputStream getOutputStream() throws IOException {
            throw new IOException("unused");
        }
    }
}
