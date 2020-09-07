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

/**
 * 网段
 */
public class Segment {

    /**
     * 掩码位数(1-32)对应的子网掩码
     */
    protected static final String[] IPV4_MASKES =
            new String[]{"128.0.0.0", "192.0.0.0", "224.0.0.0", "240.0.0.0", "248.0.0.0", "252.0.0.0", "254.0.0.0",
                    "255.0.0.0", "255.128.0.0", "255.192.0.0", "255.224.0.0", "255.240.0.0", "255.248.0.0",
                    "255.252.0.0", "255.254.0.0", "255.255.0.0", "255.255.128.0", "255.255.192.0", "255.255.224.0",
                    "255.255.240.0", "255.255.248.0", "255.255.252.0", "255.255.254.0", "255.255.255.0",
                    "255.255.255.128", "255.255.255.192", "255.255.255.224", "255.255.255.240", "255.255.255.248",
                    "255.255.255.252", "255.255.255.254", "255.255.255.255"};
    protected static final long[] IPV6_MASKES = new long[]{
            0x1L, 0x3L, 0x7L, 0xfL,
            0x1fL, 0x3fL, 0x7fL, 0xffL,
            0x1ffL, 0x3ffL, 0x7ffL, 0xfffL,
            0x1fffL, 0x3fffL, 0x7fffL, 0xffffL,
            0x1ffffL, 0x3ffffL, 0x7ffffL, 0xfffffL,
            0x1fffffL, 0x3fffffL, 0x7fffffL, 0xffffffL,
            0x1ffffffL, 0x3ffffffL, 0x7ffffffL, 0xfffffffL,
            0x1fffffffL, 0x3fffffffL, 0x7fffffffL, 0xffffffffL,
            0x1ffffffffL, 0x3ffffffffL, 0x7ffffffffL, 0xfffffffffL,
            0x1fffffffffL, 0x3fffffffffL, 0x7fffffffffL, 0xffffffffffL,
            0x1ffffffffffL, 0x3ffffffffffL, 0x7ffffffffffL, 0xfffffffffffL,
            0x1fffffffffffL, 0x3fffffffffffL, 0x7fffffffffffL, 0xffffffffffffL,
            0x1ffffffffffffL, 0x3ffffffffffffL, 0x7ffffffffffffL, 0xfffffffffffffL,
            0x1fffffffffffffL, 0x3fffffffffffffL, 0x7fffffffffffffL, 0xffffffffffffffL,
            0x1ffffffffffffffL, 0x3ffffffffffffffL, 0x7ffffffffffffffL, 0xfffffffffffffffL,
            0x1fffffffffffffffL, 0x3fffffffffffffffL, 0x7fffffffffffffffL, 0xffffffffffffffffL,
    };

    // 起始IP
    private IpLong begin;
    // 最后IP
    private IpLong end;

    public Segment(final String ips) {
        if (ips == null || ips.isEmpty()) {
            throw new IllegalArgumentException("ips is empty.");
        }
        int length = ips.length();
        if (length == 1) {
            switch (ips.charAt(0)) {
                case '-':
                case '*':
                    begin = Ipv4.IP_MIN;
                    end = Ipv4.IP_MAX;
                    break;
            }
        } else {
            int pos = ips.indexOf('-');
            if (pos == 0) {
                begin = Ipv4.IP_MIN;
                end = new IpLong(ips.substring(1));
            } else if (pos == length - 1) {
                begin = new IpLong(ips.substring(0, length - 1));
                end = Ipv4.IP_MAX;
            } else if (pos > 0) {
                // IP-IP格式
                begin = new IpLong(ips.substring(0, pos));
                end = new IpLong(ips.substring(pos + 1));
            } else {
                pos = ips.lastIndexOf('/');
                if (pos == 0 || pos == length - 1) {
                    throw new IllegalArgumentException(String.format("ips is invalid. %s", ips));
                } else {
                    boolean ipv4 = ips.indexOf(':') == -1;
                    if (pos > 0) {
                        // IP/掩码格式
                        int bits = Integer.parseInt(ips.substring(pos + 1));
                        if (bits < 1 || ipv4 && bits > 32 || bits > 128) {
                            throw new IllegalArgumentException(String.format("ips is invalid. %s", ips));
                        }
                        if (!ipv4) {
                            //ipv6
                            begin = new IpLong(ips.substring(0, pos));
                            if (bits < 64) {
                                begin = new IpLong(begin.getHigh() & IPV6_MASKES[bits - 1], 0x0L);
                                end = new IpLong(begin.getHigh() | IPV6_MASKES[64 - bits - 1], 0xFFFFFFFFFFFFFFFFL);
                            } else if (bits == 64) {
                                begin = new IpLong(begin.getHigh(), begin.getLow());
                                end = new IpLong(begin.getHigh(), 0xFFFFFFFFFFFFFFFFL);
                            } else if (bits == 128) {
                                begin = new IpLong(begin.getHigh(), begin.getLow());
                                end = new IpLong(begin.getHigh(), begin.getLow());
                            } else {
                                begin = new IpLong(begin.getHigh(), begin.getLow() & (IPV6_MASKES[bits - 64 - 1] << (128 - bits)));
                                end = new IpLong(begin.getHigh(), begin.getLow() | IPV6_MASKES[128 - bits - 1]);
                            }
                        } else {
                            // ipv4处理cdir
                            begin = new IpLong(IPV4_MASKES[bits - 1]);
                            long mask = begin.getLow();
                            end = new IpLong(ips.substring(0, pos));
                            begin = new IpLong(end.getLow() & mask);
                            end = new IpLong(begin.getLow() + ~((int) mask));
                        }
                    } else if (ipv4) {
                        // ipv4，处理'*'号。常用写法如：192.168.1.*
                        begin = new IpLong(ips.replaceAll("\\*", "0"));
                        end = new IpLong(ips.replaceAll("\\*", "255"));
                    }
                }
            }
        }
    }

    public IpLong getBegin() {
        return begin;
    }

    public IpLong getEnd() {
        return end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Segment segment = (Segment) o;

        if (!begin.equals(segment.begin)) {
            return false;
        }
        return end.equals(segment.end);
    }

    @Override
    public int hashCode() {
        int result = begin.hashCode();
        result = 31 * result + end.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return begin.toString() + "-" + end.toString();
    }

    /**
     * 是否包含指定IP
     *
     * @param ip IP
     * @return 布尔值
     */
    public boolean contains(final String ip) {
        return ip != null && contains(new IpLong(ip));
    }

    /**
     * 是否包含指定IP
     *
     * @param ip ip
     * @return 布尔值
     */
    public boolean contains(final IpLong ip) {
        return ip != null && begin.compareTo(ip) <= 0 && end.compareTo(ip) >= 0;
    }
}
