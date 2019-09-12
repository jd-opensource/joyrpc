package io.joyrpc.codec.compression;

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

import io.joyrpc.transport.buffer.ChannelBuffer;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 自适应压缩
 */
public class AdaptiveCompressOutputStream extends OutputStream implements Finishable {

    /**
     * 提供压缩流
     */
    protected Compression compression;

    /**
     * 压缩流
     */
    protected OutputStream out;
    /**
     * 底层的数据缓冲区
     */
    protected ChannelBuffer buffer;
    /**
     * 开启压缩的阈值
     */
    protected int threshold;
    /**
     * 总的字节数
     */
    protected int total;
    /**
     * 起始的写入位置
     */
    protected int writerIndex;

    protected boolean finished;

    protected boolean closed;

    /**
     * 构建自适应压缩流
     *
     * @param buffer      数据缓冲区.
     * @param compression 压缩流提供者.
     * @throws IllegalArgumentException
     */
    public AdaptiveCompressOutputStream(final ChannelBuffer buffer, final Compression compression) {
        this(buffer, compression, 2048);
    }

    /**
     * 构建自适应压缩流
     *
     * @param buffer      数据缓冲区.
     * @param compression 压缩流提供者.
     * @param threshold   缓冲区大小.
     * @throws IllegalArgumentException
     */
    public AdaptiveCompressOutputStream(final ChannelBuffer buffer, final Compression compression, final int threshold) {
        if (buffer == null) {
            throw new NullPointerException("buffer can not be null.");
        } else if (compression == null) {
            throw new NullPointerException("function can not be null.");
        } else if (threshold <= 0) {
            throw new IllegalArgumentException("Buffer threshold <= 0");
        }
        this.writerIndex = buffer.writerIndex();
        this.buffer = buffer;
        this.compression = compression;
        this.threshold = threshold;
    }

    public boolean isCompressed() {
        return out != null;
    }

    public int getTotal() {
        return total;
    }

    /**
     * 把数据缓冲区转换成压缩流
     */
    protected void compress() throws IOException {
        if (out == null) {
            //读取写入的数据
            int size = buffer.writerIndex() - writerIndex;
            if (size > 0) {
                byte[] bytes = new byte[size];
                buffer.getBytes(writerIndex, bytes);
                buffer.writerIndex(writerIndex);
                //转换成压缩流，再次写入数据
                out = compression.compress(buffer.outputStream());
                out.write(bytes);
            } else {
                out = compression.compress(buffer.outputStream());
            }
        }
    }

    /**
     * Writes the specified byte to this buffered output stream.
     *
     * @param b the byte to be written.
     * @throws IOException if an I/O error occurs.
     */
    public void write(int b) throws IOException {
        if (out != null) {
            //开启了压缩，直接输出到压缩流，避免二次拷贝
            out.write(b);
        } else {
            if (total >= threshold) {
                //超过了压缩阈值，转换成压缩流输出
                compress();
                out.write(b);
            } else {
                //写入缓冲区
                buffer.writeByte(b);
            }
        }
        total++;
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array
     * starting at offset <code>off</code> to this buffered output stream.
     *
     * <p> Ordinarily this method stores bytes from the given array into this
     * stream's buffer, flushing the buffer to the underlying output stream as
     * needed.  If the requested length is at least as large as this stream's
     * buffer, however, then this method will flush the buffer and write the
     * bytes directly to the underlying output stream.  Thus redundant
     * <code>BufferedOutputStream</code>s will not copy data unnecessarily.
     *
     * @param b   the data.
     * @param off the start offset in the data.
     * @param len the number of bytes to write.
     * @throws IOException if an I/O error occurs.
     */
    public void write(final byte b[], final int off, final int len) throws IOException {
        if (out != null) {
            //开启了压缩，直接输出到压缩缓冲区，避免二次拷贝数据
            out.write(b, off, len);
        } else if (len > threshold - total) {
            //超过了压缩阈值，转换成压缩流输出
            compress();
            out.write(b, off, len);
        } else {
            //写入原始值
            buffer.writeBytes(b, off, len);
        }
        total += len;
    }

    /**
     * 提交数据，该方法不做任何操作
     *
     * @throws IOException if an I/O error occurs.
     * @see #finish()
     */
    public void flush() throws IOException {
        if (out != null) {
            out.flush();
        }
    }

    @Override
    public void finish() throws IOException {
        if (!finished) {
            finished = true;
            if (out != null && out instanceof Finishable) {
                ((Finishable) out).finish();
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            finish();
            if (out != null) {
                out.close();
            }
        }
    }
}
