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

/**
 * 16进制
 */
public abstract class Hex {

    /**
     * 把字节数组用十六进制表示出来
     *
     * @param source 字节数组
     * @return 十六进制字符串
     */
    public static String encode(final byte[] source) {
        return HexCodec.INSTANCE.encode(source);
    }

    /**
     * 把字节数组用十六进制表示出来
     *
     * @param source 字节数组
     * @return 十六进制字符串
     * @throws IOException
     */
    public static byte[] decode(final String source) throws IOException {
        return HexCodec.INSTANCE.decode(source);
    }
}
