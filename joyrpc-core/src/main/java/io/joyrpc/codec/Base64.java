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
 * Base64编解码
 */
public abstract class Base64 {

    /**
     * BASE64编码数据
     *
     * @param data 数据
     * @return BASE64字符串
     */
    public static String encode(final byte[] data) {
        return Base64Codec.INSTANCE.encode(data);
    }

    /**
     * BASE64编码数据
     *
     * @param data    数据
     * @param options 编码参数
     * @return BASE64字符串
     */
    public static String encode(final byte[] data, final int options) {
        return Base64Codec.INSTANCE.encode(data, options);
    }

    /**
     * BASE64编码数据
     *
     * @param data    数据
     * @param options 编码参数
     * @return BASE64字符串
     */
    public static String encode(final byte[] data, final int options, final Charset charset) {
        return Base64Codec.INSTANCE.encode(data, 0, data.length, options, charset);
    }

    /**
     * BASE64解码数据
     *
     * @param str BASE64字符串
     * @return 数据
     * @throws IOException
     */
    public static byte[] decode(final String str) throws IOException {
        return Base64Codec.INSTANCE.decode(str);
    }

    /**
     * BASE64解码数据
     *
     * @param str     BASE64字符串
     * @param options 解码参数
     * @return 数据，错误返回null
     * @throws IOException
     */
    public static byte[] decode(final String str, final int options) throws IOException {
        return Base64Codec.INSTANCE.decode(str, options);
    }

    /**
     * BASE64解码数据
     *
     * @param str     BASE64字符串
     * @param options 解码参数
     * @param charset 字符集
     * @return 数据
     * @throws IOException
     */
    public static byte[] decode(final String str, final int options, final Charset charset) throws IOException {
        return Base64Codec.INSTANCE.decode(str, options, charset);
    }

}
