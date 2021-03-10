package io.joyrpc.transport.netty4.http2;

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

import io.joyrpc.transport.http2.Http2Headers;
import io.netty.handler.codec.http2.DefaultHttp2Headers;

import java.util.Map;

/**
 * Netty的Http2消息头
 */
public class Http2NettyHeaders extends DefaultHttp2Headers {

    public Http2NettyHeaders(Http2Headers headers) {
        if (headers != null) {
            Map<CharSequence, Object> map = headers.getAll();
            if (map != null) {
                for (Map.Entry<CharSequence, Object> entry : map.entrySet()) {
                    add(entry.getKey(), entry.getValue().toString());
                }
            }
        }
    }
}
