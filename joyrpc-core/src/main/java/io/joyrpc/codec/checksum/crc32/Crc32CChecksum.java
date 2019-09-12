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

/**
 * 纯净的CRC32-C的Java实现，100以下的小字节数组性能比crc32快
 */
@Extension(value = "crc32-c", provider = "c")
public class Crc32CChecksum implements Checksum {

    @Override
    public byte getTypeId() {
        return CRC32C;
    }

    @Override
    public long compute(final byte[] data, final int offset, final int length) {
        Crc32C crc32c = new Crc32C();
        crc32c.update(data, 0, data.length);
        return crc32c.getValue();
    }

}
