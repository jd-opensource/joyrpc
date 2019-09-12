package io.joyrpc.codec.compression.gzip;

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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Gzip压缩算法
 */
@Extension(value = "gzip", provider = "java")
public class GzipCompression implements Compression {

    @Override
    public byte getTypeId() {
        return GZIP;
    }

    @Override
    public String getTypeName() {
        return "gzip";
    }

    @Override
    public OutputStream compress(final OutputStream out) throws IOException {
        return new MyGZIPOutputStream(out);
    }

    @Override
    public InputStream decompress(final InputStream input) throws IOException {
        return new GZIPInputStream(input);
    }

    /**
     * 压缩
     */
    protected static class MyGZIPOutputStream extends GZIPOutputStream implements Finishable {

        public MyGZIPOutputStream(final OutputStream out) throws IOException {
            super(out);
        }

    }
}
