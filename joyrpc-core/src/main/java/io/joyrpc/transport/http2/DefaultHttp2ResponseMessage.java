package io.joyrpc.transport.http2;

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

import java.util.Map;

/**
 * 默认http2应答消息
 */
public class DefaultHttp2ResponseMessage extends AbstractHttp2Message implements Http2ResponseMessage {

    public DefaultHttp2ResponseMessage(int streamId, long msgId, Http2Headers headers, byte[] content) {
        super(streamId, msgId, headers, content);
    }

    public DefaultHttp2ResponseMessage(int streamId, long msgId, Iterable<Map.Entry<CharSequence, CharSequence>> headers, byte[] content) {
        super(streamId, msgId, headers, content);
    }

    public DefaultHttp2ResponseMessage(int streamId, long msgId, Http2Headers headers, byte[] content, boolean end) {
        super(streamId, msgId, headers, content, end);
    }

    public DefaultHttp2ResponseMessage(int streamId, long msgId, Iterable<Map.Entry<CharSequence, CharSequence>> headers, byte[] content, boolean end) {
        super(streamId, msgId, headers, content, end);
    }
}
