package io.joyrpc.codec.compression.zlib;

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

import io.joyrpc.codec.compression.Compression;
import io.joyrpc.extension.Extension;

/**
 * Deflate压缩
 */
@Extension(value = "deflate", provider = "java", order = Compression.DEFLATE_ORDER)
public class DeflateCompression extends ZlibCompression {

    @Override
    public byte getTypeId() {
        return Compression.DEFLATE;
    }

    @Override
    public String getTypeName() {
        return "deflate";
    }
}
