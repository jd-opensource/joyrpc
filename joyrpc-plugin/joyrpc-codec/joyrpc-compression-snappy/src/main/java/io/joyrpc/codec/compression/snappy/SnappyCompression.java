package io.joyrpc.codec.compression.snappy;

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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @date: 2019/8/15
 */
@Extension(value = "snappy", provider = "pure", order = Compression.SNAPPY_ORDER)
public class SnappyCompression implements Compression {

    @Override
    public OutputStream compress(OutputStream out) throws IOException {
        return new SnappyOutputStream(out);
    }

    @Override
    public InputStream decompress(InputStream input) throws IOException {
        return new SnappyInputStream(input);
    }

    @Override
    public byte getTypeId() {
        return SNAPPY;
    }

    @Override
    public String getTypeName() {
        return "snappy";
    }
}
