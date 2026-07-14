package com.zegoggles.smssync.mail;

import org.junit.Test;

import java.net.InetAddress;

import static com.google.common.truth.Truth.assertThat;

public class InetAddressFamiliesTest {

    @Test public void ipv4OnlyKeepsIpv4Addresses() throws Exception {
        InetAddress[] mixed = new InetAddress[]{
                InetAddress.getByName("2001:db8::1"),
                InetAddress.getByName("192.0.2.1"),
                InetAddress.getByName("2001:db8::2"),
                InetAddress.getByName("192.0.2.2"),
        };

        InetAddress[] ipv4 = InetAddressFamilies.ipv4Only(mixed);

        assertThat(ipv4).hasLength(2);
        assertThat(ipv4[0].getHostAddress()).isEqualTo("192.0.2.1");
        assertThat(ipv4[1].getHostAddress()).isEqualTo("192.0.2.2");
    }

    @Test public void ipv6OnlyKeepsIpv6Addresses() throws Exception {
        InetAddress[] mixed = new InetAddress[]{
                InetAddress.getByName("192.0.2.1"),
                InetAddress.getByName("2001:db8::1"),
        };

        InetAddress[] ipv6 = InetAddressFamilies.ipv6Only(mixed);

        assertThat(ipv6).hasLength(1);
        assertThat(ipv6[0].getHostAddress()).contains("2001:db8");
    }

    @Test public void isMixedDualStackDetectsBothFamilies() throws Exception {
        InetAddress[] mixed = new InetAddress[]{
                InetAddress.getByName("192.0.2.1"),
                InetAddress.getByName("2001:db8::1"),
        };
        InetAddress[] ipv4Only = new InetAddress[]{InetAddress.getByName("192.0.2.1")};
        InetAddress[] ipv6Only = new InetAddress[]{InetAddress.getByName("2001:db8::1")};

        assertThat(InetAddressFamilies.isMixedDualStack(mixed)).isTrue();
        assertThat(InetAddressFamilies.isMixedDualStack(ipv4Only)).isFalse();
        assertThat(InetAddressFamilies.isMixedDualStack(ipv6Only)).isFalse();
        assertThat(InetAddressFamilies.isMixedDualStack(new InetAddress[0])).isFalse();
        assertThat(InetAddressFamilies.isMixedDualStack(null)).isFalse();
    }
}
