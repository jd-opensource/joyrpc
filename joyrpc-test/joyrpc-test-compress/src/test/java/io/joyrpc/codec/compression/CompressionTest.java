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

import io.joyrpc.codec.UnsafeByteArrayOutputStream;
import io.joyrpc.transport.netty4.buffer.NettyChannelBuffer;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.io.*;
import java.util.List;

import static io.joyrpc.Plugin.COMPRESSION;

public class CompressionTest {

    @Test
    public void testCompression() throws IOException {
        List<String> types = COMPRESSION.names();
        byte[] source = new byte[10];
        source[1] = 1;
        byte[] target = new byte[10];

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (String type : types) {
            try {
                bos.reset();
                Compression compression = COMPRESSION.get(type);
                OutputStream os = compression.compress(bos);
                os.write(source);
                if (os instanceof Finishable) {
                    ((Finishable) os).finish();
                }
                os.flush();
                byte[] bytes = bos.toByteArray();
                ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                InputStream is = compression.decompress(bis);
                is.read(target);
                Assertions.assertArrayEquals(source, target);
            } catch (Throwable e) {
                System.out.println("compress error " + type);
                throw e;
            }
        }

    }

    @Test
    public void testTps() throws IOException {

        List<String> types = COMPRESSION.names();
        //LZMA太慢了，去掉性能测试
        types.remove("lzma");

        byte[] source = new byte[2048];
        for (int i = 0; i < source.length; i++) {
            source[i] = (byte) (i % 128);
        }
        byte[] target = new byte[2048];

        UnsafeByteArrayOutputStream baos = new UnsafeByteArrayOutputStream(1024);
        long startTime;
        long endTime;
        long encodeTime;
        long decodeTime;
        long size;
        long count = 10000;
        Compression compression;
        for (String type : types) {
            compression = COMPRESSION.get(type);
            encodeTime = 0;
            decodeTime = 0;
            size = 0;
            for (int i = 0; i < count; i++) {
                baos.reset();
                startTime = System.nanoTime();
                OutputStream os = compression.compress(baos);
                os.write(source);
                if (os instanceof Finishable) {
                    ((Finishable) os).finish();
                }
                os.flush();
                endTime = System.nanoTime();
                encodeTime += endTime - startTime;
                size += baos.size();
                ByteArrayInputStream bis = new ByteArrayInputStream(baos.toByteArray());
                startTime = System.nanoTime();
                InputStream is = compression.decompress(bis);
                is.read(target);
                endTime = System.nanoTime();
                decodeTime += endTime - startTime;
            }
            System.out.println(String.format("%s encode_tps %d decode_tps %d size %d", type, count * 1000000000L / encodeTime, count * 1000000000L / decodeTime, size / count));
        }
    }

    @Test
    public void testAdaptive() throws IOException {

        Compression lz4 = COMPRESSION.get("lz4");
        AdaptiveCompressOutputStream acos = new AdaptiveCompressOutputStream(new NettyChannelBuffer(ByteBufAllocator.DEFAULT.buffer(1024)), lz4, 128);
        acos.write(1);
        acos.write(new byte[120]);
        acos.finish();
        Assertions.assertFalse(acos.isCompressed());

        acos = new AdaptiveCompressOutputStream(new NettyChannelBuffer(ByteBufAllocator.DEFAULT.buffer(1024)), lz4, 128);
        acos.write(1);
        acos.write(new byte[127]);
        acos.finish();
        Assertions.assertFalse(acos.isCompressed());

        acos = new AdaptiveCompressOutputStream(new NettyChannelBuffer(ByteBufAllocator.DEFAULT.buffer(1024)), lz4, 128);
        acos.write(1);
        acos.write(new byte[128]);
        acos.finish();
        Assertions.assertTrue(acos.isCompressed());

        acos = new AdaptiveCompressOutputStream(new NettyChannelBuffer(ByteBufAllocator.DEFAULT.buffer(1024)), lz4, 128);
        acos.write(1);
        acos.write(new byte[127]);
        acos.write(1);
        acos.finish();
        Assertions.assertTrue(acos.isCompressed());

        acos = new AdaptiveCompressOutputStream(new NettyChannelBuffer(ByteBufAllocator.DEFAULT.buffer(1024)), lz4, 128);
        acos.write(1);
        acos.write(new byte[100]);
        acos.write(new byte[27]);
        acos.finish();
        Assertions.assertFalse(acos.isCompressed());

        acos = new AdaptiveCompressOutputStream(new NettyChannelBuffer(ByteBufAllocator.DEFAULT.buffer(1024)), lz4, 128);
        acos.write(1);
        acos.write(new byte[100]);
        acos.write(new byte[28]);
        acos.finish();
        Assertions.assertTrue(acos.isCompressed());

    }

}
