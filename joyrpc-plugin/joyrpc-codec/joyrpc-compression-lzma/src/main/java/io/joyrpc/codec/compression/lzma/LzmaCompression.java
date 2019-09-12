package io.joyrpc.codec.compression.lzma;

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
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.LZMAInputStream;
import org.tukaani.xz.LZMAOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Lzma算法
 */
@Extension(value = "lzma", provider = "tukaani", order = Compression.LZMA_ORDER)
@ConditionalOnClass("org.tukaani.xz.LZMAOutputStream")
public class LzmaCompression implements Compression {
    @Override
    public byte getTypeId() {
        return LZMA;
    }

    @Override
    public OutputStream compress(final OutputStream out) throws IOException {
        return new MyLZMAOutputStream(out);
    }

    @Override
    public InputStream decompress(final InputStream input) throws IOException {
        return new LZMAInputStream(input, -1);
    }

    @Override
    public String getTypeName() {
        return "lzma";
    }

    /**
     * 覆盖flush操作
     */
    protected static class MyLZMAOutputStream extends LZMAOutputStream implements Finishable {

        protected OutputStream os;

        public MyLZMAOutputStream(OutputStream os) throws IOException {
            super(os, new LZMA2Options(), -1);
            this.os = os;
        }

        @Override
        public void flush() throws IOException {
            os.flush();
        }
    }
}
