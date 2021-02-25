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
 * 默认http2应答消息
 */
public class DefaultHttp2ResponseMessage implements Http2ResponseMessage {

    protected int streamId;
    protected long msgId;
    protected Http2Headers headers;
    protected Http2Headers endHeaders;
    protected byte[] content;

    public DefaultHttp2ResponseMessage(int streamId, byte[] content) {
        this(streamId, null, content);
    }

    public DefaultHttp2ResponseMessage(int streamId, Http2Headers headers, byte[] content) {
        this(streamId, 0, headers, content);
    }

    public DefaultHttp2ResponseMessage(int streamId, long msgId, byte[] content) {
        this(streamId, msgId, null, content);
    }

    public DefaultHttp2ResponseMessage(int streamId, long msgId, Http2Headers headers, byte[] content) {
        this(streamId, msgId, headers, content, null);
    }

    public DefaultHttp2ResponseMessage(int streamId, long msgId, Http2Headers headers, byte[] content, Http2Headers endHeaders) {
        this.streamId = streamId;
        this.msgId = msgId;
        this.content = content;
        this.headers = headers == null ? new DefaultHttp2Headers() : headers;
        this.endHeaders = endHeaders;
    }

    @Override
    public void setContent(byte[] bytes) {
        this.content = bytes;
    }

    @Override
    public int getStreamId() {
        return this.streamId;
    }

    @Override
    public long getMsgId() {
        return msgId;
    }

    @Override
    public Http2Headers headers() {
        return this.headers;
    }

    @Override
    public byte[] content() {
        return content;
    }

    @Override
    public Http2Headers getEndHeaders() {
        return endHeaders;
    }

    @Override
    public void setEndHeaders(Http2Headers endHeaders) {
        this.endHeaders = endHeaders;
    }
}
