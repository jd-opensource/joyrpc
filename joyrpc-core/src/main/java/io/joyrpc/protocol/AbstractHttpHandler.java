package io.joyrpc.protocol;

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

import io.joyrpc.codec.compression.Compression;
import io.joyrpc.codec.compression.Finishable;
import io.joyrpc.codec.serialization.Serialization;
import io.joyrpc.codec.serialization.UnsafeByteArrayInputStream;
import io.joyrpc.codec.serialization.UnsafeByteArrayOutputStream;
import io.joyrpc.constants.Constants;
import io.joyrpc.extension.Parametric;
import io.joyrpc.protocol.message.BaseMessage;
import io.joyrpc.transport.channel.ChannelHandler;
import io.joyrpc.util.Close;
import io.joyrpc.util.Pair;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;

import static io.joyrpc.Plugin.COMPRESSION;
import static io.joyrpc.Plugin.SERIALIZATION;
import static io.joyrpc.util.StringUtils.SEMICOLON_COMMA_WHITESPACE;
import static io.joyrpc.util.StringUtils.split;

/**
 * @date: 2019/5/6
 */
public abstract class AbstractHttpHandler implements ChannelHandler {

    /**
     * 获取日志
     *
     * @return
     */
    protected abstract Logger getLogger();

    /**
     * 解压缩
     *
     * @param compression
     * @param content
     * @return
     * @throws IOException
     */
    protected byte[] decompress(final Compression compression, final byte[] content) throws IOException {
        if (compression == null) {
            return content;
        }
        UnsafeByteArrayOutputStream baos = new UnsafeByteArrayOutputStream();
        UnsafeByteArrayInputStream bais = new UnsafeByteArrayInputStream(content);
        InputStream is = null;
        try {
            is = compression.decompress(bais);
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            baos.flush();
            return baos.toByteArray();
        } finally {
            Close.close(is).close(bais).close(baos);
        }
    }

    /**
     * 压缩
     *
     * @param compression 压缩算法
     * @param content     待压缩内容
     * @return
     * @throws IOException
     */
    protected byte[] compress(final Compression compression, final byte[] content) throws IOException {
        return compress(compression, null, content, 0, content.length);
    }

    /**
     * 压缩
     *
     * @param compression 压缩算法
     * @param content     待压缩内容
     * @param offset      待压缩内容偏移量
     * @param length      待压缩内容长度
     * @return
     * @throws IOException
     */
    protected byte[] compress(final Compression compression, final byte[] content, final int offset, final int length) throws IOException {
        return compress(compression, null, content, offset, length);
    }

    /**
     * 压缩
     *
     * @param compression 压缩算法
     * @param baos        压缩输出流，便于外部提前写入header
     * @param content     待压缩内容
     * @param offset      待压缩内容偏移量
     * @param length      待压缩内容长度
     * @return
     * @throws IOException
     */
    protected byte[] compress(final Compression compression, final UnsafeByteArrayOutputStream baos,
                              final byte[] content, final int offset, final int length) throws IOException {
        if (compression == null) {
            return content;
        }
        UnsafeByteArrayOutputStream buffer = baos == null ? new UnsafeByteArrayOutputStream() : baos;
        OutputStream os = null;
        try {
            os = compression.compress(buffer);
            os.write(content, offset, length);
            //先写完数据
            if (os instanceof Finishable) {
                ((Finishable) os).finish();
            }
            //再提交数据
            os.flush();
            return buffer.toByteArray();
        } finally {
            Close.close(os).close(buffer);
        }
    }

    /**
     * 压缩
     *
     * @param content
     * @param message
     * @param acceptEncoding
     * @param consumer
     * @return
     */
    protected byte[] compress(byte[] content, final BaseMessage message, final byte acceptEncoding,
                              final Consumer<String> consumer) {
        if (content.length > 1024) {
            //超过1K再压缩
            String encodings = (String) message.getHeader().getAttribute(acceptEncoding);
            Pair<String, Compression> pair = getEncoding(encodings);
            if (pair != null) {
                try {
                    content = compress(pair.getValue(), content);
                    consumer.accept(pair.getKey());
                } catch (IOException e) {
                    getLogger().error("Error occurs while compressing.", e);
                }
            }
        }
        return content;
    }

    /**
     * 获取编解码
     *
     * @param encodings
     * @return
     */
    protected Pair<String, Compression> getEncoding(final String encodings) {
        Compression compression = null;
        String encoding = null;
        if (encodings != null && !encodings.isEmpty()) {
            String[] accepts = split(encodings, SEMICOLON_COMMA_WHITESPACE);
            for (String accept : accepts) {
                compression = COMPRESSION.get(accept);
                if (compression != null) {
                    encoding = accept;
                    break;
                }
            }
        }
        return compression == null ? null : new Pair<>(encoding, compression);
    }

    /**
     * 获取压缩
     *
     * @param parametric
     * @param header
     * @return
     */
    protected Compression getCompression(final Parametric parametric, final String header) {
        //Content-Encoding:gzip
        String type = parametric.getString(header);
        return type == null ? null : COMPRESSION.get(type);
    }

    /**
     * 获取序列化
     *
     * @param parametric
     * @param header
     * @param defSerial
     * @return
     */
    protected Serialization getSerialization(final Parametric parametric, final String header, final Serialization defSerial) {
        Serialization result = null;
        //Content-Type:application/grpc+proto
        //Content-Type:application/grpc+json
        String type = parametric.getString(header);
        if (type != null) {
            int pos = type.lastIndexOf('+');
            if (pos > 0) {
                type = type.substring(pos + 1);
                result = SERIALIZATION.get(type);
            }
        }
        return result == null ? defSerial : result;
    }

    /**
     * 获取超时时间
     *
     * @param parametric
     * @param header
     * @return
     */
    protected int getTimeout(final Parametric parametric, final String header) {
        String value = parametric.getString(header);
        int timeout = 0;
        if (value != null && !value.isEmpty()) {
            //兼容grpc-time格式
            try {
                int last = value.length() - 1;
                switch (value.charAt(last)) {
                    case 'H':
                        timeout = Integer.parseInt(value.substring(0, last)) * 3600 * 1000;
                        break;
                    case 'M':
                        timeout = Integer.parseInt(value.substring(0, last)) * 60 * 1000;
                        break;
                    case 'S':
                        timeout = Integer.parseInt(value.substring(0, last)) * 1000;
                        break;
                    case 'm':
                        timeout = Integer.parseInt(value.substring(0, last));
                        break;
                    case 'u':
                        break;
                    case 'n':
                        break;
                    default:
                        timeout = Integer.parseInt(value);
                }
            } catch (NumberFormatException e) {
            }
        }
        return timeout <= 0 ? Constants.TIMEOUT_OPTION.getValue() : timeout;

    }

}
