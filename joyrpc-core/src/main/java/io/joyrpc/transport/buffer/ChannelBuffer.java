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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @date: 2019/1/7
 */
public interface ChannelBuffer {

    int capacity();

    ChannelBuffer capacity(int newCapacity);

    /**
     * 确保数据容量
     *
     * @param minWritableBytes 最小可写字节数
     */
    void ensureWritable(int minWritableBytes);

    void clear();

    ChannelBuffer copy();

    ChannelBuffer copy(int index, int length);

    void discardReadBytes();

    boolean equals(Object o);

    short getUnsignedByte(int index);

    int getUnsignedShort(int var1);

    byte getByte(int index);

    short getShort(int index);

    int getInt(int index);

    long getLong(int index);

    ChannelBuffer getBytes(int index, byte[] dst);

    ChannelBuffer getBytes(int index, byte[] dst, int dstIndex, int length);

    ChannelBuffer getBytes(int index, ByteBuffer dst);

    ChannelBuffer getBytes(int index, ChannelBuffer dst);

    boolean isReadable();

    int readableBytes();

    byte readByte();

    void readBytes(byte[] dst);

    void readBytes(byte[] dst, int dstIndex, int length);

    void readBytes(ChannelBuffer dst);

    ChannelBuffer readBytes(OutputStream out, int length) throws IOException;

    void resetReaderIndex();

    void resetWriterIndex();

    int readerIndex();

    void readerIndex(int readerIndex);

    ChannelBuffer readSlice(int length);

    void setByte(int index, int value);

    void setBytes(int index, byte[] src);

    void setBytes(int index, byte[] src, int srcIndex, int length);

    void setBytes(int index, ByteBuffer src);

    void setBytes(int index, ChannelBuffer src);

    void setIndex(int readerIndex, int writerIndex);

    void skipBytes(int length);

    ByteBuffer toByteBuffer();

    ByteBuffer toByteBuffer(int index, int length);

    boolean isWritable();

    int writableBytes();

    void writeByte(int value);

    void writeBytes(byte[] src);

    void writeBytes(byte[] src, int index, int length);

    void writeBytes(ChannelBuffer src);

    int writeBytes(InputStream in, int length) throws IOException;

    default int writeString(final String value) {
        return writeString(value, StandardCharsets.UTF_8, false, false);
    }

    default int writeString(final String value, final Charset charset) {
        return writeString(value, charset, false, false);
    }

    default int writeString(final String value, final Charset charset, final boolean zeroNull, final boolean shortLength) {
        byte[] bytes = value == null ? null : value.getBytes(charset == null ? StandardCharsets.UTF_8 : charset);
        int length = bytes == null ? (zeroNull ? 0 : -1) : bytes.length;
        int result = (shortLength ? 2 : 4) + (length > 0 ? length : 0);
        //申请容量
        ensureWritable(result);
        //保存位置
        int pos = writerIndex();
        //写入长度
        if (shortLength) {
            setShort(pos, length);
            pos += 2;
        } else {
            setInt(pos, length);
            pos += 4;
        }
        //写入数据
        if (length > 0) {
            setBytes(pos, bytes);
            pos += bytes.length;
        }
        //定位写入位置
        writerIndex(pos);
        return result;
    }

    default String readString() {
        return readString(StandardCharsets.UTF_8, false);
    }

    default String readString(final Charset charset) {
        return readString(charset, false);
    }

    default String readString(final Charset charset, final boolean shortLength) {
        int length = shortLength ? readShort() : readInt();
        if (length > 0) {
            byte[] bytes = new byte[length];
            readBytes(bytes);
            return new String(bytes, charset == null ? StandardCharsets.UTF_8 : charset);
        } else if (length == 0) {
            return "";
        } else {
            return null;
        }
    }

    int writerIndex();

    void writerIndex(int writerIndex);

    byte[] array();

    boolean hasArray();

    int arrayOffset();

    long readLong();

    int readInt();

    short readShort();

    float readFloat();

    double readDouble();

    boolean readBoolean();

    void writeLong(long value);

    void writeInt(int value);

    void writeShort(int value);

    void writeDouble(double value);

    void writeFloat(float value);

    void writeBoolean(boolean value);

    void setInt(int index, int value);

    void setBoolean(int index, boolean value);

    void setShort(int index, int value);

    void setLong(int index, long value);

    void setFloat(int index, float value);

    void setDouble(int index, double value);

    boolean release();

    boolean isReleased();

    InputStream inputStream();

    InputStream inputStream(int length);

    OutputStream outputStream();
}
