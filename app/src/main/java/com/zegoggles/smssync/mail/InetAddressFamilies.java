package com.zegoggles.smssync.mail;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Filters {@link InetAddress} arrays for dual-stack connect strategies.
 *
 * <p>K-9's {@code ImapConnection} prefers IPv6 and waits ~5s per failing address,
 * which stalls IMAP on networks with broken IPv6. Preferring A records first
 * avoids that without forking the mail library.
 */
public final class InetAddressFamilies {
    private InetAddressFamilies() {}

    public static InetAddress[] ipv4Only(InetAddress[] addresses) {
        return filter(addresses, true);
    }

    public static InetAddress[] ipv6Only(InetAddress[] addresses) {
        return filter(addresses, false);
    }

    public static boolean isMixedDualStack(InetAddress[] addresses) {
        boolean v4 = false;
        boolean v6 = false;
        if (addresses == null) {
            return false;
        }
        for (InetAddress address : addresses) {
            if (address instanceof Inet4Address) {
                v4 = true;
            } else if (address instanceof Inet6Address) {
                v6 = true;
            }
            if (v4 && v6) {
                return true;
            }
        }
        return false;
    }

    private static InetAddress[] filter(InetAddress[] addresses, boolean wantIpv4) {
        if (addresses == null || addresses.length == 0) {
            return new InetAddress[0];
        }
        List<InetAddress> filtered = new ArrayList<>(addresses.length);
        for (InetAddress address : addresses) {
            if (wantIpv4) {
                if (address instanceof Inet4Address) {
                    filtered.add(address);
                }
            } else if (address instanceof Inet6Address) {
                filtered.add(address);
            }
        }
        return filtered.toArray(new InetAddress[0]);
    }
}
