package io.joyrpc.codec;

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

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * 线程不安全的字节数组输出流
 */
public class UnsafeByteArrayOutputStream extends OutputStream {

    protected static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    protected byte buf[];

    protected int count;

    public UnsafeByteArrayOutputStream() {
        this(32);
    }

    public UnsafeByteArrayOutputStream(final int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Negative initial size: " + size);
        }
        buf = new byte[size];
    }

    /**
     * 确保容量
     *
     * @param minCapacity
     */
    protected void ensureCapacity(final int minCapacity) {
        // overflow-conscious code
        if (minCapacity - buf.length > 0) {
            // overflow-conscious code
            int oldCapacity = buf.length;
            int newCapacity = oldCapacity << 1;
            if (newCapacity - minCapacity < 0) {
                newCapacity = minCapacity;
            }
            if (newCapacity - MAX_ARRAY_SIZE > 0) {
                newCapacity = hugeCapacity(minCapacity);
            }
            buf = Arrays.copyOf(buf, newCapacity);
        }
    }

    protected static int hugeCapacity(final int minCapacity) {
        if (minCapacity < 0) {
            throw new OutOfMemoryError();
        }
        return (minCapacity > MAX_ARRAY_SIZE) ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
    }

    /**
     * 写字节
     *
     * @param b
     */
    public void write(final int b) {
        int capacity = count + 1;
        ensureCapacity(capacity);
        buf[count] = (byte) b;
        count = capacity;
    }

    /**
     * 写字节数组
     *
     * @param b
     * @param off
     * @param len
     */
    public void write(final byte b[], final int off, final int len) {
        if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
        int capacity = count + len;
        ensureCapacity(capacity);
        System.arraycopy(b, off, buf, count, len);
        count = capacity;
    }

    /**
     * 大小
     *
     * @return
     */
    public int size() {
        return count;
    }

    /**
     * 重置
     */
    public void reset() {
        count = 0;
    }

    /**
     * 转换成字节数组
     *
     * @return
     */
    public byte[] toByteArray() {
        return Arrays.copyOf(buf, count);
    }

    /**
     * 转换成ByteBuffer
     *
     * @return
     */
    public ByteBuffer toByteBuffer() {
        return ByteBuffer.wrap(buf, 0, count);
    }

    /**
     * 写入到流
     *
     * @param out
     * @throws IOException
     */
    public void writeTo(final OutputStream out) throws IOException {
        out.write(buf, 0, count);
    }

    /**
     * 转换成字符串
     *
     * @return
     */
    public String toString() {
        return new String(buf, 0, count);
    }

    /**
     * 转换成字符串
     *
     * @param charset
     * @return
     * @throws UnsupportedEncodingException
     */
    public String toString(String charset) throws UnsupportedEncodingException {
        return new String(buf, 0, count, charset);
    }

    /**
     * 关闭
     *
     * @throws IOException
     */
    public void close() throws IOException {
    }
}
