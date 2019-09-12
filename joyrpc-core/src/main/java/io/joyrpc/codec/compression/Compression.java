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
     * @param out
     * @return
     * @throws IOException
     */
    OutputStream compress(OutputStream out) throws IOException;

    /**
     * 构建解压流
     *
     * @param input
     * @return
     * @throws IOException
     */
    InputStream decompress(InputStream input) throws IOException;

    /**
     * 获取压缩类型插件名称
     *
     * @return
     */
    String getTypeName();

}
