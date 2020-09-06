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

import java.util.Arrays;

/**
 * IP以Long表示
 */
public class IpLong implements Comparable<IpLong> {
    /**
     * IPV6高8字节
     */
    protected long high;
    /**
     * IPV6低8字节或IPV4的4字节
     */
    protected long low;
    /**
     * IP信息
     */
    protected String ip;

    public IpLong(final String ip) {
        int[] data = parseIp(ip);
        if (data == null) {
            throw new IllegalArgumentException(String.format("invalid ip %s", ip));
        }
        this.ip = ip;
        int index = 0;
        if (data.length == 8) {
            //ipv6
            high += ((long) data[index++]) << 48;
            high += ((long) (data[index++]) << 32);
            high += ((long) (data[index++]) << 16);
            high += data[index++];
            low += ((long) data[index++]) << 48;
            low += ((long) (data[index++]) << 32);
            low += ((long) (data[index++]) << 16);
            low += ((long) (data[index]));
        } else {
            high = -1;
            //ipv4
            low += ((long) data[index++]) << 24;
            low += ((long) (data[index++]) << 16);
            low += ((long) (data[index++]) << 8);
            low += ((long) (data[index]));
        }
    }

    public IpLong(long low) {
        high = -1;
        this.low = low;
    }

    public IpLong(long high, long low) {
        this.high = high;
        this.low = low;
    }

    public long getHigh() {
        return high;
    }

    public long getLow() {
        return low;
    }

    @Override
    public int compareTo(final IpLong o) {
        if (o == null && this == null) {
            return 0;
        } else if (o == null) {
            return 1;
        } else if (this == null) {
            return -1;
        } else if (high > o.high) {
            return 1;
        } else if (high < o.high) {
            return -1;
        } else if (low > o.low) {
            return 1;
        } else if (low < o.low) {
            return -1;
        } else {
            return 0;
        }
    }

    protected StringBuilder append(final StringBuilder builder, final long value) {
        String st = Long.toHexString(value).toUpperCase();
        switch (st.length()) {
            case 1:
                builder.append("000").append(st);
                break;
            case 2:
                builder.append("00").append(st);
                break;
            case 3:
                builder.append("0").append(st);
                break;
            default:
                builder.append(st);
                break;
        }
        return builder;
    }

    @Override
    public String toString() {
        if (ip == null) {
            StringBuilder builder = new StringBuilder(40);
            if (high < 0) {
                //ipv4
                builder.append((low & 0xFFFFFFFFL) >>> 24).append('.')
                        .append((low & 0xFFFFFFL) >>> 16).append('.')
                        .append((low & 0xFFFFL) >>> 8).append('.')
                        .append(low & 0xFFL);
            } else {
                //ipv6
                append(builder, (high & 0xFFFFFFFFFFFFFFFFL) >>> 48).append(':');
                append(builder, (high & 0xFFFFFFFFFFFL) >>> 32).append(':');
                append(builder, (high & 0xFFFFFFFFL) >>> 16).append(':');
                append(builder, (high & 0xFFFFL)).append(':');
                append(builder, (low & 0xFFFFFFFFFFFFFFFFL) >>> 48).append(':');
                append(builder, (low & 0xFFFFFFFFFFFL) >>> 32).append(':');
                append(builder, (low & 0xFFFFFFFFL) >>> 16).append(':');
                append(builder, (low & 0xFFFFL));
            }
            return builder.toString();
        }
        return ip;
    }

    /**
     * 分解IP
     *
     * @param ip ip地址
     * @return 分段
     */
    public static int[] parseIp(final String ip) {
        if (ip == null || ip.isEmpty()) {
            return null;
        }
        boolean ipv4 = false;
        boolean ipv6 = false;
        int[] parts = new int[8];
        int index = 0;
        int start = -1;
        int end = -1;
        int part;
        int colon = 0;
        int ellipsis = -1;//占位符索引
        char[] chars = ip.toCharArray();
        char ch = 0;
        for (int i = 0; i < chars.length; i++) {
            ch = chars[i];
            switch (ch) {
                case 'a':
                case 'b':
                case 'c':
                case 'd':
                case 'e':
                case 'f':
                case 'A':
                case 'B':
                case 'C':
                case 'D':
                case 'E':
                case 'F':
                    if (ipv4) {
                        return null;
                    }
                    ipv6 = true;
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    if (start == -1) {
                        start = i;
                    }
                    end = i;
                    if (end - start > 4) {
                        // 超长了
                        return null;
                    }
                    colon = 0;
                    break;
                case '.':
                    if (ipv6) {
                        return null;
                    }
                    ipv4 = true;
                    if (start == -1) {
                        // 前面必须有字符
                        return null;
                    }
                    part = Integer.parseInt(new String(chars, start, end - start + 1));
                    if (part > 0xFF) {
                        // ipv4每个部分最大255
                        return null;
                    }
                    parts[index++] = part;
                    start = -1;
                    end = -1;
                    break;
                case ':':
                    if (ipv4) {
                        return null;
                    }
                    ipv6 = true;
                    //判断冒号数量
                    switch (++colon) {
                        case 1:
                            if (i == 0) {
                                //第一个字符，后面还需要一个':'
                                if (i >= chars.length - 1 || chars[i] != ':') {
                                    return null;
                                }
                            } else {
                                part = Integer.parseInt(new String(chars, start, end - start + 1), 16);
                                if (part > 0xFFFF) {
                                    // ipv4每个部分最大65535
                                    return null;
                                }
                                parts[index++] = part;
                                start = -1;
                                end = -1;
                            }
                            break;
                        case 2:
                            //ipv6只能出现一个"::"
                            if (ellipsis >= 0) {
                                return null;
                            }
                            ellipsis = index;
                            parts[index++] = -1;
                            break;
                        default:
                            return null;
                    }
                    break;
                default:
                    return null;
            }
        }
        if ((start == -1 && colon == 0) || colon == 1) {
            // 以'.'或者':'结尾
            return null;
        } else if (start > -1) {
            // 以数字结尾
            part = Integer.parseInt(new String(chars, start, end - start + 1), ipv6 ? 16 : 10);
            if (part > (ipv6 ? 0xFFFF : 0xFF)) {
                return null;
            }
            parts[index++] = part;
            if (ipv4) {
                return index == 4 ? Arrays.copyOfRange(parts, 0, 3) : null;
            }
        }
        // ipv6
        if (index > 8) {
            return null;
        } else if (ellipsis == -1) {
            return index == 8 ? parts : null;
        }
        int[] result = new int[8];
        for (int i = 0; i < ellipsis; i++) {
            result[i] = parts[i];
        }
        for (int i = index - 1; i > ellipsis; i--) {
            result[i] = parts[i];
        }
        int max = ellipsis + (7 - index + 1);
        for (int i = ellipsis; i <= max; i++) {
            result[i] = 0xFFFF;
        }
        return result;
    }
}
