package io.joyrpc.transport.netty4.buffer;

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
import io.joyrpc.transport.buffer.ChannelBufferInputStream;
import io.joyrpc.transport.buffer.ChannelBufferOutputStream;
import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * @date: 2019/1/15
 */
public class NettyChannelBuffer implements ChannelBuffer {

    protected final ByteBuf byteBuf;

    protected boolean released = false;

    public NettyChannelBuffer(ByteBuf byteBuf) {
        this.byteBuf = byteBuf;
    }

    public ByteBuf getByteBuf() {
        return byteBuf;
    }

    @Override
    public int capacity() {
        return byteBuf.capacity();
    }

    @Override
    public ChannelBuffer capacity(int newCapacity) {
        return new NettyChannelBuffer(byteBuf.capacity(newCapacity));
    }

    @Override
    public void ensureWritable(final int minWritableBytes) {
        byteBuf.ensureWritable(minWritableBytes);
    }

    @Override
    public void clear() {
        byteBuf.clear();
    }

    @Override
    public ChannelBuffer copy() {
        return new NettyChannelBuffer(byteBuf.copy());
    }

    @Override
    public ChannelBuffer copy(int index, int length) {
        return new NettyChannelBuffer(byteBuf.copy(index, length));
    }

    @Override
    public void discardReadBytes() {
        byteBuf.discardReadBytes();
    }

    @Override
    public short getUnsignedByte(final int index) {
        return byteBuf.getUnsignedByte(index);
    }

    @Override
    public int getUnsignedShort(final int var1) {
        return byteBuf.getUnsignedShort(var1);
    }

    @Override
    public byte getByte(final int index) {
        return byteBuf.getByte(index);
    }

    @Override
    public short getShort(final int index) {
        return byteBuf.getShort(index);
    }

    @Override
    public int getInt(final int index) {
        return byteBuf.getInt(index);
    }

    @Override
    public long getLong(final int index) {
        return byteBuf.getLong(index);
    }

    @Override
    public ChannelBuffer getBytes(final int index, final byte[] dst) {
        return new NettyChannelBuffer(byteBuf.getBytes(index, dst));
    }

    @Override
    public ChannelBuffer getBytes(final int index, final byte[] dst, final int dstIndex, final int length) {
        return new NettyChannelBuffer(byteBuf.getBytes(index, dst, dstIndex, length));
    }

    @Override
    public ChannelBuffer getBytes(final int index, final ByteBuffer dst) {
        return new NettyChannelBuffer(byteBuf.getBytes(index, dst));
    }

    @Override
    public ChannelBuffer getBytes(final int index, final ChannelBuffer dst) {
        return new NettyChannelBuffer(byteBuf.getBytes(index, dst.toByteBuffer()));
    }

    @Override
    public boolean isReadable() {
        return byteBuf.isReadable();
    }

    @Override
    public int readableBytes() {
        return byteBuf.readableBytes();
    }

    @Override
    public byte readByte() {
        return byteBuf.readByte();
    }

    @Override
    public void readBytes(final byte[] dst) {
        byteBuf.readBytes(dst);
    }

    @Override
    public void readBytes(final byte[] dst, final int dstIndex, final int length) {
        byteBuf.readBytes(dst, dstIndex, length);
    }

    @Override
    public void readBytes(final ChannelBuffer dst) {
        byteBuf.readBytes(dst.toByteBuffer());
    }

    @Override
    public ChannelBuffer readBytes(final OutputStream out, final int length) throws IOException {
        return new NettyChannelBuffer(byteBuf.readBytes(out, length));
    }

    @Override
    public void resetReaderIndex() {
        byteBuf.resetReaderIndex();
    }

    @Override
    public void resetWriterIndex() {
        byteBuf.resetWriterIndex();
    }

    @Override
    public int readerIndex() {
        return byteBuf.readerIndex();
    }

    @Override
    public void readerIndex(final int readerIndex) {
        byteBuf.readerIndex(readerIndex);
    }

    @Override
    public ChannelBuffer readSlice(final int length) {
        return new NettyChannelBuffer(byteBuf.readSlice(length));
    }

    @Override
    public void setInt(final int index, final int value) {
        byteBuf.setInt(index, value);
    }

    @Override
    public void setByte(final int index, final int value) {
        byteBuf.setByte(index, value);
    }

