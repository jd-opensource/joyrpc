/*
 * Copyright (C) 2011 the original author or authors.
 * See the notice.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
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
 */
package io.joyrpc.codec.compression.snappy;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static java.lang.Math.min;

/**
 * Implements the <a href="http://snappy.googlecode.com/svn/trunk/framing_format.txt" >x-snappy-framed</a> as an
 * {@link InputStream}.
 */
public class SnappyFramedInputStream extends InputStream {
    private final InputStream in;
    private final byte[] frameHeader;
    private final BufferRecycler recycler;

    /**
     * A single frame read from the underlying {@link InputStream}.
     */
    protected byte[] input;

    /**
     * The decompressed data from {@link #input}.
     */
    protected byte[] uncompressed;

    /**
     * Indicates if this instance has been closed.
     */
    protected boolean closed;

    /**
     * Indicates if we have reached the EOF on {@link #in}.
     */
    protected boolean eof;

    /**
     * The position in {@link #input} to read to.
     */
    protected int valid;

    /**
     * The next position to read from {@link #buffer}.
     */
    protected int position;

    /**
     * Buffer is a reference to the real buffer of uncompressed data for the
     * current block: uncompressed if the block is compressed, or input if it is
     * not.
     */
    protected byte[] buffer;

    public SnappyFramedInputStream(InputStream in) throws IOException {
        this(in, SnappyFramedOutputStream.MAX_BLOCK_SIZE, 4, SnappyFramed.HEADER_BYTES);
    }

    /**
     * Creates a Snappy input stream to read data from the specified underlying
     * input stream.
     *
     * @param in              the underlying input stream
     * @param maxBlockSize    max block size
     * @param frameHeaderSize frame header size
     * @param expectedHeader  the expected stream header
     */
    public SnappyFramedInputStream(InputStream in, int maxBlockSize, int frameHeaderSize,
                                   byte[] expectedHeader) throws IOException {
        this.in = in;
        this.recycler = BufferRecycler.instance();
        this.input = recycler.allocInputBuffer(maxBlockSize + 5);
        this.uncompressed = recycler.allocDecodeBuffer(maxBlockSize + 5);
        this.frameHeader = new byte[frameHeaderSize];

        // stream must begin with stream header
        byte[] actualHeader = new byte[expectedHeader.length];

        int read = readBytes(in, actualHeader, 0, actualHeader.length);
        if (read < expectedHeader.length) {
            throw new EOFException("encountered EOF while reading stream header");
        }
        if (!Arrays.equals(expectedHeader, actualHeader)) {
            throw new IOException("invalid stream header");
        }
    }

    /**
     * Reads <i>length</i> bytes from <i>source</i> into <i>dest</i> starting at <i>offset</i>. <br/>
     * <p/>
     * The only case where the <i>length</i> <tt>byte</tt>s will not be read is if <i>source</i> returns EOF.
     *
     * @param source The source of bytes to read from. Must not be <code>null</code>.
     * @param dest   The <tt>byte[]</tt> to read bytes into. Must not be <code>null</code>.
     * @param offset The index in <i>dest</i> to start filling.
     * @param length The number of bytes to read.
     * @return Total number of bytes actually read.
     * @throws IndexOutOfBoundsException if <i>offset</i> or <i>length</i> are invalid.
     */
    protected static int readBytes(final InputStream source, final byte[] dest, final int offset, final int length) throws
            IOException {
        if (source == null || dest == null) {
            return 0;
        }

        // how many bytes were read.
        int lastRead = source.read(dest, offset, length);

        int totalRead = lastRead;

        // if we did not read as many bytes as we had hoped, try reading again.
        if (lastRead < length) {
            // as long the buffer is not full (remaining() == 0) and we have not reached EOF (lastRead == -1) keep
            // reading.
            while (totalRead < length && lastRead != -1) {
                lastRead = source.read(dest, offset + totalRead, length - totalRead);

                // if we got EOF, do not add to total read.
                if (lastRead != -1) {
                    totalRead += lastRead;
                }
            }
        }

        return totalRead;
    }

    /**
     * 跳过指定字节数
     *
     * @param source 输入缓冲器
     * @param skip   字节数
     * @return 实际跳过的字节数
     * @throws IOException
     */
    protected static int skip(final InputStream source, final int skip) throws IOException {
        // optimization also avoids potential for error with some implementation of
        // InputStream.skip() which throw exceptions with negative numbers (ie. ZipInputStream).
        if (source == null || skip <= 0) {
            return 0;
        }

        int toSkip = skip - (int) source.skip(skip);

        boolean more = true;
        while (toSkip > 0 && more) {
            // check to see if we reached EOF
            int read = source.read();
            if (read == -1) {
                more = false;
            } else {
                --toSkip;
                toSkip -= source.skip(toSkip);
            }
        }

        int skipped = skip - toSkip;

        return skipped;
    }

    @Override
    public int read() throws IOException {
        if (closed) {
            return -1;
        }
        if (!ensureBuffer()) {
            return -1;
        }
        return buffer[position++] & 0xFF;
    }

    @Override
    public int read(final byte[] output, final int offset, final int length) throws IOException {
        if (output == null) {
            throw new NullPointerException("output is null");
        }
        Preconditions.checkPositionIndexes(offset, offset + length, output.length);
        if (closed) {
            return -1;
        }
        if (length == 0) {
            return 0;
        }
        if (!ensureBuffer()) {
            return -1;
        }

        int size = min(length, available());
        System.arraycopy(buffer, position, output, offset, size);
        position += size;
        return size;
    }

