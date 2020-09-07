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
     * 类型
     */
    protected IpType type;
    /**
     * IP信息
     */
    protected String ip;

    public IpLong(final String ip) {
        IpPart part = parseIp(ip);
        if (part == null) {
            throw new IllegalArgumentException(String.format("invalid ip %s", ip));
        }
        this.ip = ip;
        this.type = part.type;
        int index = 0;
        int[] parts = part.parts;
        if (parts.length == 8) {
            //ipv6
            high += ((long) parts[index++]) << 48;
            high += ((long) (parts[index++]) << 32);
            high += ((long) (parts[index++]) << 16);
            high += parts[index++];
            low += ((long) parts[index++]) << 48;
            low += ((long) (parts[index++]) << 32);
            low += ((long) (parts[index++]) << 16);
            low += ((long) (parts[index]));
        } else {
            high = -1;
            //ipv4
            low += ((long) parts[index++]) << 24;
            low += ((long) (parts[index++]) << 16);
            low += ((long) (parts[index++]) << 8);
            low += ((long) (parts[index]));
        }
    }

    public IpLong(long low) {
        this.high = -1;
        this.low = low;
        this.type = IpType.IPV4;
    }

    public IpLong(long high, long low) {
        this.high = high;
        this.low = low;
        this.type = IpType.IPV6;
    }

    public IpLong(long high, long low, IpType type) {
        this.high = high;
        this.low = low;
        this.type = type;
        this.ip = null;
    }

    public long getHigh() {
        return high;
    }

    public long getLow() {
        return low;
    }

    public IpType getType() {
        return type;
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
                //ipv6，自动缩略2个及以上的0000:0000
                int[] parts = new int[8];
                parts[0] = (int) ((high & 0xFFFFFFFFFFFFFFFFL) >>> 48);
                parts[1] = (int) ((high & 0xFFFFFFFFFFFL) >>> 32);
                parts[2] = (int) ((high & 0xFFFFFFFFL) >>> 16);
                parts[3] = (int) ((high & 0xFFFFL));
                parts[4] = (int) ((low & 0xFFFFFFFFFFFFFFFFL) >>> 48);
                parts[5] = (int) ((low & 0xFFFFFFFFFFFFL) >>> 32);
                parts[6] = (int) ((low & 0xFFFFFFFFL) >>> 16);
                parts[7] = (int) (low & 0xFFFFL);
                int end = type == IpType.IPV6 ? 7 : 5;
                int zero = 0;
                int zeroIndex = -1;
                int maxZero = 0;
                int maxZeroIndex = 0;
                for (int i = 0; i <= end; i++) {
                    if (parts[i] == 0) {
                        if (zero++ == 0) {
                            zeroIndex = i;
                        }
                    } else if (zero > 0) {
                        if (maxZero < zero) {
                            maxZero = zero;
                            maxZeroIndex = zeroIndex;
                        }
                        zero = 0;
                        zeroIndex = -1;
                    }
                }
                if (zero > 0 && maxZero < zero) {
                    maxZero = zero;
                    maxZeroIndex = zeroIndex;
                }
                if (maxZero < 2) {
                    // 两个以上再省略
                    maxZero = 0;
                    maxZeroIndex = 0;
                }
                for (int i = 0; i < maxZeroIndex; i++) {
                    append(builder.append(i > 0 ? ":" : ""), parts[i]);
                }
                if (maxZero > 0) {
                    builder.append("::");
                }
                for (int i = maxZeroIndex + maxZero; i <= end; i++) {
                    append(builder, parts[i]).append(i < 7 ? ":" : "");
                }
                if (type == IpType.MIXER) {
                    // mixer
                    builder.append(parts[6] >>> 8).append('.');
                    builder.append(parts[6] & 0xFF).append('.');
                    builder.append(parts[7] >>> 8).append('.');
                    builder.append(parts[7] & 0xFF);
                }
            }
            ip = builder.toString();
        }
        return ip;
    }

    /**
     * 分解IP<br/>
     * ipv6与ipv4之间的转换<br/>
     * 在兼容情况下：如果ipv4表示为“X.X.X.X”，那么对应的ipv6即为“::X.X.X.X”（高位补零） <br/>
     * 在映射情况下：如果ipv6表示为“::FFFF:X.X.X.X”（33-128位为::FFFF），这类情况下的ipv6将会被映射为ipv4
     *
     * @param ip ip地址
     * @return 分段
     */
    public static IpPart parseIp(final String ip) {
        if (ip == null || ip.isEmpty()) {
            return null;
        }
        IpType ipType = null;
        int[] parts = new int[8];
        int index = 0;
        int start = -1;
        int end = -1;
        int part;
        int colon = 0;
        int ellipsis = -1;//占位符索引
        int ipv4Index = -1;//ipv4起始位置，用于混合兼容情况
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
                    if (ipType == IpType.IPV4 || ipType == IpType.MIXER) {
                        //ipv4后不能再出现ipv6
                        return null;
                    }
                    ipType = IpType.IPV6;
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
                    if (start == -1) {
                        // 前面必须有字符
                        return null;
                    }
                    if (ipv4Index == -1) {
                        //ipv4起始位置
                        ipv4Index = index;
                    }
                    //支持混合模式
                    ipType = ipType == null || ipType == IpType.IPV4 ? IpType.IPV4 : IpType.MIXER;
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
                    if (ipType == IpType.IPV4 || ipType == IpType.MIXER) {
                        //ipv4后不能再出现ipv6
                        return null;
                    }
                    ipType = IpType.IPV6;
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
            part = Integer.parseInt(new String(chars, start, end - start + 1), ipType == IpType.IPV4 || ipType == IpType.MIXER ? 10 : 16);
            if (part > (ipType == IpType.IPV4 || ipType == IpType.MIXER ? 0xFF : 0xFFFF)) {
                //优先ipv4
                return null;
            }
            parts[index++] = part;
            if (ipType == IpType.IPV4) {
                // 纯ipv4
                return index == 4 ? new IpPart(ipType, Arrays.copyOfRange(parts, 0, 4)) : null;
            } else if (ipType == IpType.MIXER) {
                // 混合模式
                if (index - ipv4Index < 4) {
                    // ipv4小于4段
                    return null;
                }
                // 变成ipv6
                parts[ipv4Index] = (parts[ipv4Index] << 8) | parts[ipv4Index + 1];
                parts[ipv4Index + 1] = (parts[ipv4Index + 2] << 8) | parts[ipv4Index + 3];
                index -= 2;
            }
        }
        // ipv6或者mixer
        if (index > 8) {
            return null;
        } else if (ellipsis == -1) {
            return index == 8 ? new IpPart(ipType, parts) : null;
        }
        int[] result = new int[8];
        //省略号前
        for (int i = 0; i < ellipsis; i++) {
            result[i] = parts[i];
        }
        //省略号代表的
        int max = ellipsis + (7 - index + 1);
        for (int i = ellipsis; i <= max; i++) {
            result[i] = 0x0000;
        }
        //省略号后
        for (int i = ellipsis + 1; i < index; i++) {
            result[max + i - ellipsis] = parts[i];
        }
        return new IpPart(ipType, result);
    }

}
