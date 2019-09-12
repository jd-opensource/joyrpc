package io.joyrpc.codec.compression.lz4;

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
import io.joyrpc.extension.condition.ConditionalOnClass;
import org.apache.commons.compress.compressors.lz4.BlockLZ4CompressorInputStream;
import org.apache.commons.compress.compressors.lz4.BlockLZ4CompressorOutputStream;
import org.apache.commons.compress.compressors.lz77support.Parameters;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Lz4压缩算法
 */
@Extension(value = "lz4", provider = "commons-compress", order = Compression.LZ4_ORDER)
@ConditionalOnClass("org.apache.commons.compress.compressors.lz4.BlockLZ4CompressorOutputStream")
public class Lz4Compression implements Compression {
    @Override
    public byte getTypeId() {
        return LZ4;
    }

    @Override
    public OutputStream compress(final OutputStream out) throws IOException {
        return new MyBlockLZ4CompressorOutputStream(out);
    }

    @Override
    public InputStream decompress(final InputStream input) throws IOException {
        return new BlockLZ4CompressorInputStream(input);
    }

    @Override
    public String getTypeName() {
        return "lz4";
    }

    /**
     * 覆盖flush操作
     */
    protected static class MyBlockLZ4CompressorOutputStream extends BlockLZ4CompressorOutputStream implements Finishable {

        protected final OutputStream os;

        public MyBlockLZ4CompressorOutputStream(OutputStream os) throws IOException {
            super(os);
            this.os = os;
        }

        public MyBlockLZ4CompressorOutputStream(OutputStream os, Parameters params) throws IOException {
            super(os, params);
            this.os = os;
        }

        @Override
        public void flush() throws IOException {
            os.flush();
        }
    }
}
