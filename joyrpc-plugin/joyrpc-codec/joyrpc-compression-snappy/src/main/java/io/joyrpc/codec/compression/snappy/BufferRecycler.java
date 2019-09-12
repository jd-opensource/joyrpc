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

import java.lang.ref.SoftReference;

/**
 * Simple helper class to encapsulate details of basic buffer
 * recycling scheme, which helps a lot (as per profiling) for
 * smaller encoding cases.
 *
 * @author tatu
 */
class BufferRecycler {
    private static final int MIN_ENCODING_BUFFER = 4000;
    private static final int MIN_OUTPUT_BUFFER = 8000;

    /**
     * This <code>ThreadLocal</code> contains a {@link SoftReference}
     * to a {@link BufferRecycler} used to provide a low-cost
     * buffer recycling for buffers we need for encoding, decoding.
     */
    protected static final ThreadLocal<SoftReference<BufferRecycler>> recyclerRef =
            new ThreadLocal<SoftReference<BufferRecycler>>();
    //输入缓冲区
    private byte[] inputBuffer;
    //输出缓冲区
    private byte[] outputBuffer;
    //解码缓冲区
    private byte[] decodingBuffer;
    //编码缓冲区
    private byte[] encodingBuffer;
    //编码散列
    private short[] encodingHash;

    /**
     * Accessor to get thread-local recycler instance
     */
    public static BufferRecycler instance() {
        SoftReference<BufferRecycler> ref = recyclerRef.get();

        BufferRecycler bufferRecycler;
        if (ref == null) {
            bufferRecycler = null;
        } else {
            bufferRecycler = ref.get();
        }

        if (bufferRecycler == null) {
            bufferRecycler = new BufferRecycler();
            recyclerRef.set(new SoftReference<BufferRecycler>(bufferRecycler));
        }
        return bufferRecycler;
    }

    /**
     * 清理
     */
    public void clear() {
        inputBuffer = null;
        outputBuffer = null;
        decodingBuffer = null;
        encodingBuffer = null;
        encodingHash = null;
    }

    /**
     * 获取编码缓冲器
     *
     * @param minSize 最小长度
     * @return 编码缓冲器
     */
    public byte[] allocEncodingBuffer(final int minSize) {
        byte[] buf = encodingBuffer;
        if (buf == null || buf.length < minSize) {
            buf = new byte[Math.max(minSize, MIN_ENCODING_BUFFER)];
        } else {
            encodingBuffer = null;
        }
        return buf;
    }

    /**
     * 释放编码缓冲区
     *
     * @param buffer 编码缓冲区
     */
    public void releaseEncodeBuffer(final byte[] buffer) {
        if (encodingBuffer == null || buffer.length > encodingBuffer.length) {
            encodingBuffer = buffer;
        }
    }

    /**
     * 获取输出缓冲器
     *
     * @param minSize 最小长度
     * @return 输出缓冲器
     */
    public byte[] allocOutputBuffer(final int minSize) {
        byte[] buf = outputBuffer;
        if (buf == null || buf.length < minSize) {
            buf = new byte[Math.max(minSize, MIN_OUTPUT_BUFFER)];
        } else {
            outputBuffer = null;
        }
        return buf;
    }

    /**
     * 释放输出缓冲区
     *
     * @param buffer 输出缓冲区
     */
    public void releaseOutputBuffer(final byte[] buffer) {
        if (outputBuffer == null || (buffer != null && buffer.length > outputBuffer.length)) {
            outputBuffer = buffer;
        }
    }

    /**
     * 获取编码散列缓冲区
     *
     * @param suggestedSize 建议大小
     * @return 编码散列缓冲区
     */
    public short[] allocEncodingHash(final int suggestedSize) {
        short[] buf = encodingHash;
        if (buf == null || buf.length < suggestedSize) {
            buf = new short[suggestedSize];
        } else {
            encodingHash = null;
        }
        return buf;
    }

    /**
     * 释放编码散列缓冲区
     *
     * @param buffer 编码散列缓冲区
     */
    public void releaseEncodingHash(final short[] buffer) {
        if (encodingHash == null || (buffer != null && buffer.length > encodingHash.length)) {
            encodingHash = buffer;
        }
    }

    /**
     * 获取输入缓冲器
     *
     * @param minSize 最小长度
     * @return 输入缓冲器
     */
    public byte[] allocInputBuffer(final int minSize) {
        byte[] buf = inputBuffer;
        if (buf == null || buf.length < minSize) {
            buf = new byte[Math.max(minSize, MIN_OUTPUT_BUFFER)];
        } else {
            inputBuffer = null;
        }
        return buf;
    }

    /**
     * 释放输入缓冲区
     *
     * @param buffer 输入缓冲区
     */
    public void releaseInputBuffer(final byte[] buffer) {
        if (inputBuffer == null || (buffer != null && buffer.length > inputBuffer.length)) {
            inputBuffer = buffer;
        }
    }

    /**
     * 获取解码缓冲器
     *
     * @param size 长度
     * @return 解码缓冲器
     */
    public byte[] allocDecodeBuffer(final int size) {
        byte[] buf = decodingBuffer;
        if (buf == null || buf.length < size) {
            buf = new byte[size];
        } else {
            decodingBuffer = null;
        }
        return buf;
    }

    /**
     * 释放解码缓冲区
     *
     * @param buffer 解码缓冲区
     */
    public void releaseDecodeBuffer(final byte[] buffer) {
        if (decodingBuffer == null || (buffer != null && buffer.length > decodingBuffer.length)) {
            decodingBuffer = buffer;
        }
    }
}
