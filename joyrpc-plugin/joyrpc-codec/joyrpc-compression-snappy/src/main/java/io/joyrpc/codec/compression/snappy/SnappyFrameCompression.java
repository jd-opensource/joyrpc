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
 * Snappy压缩，没有计算校验和
 */
@Extension(value = "snappyf", provider = "pure", order = Compression.SNAPPY_FRAME_ORDER)
public class SnappyFrameCompression implements Compression {
    @Override
    public byte getTypeId() {
        return SNAPPY_FRAME;
    }

    @Override
    public OutputStream compress(final OutputStream out) throws IOException {
        return new SnappyFramedOutputStream(out);
    }

    @Override
    public InputStream decompress(final InputStream input) throws IOException {
        return new SnappyFramedInputStream(input);
    }

    @Override
    public String getTypeName() {
        return "snappyf";
    }
}