    @Override
    public void setBytes(final int index, final byte[] src) {
        byteBuf.setBytes(index, src);
    }

    @Override
    public void setBytes(final int index, final byte[] src, final int srcIndex, final int length) {
        byteBuf.setBytes(index, src, srcIndex, length);
    }

    @Override
    public void setBytes(final int index, final ByteBuffer src) {
        byteBuf.setBytes(index, src);
    }

    @Override
    public void setBytes(final int index, final ChannelBuffer src) {
        byteBuf.setBytes(index, src.toByteBuffer());
    }

    @Override
    public void setIndex(final int readerIndex, final int writerIndex) {
        byteBuf.setIndex(readerIndex, writerIndex);
    }

    @Override
    public void skipBytes(final int length) {
        byteBuf.skipBytes(length);
    }

    @Override
    public ByteBuffer toByteBuffer() {
        return byteBuf.nioBuffer();
    }

    @Override
    public ByteBuffer toByteBuffer(final int index, final int length) {
        return byteBuf.nioBuffer(index, length);
    }

    @Override
    public boolean isWritable() {
        return byteBuf.isWritable();
    }

    @Override
    public int writableBytes() {
        return byteBuf.writableBytes();
    }

    @Override
    public void writeByte(final int value) {
        byteBuf.writeByte(value);
    }

    @Override
    public void writeBytes(final byte[] src) {
        byteBuf.writeBytes(src);
    }

    @Override
    public void writeBytes(final byte[] src, final int index, final int length) {
        byteBuf.writeBytes(src, index, length);
    }

    @Override
    public void writeBytes(final ChannelBuffer src) {
        byteBuf.writeBytes(src.toByteBuffer());
    }

    @Override
    public int writeBytes(final InputStream in, final int length) throws IOException {
        return byteBuf.writeBytes(in, length);
    }

    @Override
    public int writerIndex() {
        return byteBuf.writerIndex();
    }

    @Override
    public void writerIndex(final int writerIndex) {
        byteBuf.writerIndex(writerIndex);
    }

    @Override
    public byte[] array() {
        return byteBuf.array();
    }

    @Override
    public boolean hasArray() {
        return byteBuf.hasArray();
    }

    @Override
    public int arrayOffset() {
        return byteBuf.arrayOffset();
    }

    @Override
    public int readInt() {
        return byteBuf.readInt();
    }

    @Override
    public short readShort() {
        return byteBuf.readShort();
    }

    @Override
    public void writeInt(final int value) {
        byteBuf.writeInt(value);
    }

    @Override
    public void writeShort(final int value) {
        byteBuf.writeShort(value);
    }

    @Override
    public long readLong() {
        return byteBuf.readLong();
    }

    @Override
    public float readFloat() {
        return byteBuf.readFloat();
    }

    @Override
    public double readDouble() {
        return byteBuf.readDouble();
    }

    @Override
    public boolean readBoolean() {
        return byteBuf.readBoolean();
    }

    @Override
    public void writeLong(final long value) {
        byteBuf.writeLong(value);
    }

    @Override
    public void writeDouble(final double value) {
        byteBuf.writeDouble(value);
    }

    @Override
    public void writeFloat(final float value) {
        byteBuf.writeFloat(value);
    }

    @Override
    public void writeBoolean(final boolean value) {
        byteBuf.writeBoolean(value);
    }

    @Override
    public void setBoolean(final int index, final boolean value) {
        byteBuf.setBoolean(index, value);
    }

    @Override
    public void setShort(final int index, final int value) {
        byteBuf.setShort(index, value);
    }

    @Override
    public void setLong(final int index, final long value) {
        byteBuf.setLong(index, value);
    }

    @Override
    public void setFloat(final int index, final float value) {
        byteBuf.setFloat(index, value);
    }

    @Override
    public void setDouble(final int index, final double value) {
        byteBuf.setDouble(index, value);
    }

    @Override
    public boolean release() {
        return released = byteBuf.release();
    }

    @Override
    public boolean isReleased() {
        return released;
    }

    @Override
    public InputStream inputStream() {
        return new ChannelBufferInputStream(this);
    }

    @Override
    public InputStream inputStream(final int length) {
        return new ChannelBufferInputStream(this, length);
    }

    @Override
    public OutputStream outputStream() {
        return new ChannelBufferOutputStream(this);
    }
}
