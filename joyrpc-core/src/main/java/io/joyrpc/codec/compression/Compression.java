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

import io.joyrpc.codec.CodecType;
import io.joyrpc.codec.UnsafeByteArrayInputStream;
import io.joyrpc.codec.UnsafeByteArrayOutputStream;
import io.joyrpc.extension.Extensible;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 数据压缩算法
 */
@Extensible("compression")
public interface Compression extends CodecType {

    byte NONE = 0;
    /**
     * snappy压缩
     */
    byte SNAPPY = 2;
    /**
     * lz4压缩
     */
    byte LZ4 = 3;
    /**
     * zlib压缩
     */
    byte ZLIB = 4;

    @Deprecated
    byte LZMA = 5;
    /**
     * gzip压缩
     */
    byte GZIP = 6;
    /**
     * deflate压缩（即zlib压缩）
     */
    byte DEFLATE = 7;
    /**
     * snappy frame压缩
     */
    byte SNAPPY_FRAME = SNAPPY + 20;
    /**
     * lz4 frame压缩
     */
    byte LZ4_FRAME = LZ4 + 20;

    int SNAPPY_ORDER = 100;
    int SNAPPY_FRAME_ORDER = SNAPPY_ORDER + 100;
    int LZ4_ORDER = SNAPPY_FRAME_ORDER + 100;
    int LZ4_FRAME_ORDER = LZ4_ORDER + 1;
    int ZLIB_ORDER = LZ4_ORDER + 100;
    int LZMA_ORDER = ZLIB_ORDER + 100;
    int DEFLATE_ORDER = ZLIB_ORDER + 200;

    /**
     * 构造压缩流
     *
     * @param out 输出
     * @return 压缩流
     * @throws IOException 异常
     */
    OutputStream compress(OutputStream out) throws IOException;

    /**
     * 构建解压流
     *
     * @param input 输入
     * @return 解压流
     * @throws IOException 异常
     */
    InputStream decompress(InputStream input) throws IOException;

    /**
     * 压缩
     *
     * @param content 待压缩内容
     * @return 字节数组
     * @throws IOException 异常
     */
    default byte[] compress(final byte[] content) throws IOException {
        return compress(null, content, 0, content.length);
    }

    /**
     * 压缩
     *
     * @param content 待压缩内容
     * @param offset  待压缩内容偏移量
     * @param length  待压缩内容长度
     * @return 字节数组
     * @throws IOException 异常
     */
    default byte[] compress(final byte[] content, final int offset, final int length) throws IOException {
        return compress(null, content, offset, length);
    }

    /**
     * 压缩
     *
     * @param baos    压缩输出流
     * @param content 待压缩内容
     * @param offset  待压缩内容偏移量
     * @param length  待压缩内容长度
     * @return 字节数组
     * @throws IOException 异常
     */
    default byte[] compress(final UnsafeByteArrayOutputStream baos, final byte[] content, final int offset,
                            final int length) throws IOException {
        UnsafeByteArrayOutputStream buffer = baos == null ? new UnsafeByteArrayOutputStream() : baos;
        OutputStream os = null;
        try {
            os = compress(buffer);
            os.write(content, offset, length);
            //先写完数据
            if (os instanceof Finishable) {
                ((Finishable) os).finish();
            }
            //再提交数据
            os.flush();
            return buffer.toByteArray();
        } finally {
            try {
                os.close();
            } catch (IOException ignored) {
            }
            try {
                buffer.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * 解压缩
     *
     * @param content 待解压内容
     * @return 字节数组
     * @throws IOException 异常
     */
    default byte[] decompress(final byte[] content) throws IOException {
        UnsafeByteArrayOutputStream baos = new UnsafeByteArrayOutputStream();
        UnsafeByteArrayInputStream bais = new UnsafeByteArrayInputStream(content);
        InputStream is = null;
        try {
            is = decompress(bais);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            baos.flush();
            return baos.toByteArray();
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {
            }
            try {
                baos.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * 获取压缩类型插件名称
     *
     * @return 压缩类型名称
     */
    String getTypeName();

}
