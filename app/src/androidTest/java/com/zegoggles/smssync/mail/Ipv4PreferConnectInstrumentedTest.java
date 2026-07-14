package com.zegoggles.smssync.mail;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Emulator/device probe: when imap.gmail.com is dual-stack, IPv4 should connect
 * without waiting on IPv6 timeouts (the failure mode the IMAP shim avoids).
 */
@RunWith(AndroidJUnit4.class)
public class Ipv4PreferConnectInstrumentedTest {
    private static final String HOST = "imap.gmail.com";
    private static final int PORT = 993;
    private static final int CONNECT_TIMEOUT_MS = 5000;

    @Test
    public void ipv4ConnectsWhenHostIsDualStack() throws Exception {
        InetAddress[] addresses = InetAddress.getAllByName(HOST);
        Assume.assumeTrue(
                "Host must resolve to both IPv4 and IPv6 for this probe",
                InetAddressFamilies.isMixedDualStack(addresses));

        InetAddress[] ipv4 = InetAddressFamilies.ipv4Only(addresses);
        long start = System.currentTimeMillis();
        connectFirst(ipv4);
        long elapsed = System.currentTimeMillis() - start;

        assertTrue("IPv4 IMAP connect should usually finish under one 5s timeout, was "
                + elapsed + "ms", elapsed < CONNECT_TIMEOUT_MS);
    }

    @Test
    public void ipv6FirstWouldPayTimeoutWhenIpv6Blackholed() throws Exception {
        InetAddress[] addresses = InetAddress.getAllByName(HOST);
        Assume.assumeTrue(InetAddressFamilies.isMixedDualStack(addresses));

        InetAddress[] ipv6 = InetAddressFamilies.ipv6Only(addresses);
        if (ipv6.length == 0) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            connectFirst(ipv6);
            // IPv6 works on this network — probe does not apply; still OK.
        } catch (IOException expectedWhenBroken) {
            long elapsed = System.currentTimeMillis() - start;
            assertTrue(
                    "Broken IPv6 should burn roughly the connect timeout ("
                            + expectedWhenBroken.getClass().getSimpleName() + "), was "
                            + elapsed + "ms",
                    elapsed >= CONNECT_TIMEOUT_MS - 500
                            || expectedWhenBroken instanceof SocketTimeoutException);
        }
    }

    private static void connectFirst(InetAddress[] addresses) throws IOException {
        IOException last = null;
        for (InetAddress address : addresses) {
            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(address, PORT), CONNECT_TIMEOUT_MS);
                socket.close();
                return;
            } catch (IOException e) {
                last = e;
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }
        if (last != null) {
            throw last;
        }
        fail("No addresses to connect");
    }
}
