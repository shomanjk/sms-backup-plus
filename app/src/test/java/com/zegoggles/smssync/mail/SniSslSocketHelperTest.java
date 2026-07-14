package com.zegoggles.smssync.mail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class)
public class SniSslSocketHelperTest {

    @Test public void applySniSetsServerNameOnSocket() throws Exception {
        SSLSocket socket = (SSLSocket) SSLContext.getDefault().getSocketFactory().createSocket();
        SniSslSocketHelper.applySni(socket, "imap.gmail.com");

        List<SNIServerName> names = socket.getSSLParameters().getServerNames();
        assertThat(names).isNotNull();
        assertThat(names).isNotEmpty();
        assertThat(names.get(0)).isInstanceOf(SNIHostName.class);
        assertThat(((SNIHostName) names.get(0)).getAsciiName()).isEqualTo("imap.gmail.com");
    }

    @Test public void applySniIgnoresInvalidHostWithoutThrowing() throws Exception {
        SSLSocket socket = (SSLSocket) SSLContext.getDefault().getSocketFactory().createSocket();
        SniSslSocketHelper.applySni(socket, "not a valid hostname!!!");
        // No exception; SNI may be absent for invalid hosts.
        assertThat(socket).isNotNull();
    }

    @Test public void applySniIgnoresNullAndEmptyHost() throws Exception {
        SSLSocket socket = (SSLSocket) SSLContext.getDefault().getSocketFactory().createSocket();
        SniSslSocketHelper.applySni(socket, null);
        SniSslSocketHelper.applySni(socket, "");
        assertThat(socket).isNotNull();
    }
}
