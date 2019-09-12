package io.joyrpc.codec.checksum.crc32;

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

import io.joyrpc.codec.checksum.Checksum;
import io.joyrpc.extension.Extension;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;

import static io.joyrpc.codec.checksum.Checksum.JAVA_CRC32_ORDER;

/**
 * 本地的CRC32实现
 */
@Extension(value = "crc32", provider = "java", order = JAVA_CRC32_ORDER)
public class NativeCrc32Checksum implements Checksum {

    @Override
    public byte getTypeId() {
        return CRC32;
    }

    @Override
    public long compute(byte[] data, int offset, int length) {
        CRC32 crc32 = new CRC32();
        crc32.update(data, offset, length);
        return (int) crc32.getValue();
    }

    @Override
    public long compute(final ByteBuffer buffer) {
        CRC32 crc32 = new CRC32();
        crc32.update(buffer);
        return crc32.getValue();
    }
}
