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
    public static final String[] MASKES =
            new String[]{"128.0.0.0", "192.0.0.0", "224.0.0.0", "240.0.0.0", "248.0.0.0", "252.0.0.0", "254.0.0.0",
                    "255.0.0.0", "255.128.0.0", "255.192.0.0", "255.224.0.0", "255.240.0.0", "255.248.0.0",
                    "255.252.0.0", "255.254.0.0", "255.255.0.0", "255.255.128.0", "255.255.192.0", "255.255.224.0",
                    "255.255.240.0", "255.255.248.0", "255.255.252.0", "255.255.254.0", "255.255.255.0",
                    "255.255.255.128", "255.255.255.192", "255.255.255.224", "255.255.255.240", "255.255.255.248",
                    "255.255.255.252", "255.255.255.254", "255.255.255.255"};

    // 起始IP
    private IpLong begin;
    // 最后IP
    private IpLong end;

    public Segment(final String ips) {
        if (ips == null || ips.isEmpty()) {
            throw new IllegalArgumentException("ips is empty.");
        }
        //TODO 修改IP段
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
                pos = ips.indexOf('/');
                if (pos == 0 || pos == length - 1) {
                    throw new IllegalArgumentException(String.format("ips is invalid. %s", ips));
                } else if (pos > 0) {
                    // IP/掩码格式
                    int bits = Integer.parseInt(ips.substring(pos + 1));
                    if (bits < 1 || bits > 32) {
                        throw new IllegalArgumentException(String.format("ips is invalid. %s", ips));
                    }
                    //long mask = (int) Ipv4.toLong(MASKES[bits - 1]);
                    //begin = Ipv4.toLong(ips.substring(0, pos)) & mask;
                   // end = begin + ~((int) mask);
                } else {
                    // 可能存在*号
                    begin = new IpLong(ips.replaceAll("\\*", "0"));
                    end = new IpLong(ips.replaceAll("\\*", "255"));
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
