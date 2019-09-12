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

import io.joyrpc.codec.compression.Finishable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @date: 2019/8/15
 */
public class SnappyOutputStream extends ByteArrayOutputStream implements Finishable {

    protected OutputStream output;
    protected boolean finished;
    protected boolean closed;

    /**
     * 构造函数
     *
     * @param outputStream
     */
    public SnappyOutputStream(OutputStream outputStream) {
        super(1024);
        this.output = outputStream;
    }

    @Override
    public void flush() throws IOException {
        output.flush();
    }

    @Override
    public void finish() throws IOException {
        if (!finished) {
            byte[] compressedOut = new byte[SnappyCompressor.maxCompressedLength(buf.length)];
            int compressedSize = SnappyCompressor.compress(buf, 0, count, compressedOut, 0);
            output.write(compressedOut, 0, compressedSize);
            finished = true;
        }
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            try {
                finish();
            } finally {
                super.close();
                output.close();
            }
        }
    }

}
