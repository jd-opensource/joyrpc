package io.joyrpc.codec.serialization;

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
import java.io.InputStream;

/**
 * 不安全的字节数组输入流
 */
public class UnsafeByteArrayInputStream extends InputStream {

    protected byte buf[];

    protected int pos;

    protected int limit;

    protected int mark;

    public UnsafeByteArrayInputStream(final byte buf[]) {
        this(buf, 0, buf.length);
    }

    public UnsafeByteArrayInputStream(final byte buf[], final int offset) {
        this(buf, offset, buf.length - offset);
    }

    public UnsafeByteArrayInputStream(final byte buf[], final int offset, final int length) {
        this.buf = buf;
        pos = mark = offset;
        limit = Math.min(offset + length, buf.length);
    }

    /**
     * 读字节
     *
     * @return
     */
    public int read() {
        return (pos < limit) ? (buf[pos++] & 0xff) : -1;
    }

    /**
     * 读数据到缓冲区
     *
     * @param b
     * @param off
     * @param len
     * @return
     */
    public int read(final byte b[], final int off, int len) {
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (pos >= limit) {
            return -1;
        }
        int avail = limit - pos;
        if (len > avail) {
            len = avail;
        }
        if (len <= 0) {
            return 0;
        }
        System.arraycopy(buf, pos, b, off, len);
        pos += len;
        return len;
    }

    /**
     * 跳过
     *
     * @param len
     * @return
     */
    public long skip(long len) {
        if (pos + len > limit) {
            len = limit - pos;
        }
        if (len <= 0) {
            return 0;
        }
        pos += len;
        return len;
    }

    /**
     * 可读字节数
     *
     * @return
     */
    public int available() {
        return limit - pos;
    }

    /**
     * 是否支持标记
     *
     * @return
     */
    public boolean markSupported() {
        return true;
    }

    /**
     * 标记位置
     *
     * @param readAheadLimit
     */
    public void mark(int readAheadLimit) {
        mark = pos;
    }

    /**
     * 标记位置
     */
    public void mark() {
        mark = pos;
    }

    /**
     * 重置
     */
    public void reset() {
        pos = mark;
    }

    /**
     * 位置
     *
     * @return
     */
    public int position() {
        return pos;
    }

    /**
     * 设置位置
     *
     * @param newPosition
     */
    public void position(final int newPosition) {
        pos = newPosition;
    }

    /**
     * 关闭
     *
     * @throws IOException
     */
    public void close() throws IOException {
    }
}
