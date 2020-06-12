package io.joyrpc.codec;

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

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * 16进制字符编码
 */
public class HexCodec {

    protected static final char HEX[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    public static HexCodec INSTANCE = new HexCodec();

    /**
     * 把字节数组用十六进制表示出来
     *
     * @param source 字节数组
     * @return 十六进制字符串
     */
    public String encode(final byte[] source) {
        if (source == null || source.length == 0) {
            return "";
        }
        // 把密文转换成十六进制的字符串形式
        char str[] = new char[source.length * 2];
        int k = 0;
        for (byte b : source) {
            str[k++] = HEX[b >>> 4 & 0xf];
            str[k++] = HEX[b & 0xf];
        }
        return new String(str);
    }

    /**
     * 编解码
     *
     * @param source
     * @param charset
     * @return
     * @throws Exception
     */
    public String encode(final byte[] source, final Charset charset) {
        return encode(source);
    }

    /**
     * 把十六进制表示出的字符串还原为字节数组
     *
     * @param source 十六进制字符串
     * @return 字节数组
     */
    public byte[] decode(final String source) throws IOException {
        if (source == null) {
            return null;
        } else if (source.isEmpty()) {
            return new byte[0];
        }
        int length = source.length();
        if ((length % 2) != 0) {
            throw new IOException("length must be a power of 2");
        }
        int size = length >> 1;
        byte[] result = new byte[size];
        char[] chars = source.toCharArray();
        int i = 0;
        int pos;
        byte value;
        for (char ch : chars) {
            switch (ch) {
                case '0':
                    value = 0;
                    break;
                case '1':
                    value = 1;
                    break;
                case '2':
                    value = 2;
                    break;
                case '3':
                    value = 3;
                    break;
                case '4':
                    value = 4;
                    break;
                case '5':
                    value = 5;
                    break;
                case '6':
                    value = 6;
                    break;
                case '7':
                    value = 7;
                    break;
                case '8':
                    value = 8;
                    break;
                case '9':
                    value = 9;
                    break;
                case 'a':
                case 'A':
                    value = 10;
                    break;
                case 'b':
                case 'B':
                    value = 11;
                    break;
                case 'c':
                case 'C':
                    value = 12;
                    break;
                case 'd':
                case 'D':
                    value = 13;
                    break;
                case 'e':
                case 'E':
                    value = 14;
                    break;
                case 'f':
                case 'F':
                    value = 15;
                    break;
                default:
                    throw new IOException("invalid hex string");

            }
            pos = i >> 1;
            if ((i & 0x1) == 0) {
                result[pos] = value;
            } else {
                result[pos] = (byte) (result[pos] << 4 | value);
            }
            i++;
        }
        return result;
    }

    /**
     * 解码
     *
     * @param source
     * @param charset
     * @return
     * @throws IOException
     */
    public byte[] decode(final String source, final Charset charset) throws IOException {
        return decode(source);
    }
}
