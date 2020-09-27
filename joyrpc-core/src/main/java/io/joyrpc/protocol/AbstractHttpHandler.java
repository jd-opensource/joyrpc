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
import io.joyrpc.protocol.message.Message;
import io.joyrpc.transport.channel.ChannelHandler;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.function.Consumer;

import static io.joyrpc.Plugin.COMPRESSION;
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
     * 压缩
     *
     * @param content        内容
     * @param message        消息
     * @param acceptEncoding 接收的编解码
     * @param consumer       消费者
     * @return 字节数组
     */
    protected byte[] compress(byte[] content, final Message<?> message, final byte acceptEncoding,
                              final Consumer<String> consumer) {
        if (content.length > 1024) {
            //超过1K再压缩
            String encodings = (String) message.getHeader().getAttribute(acceptEncoding);
            Compression compression = getEncoding(encodings);
            if (compression != null) {
                try {
                    content = compression.compress(content);
                    consumer.accept(compression.getTypeName());
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
     * @param encodings 待解析编解码
     * @return 编解码
     */
    protected Compression getEncoding(final String encodings) {
        Compression compression;
        if (encodings != null && !encodings.isEmpty()) {
            String[] accepts = split(encodings, SEMICOLON_COMMA_WHITESPACE);
            for (String accept : accepts) {
                compression = COMPRESSION.get(accept);
                if (compression != null) {
                    return compression;
                }
            }
        }
        return null;
    }

}
