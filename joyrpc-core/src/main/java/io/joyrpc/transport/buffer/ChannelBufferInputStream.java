package io.joyrpc.transport.buffer;

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

import io.joyrpc.codec.ArrayInputStream;

import java.io.InputStream;

/**
 * @date: 2019/3/25
 */
public class ChannelBufferInputStream extends InputStream implements ArrayInputStream {
    //缓冲区
    protected ChannelBuffer buffer;
    //最大位置
    protected int endIndex;

    public ChannelBufferInputStream(ChannelBuffer buffer) {
        this(buffer, buffer.readableBytes());
    }

    public ChannelBufferInputStream(ChannelBuffer buffer, int length) {
        if (length < 0) {
            throw new IllegalArgumentException("length: " + length);
        }
        if (length > buffer.readableBytes()) {
            throw new IndexOutOfBoundsException();
        }
        this.buffer = buffer;
        this.endIndex = buffer.readerIndex() + length;
    }

    @Override
    public int read() {
        return buffer.isReadable() ? buffer.readByte() & 0xff : -1;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) {
        int available = available();
        if (available <= 0) {
            return -1;
        }
        int length = Math.min(available, len);
        buffer.readBytes(b, off, length);
        return length;
    }

    @Override
    public int available() {
        return endIndex - buffer.readerIndex();
    }

    @Override
    public boolean hasArray() {
        return buffer.hasArray();
    }

    @Override
    public byte[] array() {
        return buffer.array();
    }

    @Override
    public int arrayOffset() {
        return buffer.arrayOffset();
    }

    @Override
    public int readerIndex() {
        return buffer.readerIndex();
    }
}
