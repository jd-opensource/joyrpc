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
import io.joyrpc.codec.compression.Finishable;
import io.joyrpc.extension.Extension;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Zlib压缩算法
 */
@Extension(value = "zlib", provider = "java", order = Compression.ZLIB_ORDER)
public class ZlibCompression implements Compression {

    @Override
    public byte getTypeId() {
        return ZLIB;
    }

    @Override
    public String getTypeName() {
        return "zlib";
    }

    @Override
    public OutputStream compress(final OutputStream out) {
        return new MyDeflaterOutputStream(out);
    }

    @Override
    public InputStream decompress(final InputStream input) {
        return new InflaterInputStream(input);
    }

    /**
     * 压缩
     */
    protected static class MyDeflaterOutputStream extends DeflaterOutputStream implements Finishable {

        public MyDeflaterOutputStream(OutputStream out) {
            super(out);
        }
    }
}
