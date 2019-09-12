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
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorInputStream;
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Lz4压缩算法
 */
@Extension(value = "lz4f", provider = "commons-compress", order = Compression.LZ4_FRAME_ORDER)
@ConditionalOnClass("org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorOutputStream")
public class Lz4FrameCompression implements Compression {
    @Override
    public byte getTypeId() {
        return LZ4_FRAME;
    }

    @Override
    public OutputStream compress(final OutputStream out) throws IOException {
        return new MyFramedLZ4CompressorOutputStream(out);
    }

    @Override
    public InputStream decompress(final InputStream input) throws IOException {
        return new FramedLZ4CompressorInputStream(input);
    }

    @Override
    public String getTypeName() {
        return "lz4f";
    }

    /**
     * 覆盖flush操作
     */
    protected static class MyFramedLZ4CompressorOutputStream extends FramedLZ4CompressorOutputStream implements Finishable {

        protected OutputStream out;

        public MyFramedLZ4CompressorOutputStream(OutputStream out) throws IOException {
            super(out, new Parameters(BlockSize.K64, false, false, false));
            this.out = out;
        }

        public MyFramedLZ4CompressorOutputStream(OutputStream out, Parameters params) throws IOException {
            super(out, params);
            this.out = out;
        }

        @Override
        public void flush() throws IOException {
            out.flush();
        }
    }
}
