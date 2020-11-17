package io.joyrpc.util.network;

/*-
 * #%L
 * joyrpc
 * %%
 * Copyright (C) 2019 joyrpc.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import io.joyrpc.extension.URL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

public class Ipv4Test {

    @Test
    public void testUrl() {
        URL url = URL.valueOf("http://[2001:470:c:1818::2]:80/index.html");
        Assertions.assertEquals(url.getProtocol(), "http");
        Assertions.assertEquals(url.getPort(), 80);
        Assertions.assertEquals(url.getHost(), "2001:470:c:1818::2");
        Assertions.assertEquals(url.getPath(), "index.html");
        url = url.setAddress("[2001:470:c:1818::3]:");
        Assertions.assertEquals(url.getHost(), "2001:470:c:1818::3");
        Assertions.assertEquals(url.getPort(), 0);
        url = url.setAddress("[]:80");
        Assertions.assertEquals(url.getHost(), "");
        Assertions.assertEquals(url.getPort(), 80);
    }

    @Test
    public void testLocal() {
        String ip = Ipv4.getLocalIp();
        Assertions.assertTrue(ip.contains(Ipv4.isIpv4() ? "." : ":"));
    }

    @Test
    public void testParseIp() {
        IpPart parts = IpLong.parseIp("0000:0000:0000:0000:0000:0000:0000:0000");
        Assertions.assertNotNull(parts);
        parts = IpLong.parseIp("::");
        Assertions.assertNotNull(parts);
        parts = IpLong.parseIp("::0001:0000:0000:0000:0000:0000:0000");
        Assertions.assertNotNull(parts);
        parts = IpLong.parseIp("0000:0000:0000:0000:0000::0000");
        Assertions.assertNotNull(parts);
        parts = IpLong.parseIp("0000:0000:a000:0000:0000:0000:0001::");
        Assertions.assertNotNull(parts);
        parts = IpLong.parseIp("::");
        Assertions.assertNotNull(parts);
        parts = IpLong.parseIp("::ffff:192.168.1.1");
        Assertions.assertNotNull(parts);
        parts = IpLong.parseIp("0000:0000:0000:0000:0000::0000%abc");
        Assertions.assertNotNull(parts);
        Assertions.assertEquals(parts.getIfName(), "abc");
        parts = IpLong.parseIp("0000:0000:0000:0000:0000::0000%");
        Assertions.assertNull(parts);
        parts = IpLong.parseIp("0000:0000::0000.0000:0000:0000:000");
        Assertions.assertNull(parts);
        parts = IpLong.parseIp(":0000:0000:0000:0000:0000:0000:0000");
        Assertions.assertNull(parts);
        parts = IpLong.parseIp("0000:0000:0000:xxxx:0000:0000:0000:0000");
        Assertions.assertNull(parts);
        parts = IpLong.parseIp("0000:0000:0000::0000::0000:0000");
        Assertions.assertNull(parts);
        parts = IpLong.parseIp("::ffff:192.168.1");
        Assertions.assertNull(parts);
        IpLong ipLong = new IpLong("0000:0000:0000:0001:0000:0000:0000:ffff");
        Assertions.assertEquals(ipLong.getHigh(), 1L);
        Assertions.assertEquals(ipLong.getLow(), 65535L);
        ipLong = new IpLong(ipLong.getHigh(), ipLong.getLow());
        Assertions.assertEquals(ipLong.toString(), "::1:0:0:0:ffff");
        ipLong = new IpLong("::ffff:192.168.1.1");
        ipLong = new IpLong(ipLong.getHigh(), ipLong.getLow(), ipLong.getType());
        Assertions.assertEquals(ipLong.toString(), "::ffff:192.168.1.1");
        ipLong = new IpLong("::ffff:192.168.1.1%en0");
        Assertions.assertEquals(ipLong.toString(), "::ffff:192.168.1.1%en0");
    }

    @Test
    public void testSegment() {
        Segment segment = new Segment("192.168.1.1");
        Assertions.assertEquals(segment.getBegin().toString(), "192.168.1.1");
        Assertions.assertEquals(segment.getEnd().toString(), "192.168.1.1");
        segment = new Segment("192.168.1.*");
        Assertions.assertEquals(segment.getBegin().toString(), "192.168.1.0");
        Assertions.assertEquals(segment.getEnd().toString(), "192.168.1.255");
        segment = new Segment("192.168.1.1-192.168.1.2");
        Assertions.assertEquals(segment.getBegin().toString(), "192.168.1.1");
        Assertions.assertEquals(segment.getEnd().toString(), "192.168.1.2");
        segment = new Segment("192.168.1.1/23");
        Assertions.assertEquals(segment.getBegin().toString(), "192.168.0.0");
        Assertions.assertEquals(segment.getEnd().toString(), "192.168.1.255");
        segment = new Segment("::/128");
        Assertions.assertEquals(segment.getBegin().toString(), "::");
        Assertions.assertEquals(segment.getEnd().toString(), "::");
        segment = new Segment("::1/128");
        Assertions.assertEquals(segment.getBegin().toString(), "::1");
        Assertions.assertEquals(segment.getEnd().toString(), "::1");
        segment = new Segment("::2/127");
        Assertions.assertEquals(segment.getBegin().toString(), "::2");
        Assertions.assertEquals(segment.getEnd().toString(), "::3");
        segment = new Segment("0:0:0:2::/63");
        Assertions.assertEquals(segment.getBegin().toString(), "0:0:0:2::");
        Assertions.assertEquals(segment.getEnd().toString(), "::3:ffff:ffff:ffff:ffff");
    }
}
