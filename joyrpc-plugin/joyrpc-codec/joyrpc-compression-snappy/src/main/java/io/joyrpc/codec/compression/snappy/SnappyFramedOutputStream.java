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

import io.joyrpc.codec.compression.Finishable;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Implements the <a href="http://snappy.googlecode.com/svn/trunk/framing_format.txt" >x-snappy-framed</a> as an
 * {@link OutputStream}.
 */
public final class SnappyFramedOutputStream extends OutputStream implements Finishable {
    /**
     * We place an additional restriction that the uncompressed data in
     * a chunk must be no longer than 65536 bytes. This allows consumers to
     * easily use small fixed-size buffers.
     */
    public static final int MAX_BLOCK_SIZE = 65536;

    public static final int DEFAULT_BLOCK_SIZE = MAX_BLOCK_SIZE;

    public static final double DEFAULT_MIN_COMPRESSION_RATIO = 0.85d;

    protected final BufferRecycler recycler;
    protected final int blockSize;
    protected final byte[] buffer;
    protected final byte[] outputBuffer;
    protected final double minCompressionRatio;

    protected final OutputStream out;

    protected int position;
    protected boolean closed;
    protected boolean finished;

    public SnappyFramedOutputStream(OutputStream out) throws IOException {
        this(out, DEFAULT_BLOCK_SIZE, DEFAULT_MIN_COMPRESSION_RATIO);
    }

    public SnappyFramedOutputStream(OutputStream out, int blockSize, double minCompressionRatio) throws IOException {
        if (out == null) {
            throw new IllegalArgumentException("output is null");
        } else if (blockSize <= 0 || blockSize > MAX_BLOCK_SIZE) {
            throw new IllegalArgumentException("blockSize must be in (0, 65536]");
        } else if (minCompressionRatio <= 0 || minCompressionRatio > 1.0) {
            throw new IllegalArgumentException("minCompressionRatio must be between (0,1.0]");
        }
        this.out = out;
        this.minCompressionRatio = minCompressionRatio;
        this.recycler = BufferRecycler.instance();
        this.blockSize = blockSize;
        this.buffer = recycler.allocOutputBuffer(blockSize);
        this.outputBuffer = recycler.allocEncodingBuffer(SnappyCompressor.maxCompressedLength(blockSize));
        writeHeader(out);
    }

    /**
     * Writes the implementation specific header or "marker bytes" to
     * <i>out</i>.
     *
     * @param out The underlying {@link OutputStream}.
     */
    protected void writeHeader(final OutputStream out) throws IOException {
        out.write(SnappyFramed.HEADER_BYTES);
    }

    /**
     * Write a frame (block) to <i>out</i>.
     * <p>
     * Each chunk consists first a single byte of chunk identifier, then a
     * three-byte little-endian length of the chunk in bytes (from 0 to
     * 16777215, inclusive), and then the data if any. The four bytes of chunk
     * header is not counted in the data length.
     *
     * @param out        The {@link OutputStream} to write to.
     * @param data       The data to write.
     * @param offset     The offset in <i>data</i> to start at.
     * @param length     The length of <i>data</i> to use.
     * @param compressed Indicates if <i>data</i> is the compressed or raw content.
     *                   This is based on whether the compression ratio desired is
     *                   reached.
     * @param crc32c     The calculated checksum.
     */
    protected void writeBlock(final OutputStream out, final byte[] data, final int offset, final int length,
                              final boolean compressed, final int crc32c) throws IOException {
        out.write(compressed ? SnappyFramed.COMPRESSED_DATA_FLAG : SnappyFramed.UNCOMPRESSED_DATA_FLAG);

        // the length written out to the header is both the checksum and the
        // frame
        int headerLength = length + 4;

        // write length
        out.write(headerLength);
        out.write(headerLength >>> 8);
        out.write(headerLength >>> 16);

        // write crc32c of user input data
        out.write(crc32c);
        out.write(crc32c >>> 8);
        out.write(crc32c >>> 16);
        out.write(crc32c >>> 24);

        // write data
        out.write(data, offset, length);
    }

    @Override
    public void write(final int b) throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }
        if (position >= blockSize) {
            flushBuffer();
        }
        buffer[position++] = (byte) b;
    }

    @Override
    public void write(final byte[] input, int offset, int length) throws IOException {
        if (input == null) {
            throw new NullPointerException("input is null");
        }
        Preconditions.checkPositionIndexes(offset, offset + length, input.length);
        if (closed) {
            throw new IOException("Stream is closed");
        }

        int free = blockSize - position;

        // easy case: enough free space in buffer for entire input
        if (free >= length) {
            copyToBuffer(input, offset, length);
            return;
        }

        // fill partial buffer as much as possible and flush
        if (position > 0) {
            copyToBuffer(input, offset, free);
            flushBuffer();
            offset += free;
            length -= free;
        }

        // write remaining full blocks directly from input array
        while (length >= blockSize) {
            writeCompressed(input, offset, blockSize);
            offset += blockSize;
            length -= blockSize;
        }

        // copy remaining partial block into now-empty buffer
        copyToBuffer(input, offset, length);
    }

    @Override
    public void finish() throws IOException {
        if (!finished) {
            finished = true;
            flushBuffer();
        }
    }

    @Override
    public final void flush() throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }
        out.flush();
    }

    @Override
    public final void close() throws IOException {
        if (closed) {
            return;
        }
        try {
            finish();
            flush();
            out.close();
        } finally {
            closed = true;
            recycler.releaseOutputBuffer(outputBuffer);
            recycler.releaseEncodeBuffer(buffer);
        }
    }

    protected void copyToBuffer(final byte[] input, final int offset, final int length) {
        System.arraycopy(input, offset, buffer, position, length);
        position += length;
    }

    /**
     * Compresses and writes out any buffered data. This does nothing if there
     * is no currently buffered data.
     */
    protected void flushBuffer() throws IOException {
        if (position > 0) {
            writeCompressed(buffer, 0, position);
            position = 0;
        }
    }

    /**
     * {@link #calculateCRC32C(byte[], int, int) Calculates} the crc, compresses
     * the data, determines if the compression ratio is acceptable and calls
     * {@link #writeBlock(OutputStream, byte[], int, int, boolean, int)} to
     * actually write the frame.
     *
     * @param input  The byte[] containing the raw data to be compressed.
     * @param offset The offset into <i>input</i> where the data starts.
     * @param length The amount of data in <i>input</i>.
     */
    protected void writeCompressed(final byte[] input, final int offset, final int length) throws IOException {
        // crc is based on the user supplied input data
        int crc32c = calculateCRC32C(input, offset, length);

        int compressed = SnappyCompressor.compress(input, offset, length, outputBuffer, 0);

        // only use the compressed data if compression ratio is <= the minCompressionRatio
        if (((double) compressed / (double) length) <= minCompressionRatio) {
            writeBlock(out, outputBuffer, 0, compressed, true, crc32c);
        } else {
            // otherwise use the uncompressed data.
            writeBlock(out, input, offset, length, false, crc32c);
        }
    }

    /**
     * Calculates a CRC32C checksum over the data.
     * <p>
     * This can be overridden to provider alternative implementations (such as
     * returning 0 if checksums are not desired).
     * </p>
     *
     * @return The CRC32 checksum.
     */
    protected int calculateCRC32C(final byte[] data, final int offset, final int length) {
        return 0;
    }
}
