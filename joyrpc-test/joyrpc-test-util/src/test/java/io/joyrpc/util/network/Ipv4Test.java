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
import org.junit.Assert;
import org.junit.Test;

public class Ipv4Test {

    @Test
    public void testUrl() {
        URL url = URL.valueOf("http://[2001:470:c:1818::2]:80/index.html");
        Assert.assertEquals(url.getProtocol(), "http");
        Assert.assertEquals(url.getPort(), 80);
        Assert.assertEquals(url.getHost(), "2001:470:c:1818::2");
        Assert.assertEquals(url.getPath(), "index.html");
    }

    @Test
    public void testLocal() {
        String ip = Ipv4.getLocalIp();
        Assert.assertTrue(ip.contains(Ipv4.isIpv4() ? "." : ":"));
    }

    @Test
    public void testParseIp() {
        IpPart parts = IpLong.parseIp("0000:0000:0000:0000:0000:0000:0000:0000");
        Assert.assertNotNull(parts);
        parts = IpLong.parseIp("::");
        Assert.assertNotNull(parts);
        parts = IpLong.parseIp("::0001:0000:0000:0000:0000:0000:0000");
        Assert.assertNotNull(parts);
        parts = IpLong.parseIp("0000:0000:0000:0000:0000::0000");
        Assert.assertNotNull(parts);
        parts = IpLong.parseIp("0000:0000:a000:0000:0000:0000:0001::");
        Assert.assertNotNull(parts);
        parts = IpLong.parseIp("::");
        Assert.assertNotNull(parts);
        parts = IpLong.parseIp("::FFFF:192.168.1.1");
        Assert.assertNotNull(parts);
        parts = IpLong.parseIp("0000:0000::0000.0000:0000:0000:000");
        Assert.assertNull(parts);
        parts = IpLong.parseIp(":0000:0000:0000:0000:0000:0000:0000");
        Assert.assertNull(parts);
        parts = IpLong.parseIp("0000:0000:0000:xxxx:0000:0000:0000:0000");
        Assert.assertNull(parts);
        parts = IpLong.parseIp("0000:0000:0000::0000::0000:0000");
        Assert.assertNull(parts);
        parts = IpLong.parseIp("::FFFF:192.168.1");
        Assert.assertNull(parts);
        IpLong ipLong = new IpLong("0000:0000:0000:0001:0000:0000:0000:FFFF");
        Assert.assertEquals(ipLong.getHigh(), 1L);
        Assert.assertEquals(ipLong.getLow(), 65535L);
        ipLong = new IpLong(ipLong.getHigh(), ipLong.getLow());
        Assert.assertEquals(ipLong.toString(), "::0001:0000:0000:0000:FFFF");
        ipLong = new IpLong("::FFFF:192.168.1.1");
        ipLong = new IpLong(ipLong.getHigh(), ipLong.getLow(), ipLong.getType());
        Assert.assertEquals(ipLong.toString(), "::FFFF:192.168.1.1");
    }

    @Test
    public void testSegment() {
        Segment segment = new Segment("192.168.1.1");
        Assert.assertEquals(segment.getBegin().toString(), "192.168.1.1");
        Assert.assertEquals(segment.getEnd().toString(), "192.168.1.1");
        segment = new Segment("192.168.1.*");
        Assert.assertEquals(segment.getBegin().toString(), "192.168.1.0");
        Assert.assertEquals(segment.getEnd().toString(), "192.168.1.255");
        segment = new Segment("192.168.1.1-192.168.1.2");
        Assert.assertEquals(segment.getBegin().toString(), "192.168.1.1");
        Assert.assertEquals(segment.getEnd().toString(), "192.168.1.2");
        segment = new Segment("192.168.1.1/23");
        Assert.assertEquals(segment.getBegin().toString(), "192.168.0.0");
        Assert.assertEquals(segment.getEnd().toString(), "192.168.1.255");
        segment = new Segment("::/128");
        Assert.assertEquals(segment.getBegin().toString(), "::");
        Assert.assertEquals(segment.getEnd().toString(), "::");
        segment = new Segment("::1/128");
        Assert.assertEquals(segment.getBegin().toString(), "::0001");
        Assert.assertEquals(segment.getEnd().toString(), "::0001");
        segment = new Segment("::2/127");
        Assert.assertEquals(segment.getBegin().toString(), "::0002");
        Assert.assertEquals(segment.getEnd().toString(), "::0003");
        segment = new Segment("0:0:0:2::/63");
        Assert.assertEquals(segment.getBegin().toString(), "0000:0000:0000:0002::");
        Assert.assertEquals(segment.getEnd().toString(), "::0003:FFFF:FFFF:FFFF:FFFF");
    }
}
