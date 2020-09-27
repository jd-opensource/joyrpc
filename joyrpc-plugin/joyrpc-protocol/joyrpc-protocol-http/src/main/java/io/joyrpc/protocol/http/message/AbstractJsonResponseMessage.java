package io.joyrpc.protocol.http.message;

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
import io.joyrpc.protocol.http.HttpResponse;
import io.joyrpc.protocol.message.MessageHeader;
import io.joyrpc.protocol.message.ResponseMessage;
import io.joyrpc.protocol.message.ResponsePayload;
import io.joyrpc.transport.http.DefaultHttpResponseMessage;
import io.joyrpc.transport.http.HttpHeaders;
import io.joyrpc.transport.http.HttpResponseMessage;

import java.io.IOException;

import static io.joyrpc.Plugin.COMPRESSION;
import static io.joyrpc.protocol.http.HeaderMapping.ACCEPT_ENCODING;
import static io.joyrpc.protocol.http.HeaderMapping.KEEP_ALIVE;
import static io.joyrpc.transport.http.HttpHeaders.Names.*;
import static io.joyrpc.util.StringUtils.SEMICOLON_COMMA_WHITESPACE;
import static io.joyrpc.util.StringUtils.split;

/**
 * JSON应答
 */
public abstract class AbstractJsonResponseMessage extends ResponseMessage<ResponsePayload> implements HttpResponse {
    /**
     * 状态
     */
    protected transient int status;
    /**
     * 内容
     */
    protected transient byte[] content;

    /**
     * 渲染
     */
    protected abstract void render();

    public AbstractJsonResponseMessage() {
    }

    public AbstractJsonResponseMessage(MessageHeader header, ResponsePayload response) {
        super(header, response);
    }

    @Override
    public HttpResponseMessage apply() {
        HttpResponseMessage result = new DefaultHttpResponseMessage();
        HttpHeaders headers = result.headers();
        //转换
        render();
        //进行压缩
        if (content != null && content.length > 1024) {
            //超过1K再压缩
            String encodings = (String) header.getAttribute(ACCEPT_ENCODING.getNum());
            Compression compression = COMPRESSION.get(split(encodings, SEMICOLON_COMMA_WHITESPACE));
            if (compression != null) {
                try {
                    content = compression.compress(content);
                    headers.set(CONTENT_ENCODING, compression.getTypeName());
                } catch (IOException e) {
                    headers.remove(CONTENT_ENCODING);
                }
            }
        }
        headers.set(CONTENT_TYPE, "text/json; charset=UTF-8");
        headers.set(CONTENT_LENGTH, content.length);
        headers.setKeepAlive(Boolean.TRUE.equals(header.getAttribute(KEEP_ALIVE.getNum())));
        result.setContent(content);
        result.setStatus(status);
        return result;
    }
}
