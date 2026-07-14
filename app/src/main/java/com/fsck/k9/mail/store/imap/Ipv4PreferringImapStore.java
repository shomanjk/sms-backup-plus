package com.fsck.k9.mail.store.imap;

import android.net.ConnectivityManager;

import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import com.fsck.k9.mail.store.StoreConfig;

/**
 * {@link ImapStore} that creates {@link Ipv4PreferringImapConnection} instances
 * so dual-stack IMAP hosts try IPv4 before IPv6.
 *
 * <p>Must live in this package to override package-private
 * {@link #createImapConnection()}.
 */
public class Ipv4PreferringImapStore extends ImapStore {

    public Ipv4PreferringImapStore(StoreConfig storeConfig,
                                   TrustedSocketFactory trustedSocketFactory,
                                   ConnectivityManager connectivityManager)
            throws MessagingException {
        super(storeConfig, trustedSocketFactory, connectivityManager);
    }

    @Override
    ImapConnection createImapConnection() {
        return Ipv4PreferringImapConnection.wrap(super.createImapConnection(), mTrustedSocketFactory);
    }
}
