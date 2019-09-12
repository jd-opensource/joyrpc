package io.joyrpc.codec.checksum;

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

import io.joyrpc.codec.CodecType;
import io.joyrpc.extension.Extension;

import java.nio.ByteBuffer;

/**
 * 校验和
 */
@Extension("checksum")
public interface Checksum extends CodecType {

    byte NONE = 0;

    byte CRC32 = 1;

    byte CRC32C = 2;

    /**
     * JAVA实现的CRC32的顺序值
     */
    int JAVA_CRC32_ORDER = 100;

    /**
     * 计算
     *
     * @param data
     * @return
     */
    default long compute(final byte[] data) {
        return compute(data, 0, data.length);
    }

    /**
     * 计算
     *
     * @param data
     * @param offset
     * @param length
     * @return
     */
    long compute(byte[] data, int offset, int length);

    /**
     * 计算
     *
     * @param buffer
     * @return
     */
    default long compute(final ByteBuffer buffer) {
        int pos = buffer.position();
        int limit = buffer.limit();
        int remaining = buffer.remaining();
        if (remaining <= 0) {
            return 0;
        } else if (buffer.hasArray()) {
            return compute(buffer.array(), pos + buffer.arrayOffset(), remaining);
        } else {
            byte[] b = new byte[remaining];
            buffer.get(b);
            buffer.position(limit);
            return compute(b, 0, b.length);
        }
    }
}
