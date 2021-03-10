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

/**
 * 默认http2请求消息
 */
public class DefaultHttp2RequestMessage extends AbstractHttp2Message implements Http2RequestMessage {

    public DefaultHttp2RequestMessage(int streamId, long msgId, Http2Headers headers, byte[] content, Http2Headers endHeaders, boolean end) {
        super(streamId, msgId, headers, content, endHeaders, end);
    }

    @Override
    public void setStreamId(int streamId) {
        this.streamId = streamId;
    }
}
