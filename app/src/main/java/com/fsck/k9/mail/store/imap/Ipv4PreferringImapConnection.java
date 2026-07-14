package com.fsck.k9.mail.store.imap;

import android.net.ConnectivityManager;
import android.util.Log;

import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import com.zegoggles.smssync.mail.InetAddressFamilies;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import static com.zegoggles.smssync.App.TAG;

/**
 * Tries IPv4 addresses before IPv6 when DNS returns both, so broken IPv6 paths
 * do not burn K-9's per-address connect timeouts first.
 *
 * <p>Lives in this package so it can override package-private
 * {@link #connectToAddress(InetAddress[])}.
 */
class Ipv4PreferringImapConnection extends ImapConnection {

    Ipv4PreferringImapConnection(ImapSettings settings,
                                 TrustedSocketFactory socketFactory,
                                 ConnectivityManager connectivityManager) {
        super(settings, socketFactory, connectivityManager);
    }

    static ImapConnection wrap(ImapConnection stock, TrustedSocketFactory socketFactory) {
        try {
            ImapSettings settings = (ImapSettings) field(stock, "settings");
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) field(stock, "connectivityManager");
            return new Ipv4PreferringImapConnection(settings, socketFactory, connectivityManager);
        } catch (ReflectiveOperationException e) {
            Log.w(TAG, "Unable to wrap ImapConnection for IPv4 preference; using stock connect", e);
            return stock;
        }
    }

    @Override
    Socket connectToAddress(InetAddress[] addresses)
            throws NoSuchAlgorithmException, KeyManagementException, MessagingException {
        if (!InetAddressFamilies.isMixedDualStack(addresses)) {
            return super.connectToAddress(addresses);
        }

        InetAddress[] ipv4 = InetAddressFamilies.ipv4Only(addresses);
        try {
            return super.connectToAddress(ipv4);
        } catch (MessagingException ipv4Failure) {
            InetAddress[] ipv6 = InetAddressFamilies.ipv6Only(addresses);
            try {
                return super.connectToAddress(ipv6);
            } catch (MessagingException ipv6Failure) {
                ipv6Failure.addSuppressed(ipv4Failure);
                throw ipv6Failure;
            }
        }
    }

    private static Object field(ImapConnection connection, String name)
            throws ReflectiveOperationException {
        Field field = ImapConnection.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(connection);
    }
}