    @Override
    public int available() throws IOException {
        if (closed) {
            return 0;
        }
        return valid - position;
    }

    @Override
    public void close() throws IOException {
        try {
            in.close();
        } finally {
            if (!closed) {
                closed = true;
                recycler.releaseInputBuffer(input);
                recycler.releaseDecodeBuffer(uncompressed);
            }
        }
    }

    private boolean ensureBuffer() throws IOException {
        if (available() > 0) {
            return true;
        }
        if (eof) {
            return false;
        }

        if (!readBlockHeader()) {
            eof = true;
            return false;
        }

        // get action based on header
        FrameMetaData frameMetaData = getFrameMetaData(frameHeader);

        if (FrameAction.SKIP == frameMetaData.frameAction) {
            skip(in, frameMetaData.length);
            return ensureBuffer();
        }

        if (frameMetaData.length > input.length) {
            this.input = recycler.allocInputBuffer(frameMetaData.length);
            this.uncompressed = recycler.allocDecodeBuffer(frameMetaData.length);
        }

        int actualRead = readBytes(in, input, 0, frameMetaData.length);
        if (actualRead != frameMetaData.length) {
            throw new EOFException("unexpectd EOF when reading frame");
        }

        FrameData frameData = getFrameData(frameHeader, input, actualRead);

        if (FrameAction.UNCOMPRESS == frameMetaData.frameAction) {
            int uncompressedLength = SnappyDecompressor.getUncompressedLength(input, frameData.offset);

            if (uncompressedLength > uncompressed.length) {
                uncompressed = recycler.allocDecodeBuffer(uncompressedLength);
            }

            this.valid = SnappyDecompressor
                    .uncompress(input, frameData.offset, actualRead - frameData.offset, uncompressed, 0);
            this.buffer = uncompressed;
            this.position = 0;
        } else {
            // we need to start reading at the offset
            this.position = frameData.offset;
            this.buffer = input;
            // valid is until the end of the read data, regardless of offset
            // indicating where we start
            this.valid = actualRead;
        }

        return true;
    }

    protected boolean readBlockHeader() throws IOException {
        int read = readBytes(in, frameHeader, 0, frameHeader.length);

        if (read == -1) {
            return false;
        }

        if (read < frameHeader.length) {
            throw new EOFException("encountered EOF while reading block header");
        }

        return true;
    }

    /**
     * Use the content of the frameHeader to describe what type of frame we have
     * and the action to take.
     */
    protected FrameMetaData getFrameMetaData(final byte[] frameHeader) throws IOException {
        int length = (frameHeader[1] & 0xFF);
        length |= (frameHeader[2] & 0xFF) << 8;
        length |= (frameHeader[3] & 0xFF) << 16;

        int minLength;
        FrameAction frameAction;
        int flag = frameHeader[0] & 0xFF;
        switch (flag) {
            case SnappyFramed.COMPRESSED_DATA_FLAG:
                frameAction = FrameAction.UNCOMPRESS;
                minLength = 5;
                break;
            case SnappyFramed.UNCOMPRESSED_DATA_FLAG:
                frameAction = FrameAction.RAW;
                minLength = 5;
                break;
            case SnappyFramed.STREAM_IDENTIFIER_FLAG:
                if (length != 6) {
                    throw new IOException("stream identifier chunk with invalid length: " + length);
                }
                frameAction = FrameAction.SKIP;
                minLength = 6;
                break;
            default:
                // Reserved unskippable chunks (chunk types 0x02-0x7f)
                if (flag <= 0x7f) {
                    throw new IOException("unsupported unskippable chunk: " + Integer.toHexString(flag));
                }

                // all that is left is Reserved skippable chunks (chunk types 0x80-0xfe)
                frameAction = FrameAction.SKIP;
                minLength = 0;
        }

        if (length < minLength) {
            throw new IOException("invalid length: " + length + " for chunk flag: " + Integer.toHexString(flag));
        }

        return new FrameMetaData(frameAction, length);
    }

    /**
     * Take the frame header and the content of the frame to describe metadata
     * about the content.
     *
     * @param frameHeader The frame header.
     * @param content     The content of the of the frame. Content begins at index {@code 0}.
     * @param length      The length of the content.
     * @return Metadata about the content of the frame.
     */
    protected FrameData getFrameData(final byte[] frameHeader, final byte[] content, final int length) {
        // crc is contained in the frame content
        int crc32c =
                (content[3] & 0xFF) << 24 | (content[2] & 0xFF) << 16 | (content[1] & 0xFF) << 8 | (content[0] & 0xFF);

        return new FrameData(crc32c, 4);
    }

    enum FrameAction {
        RAW,
        SKIP,
        UNCOMPRESS
    }

    /**
     * 片段元数据
     */
    protected static final class FrameMetaData {
        // 长度
        final int length;
        // 操作
        final FrameAction frameAction;

        /**
         * @param frameAction
         * @param length
         */
        public FrameMetaData(FrameAction frameAction, int length) {
            this.frameAction = frameAction;
            this.length = length;
        }
    }

    /**
     * 片段数据
     */
    protected static final class FrameData {
        // 校验和
        final int checkSum;
        // 偏移量
        final int offset;

        public FrameData(int checkSum, int offset) {
            this.checkSum = checkSum;
            this.offset = offset;
        }
    }
}
