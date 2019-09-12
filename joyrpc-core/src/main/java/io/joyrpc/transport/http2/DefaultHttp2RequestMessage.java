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
 * @date: 2019/4/10
 */
public class DefaultHttp2RequestMessage implements Http2RequestMessage {

    protected int streamId;
    protected int bizMsgId;
    protected Http2Headers httpHeaders;
    protected Http2Headers endHeaders;
    protected byte[] content;

    public DefaultHttp2RequestMessage(int streamId, byte[] content) {
        this(streamId, null, content);
    }

    public DefaultHttp2RequestMessage(int streamId, Http2Headers httpHeaders, byte[] content) {
        this(streamId, 0, httpHeaders, content);
    }


    public DefaultHttp2RequestMessage(int streamId, int bizMsgId, byte[] content) {
        this(streamId, bizMsgId, null, content);
    }

    public DefaultHttp2RequestMessage(int streamId, int bizMsgId, Http2Headers httpHeaders, byte[] content) {
        this.streamId = streamId;
        this.bizMsgId = bizMsgId;
        this.httpHeaders = httpHeaders == null ? new DefaultHttp2Headers() : httpHeaders;
        this.content = content;
    }

    @Override
    public int getStreamId() {
        return streamId;
    }

    @Override
    public void setStreamId(int streamId) {
        this.streamId = streamId;
    }

    @Override
    public int getBizMsgId() {
        return bizMsgId;
    }

    @Override
    public Http2Headers headers() {
        return httpHeaders;
    }

    @Override
    public byte[] content() {
        return content;
    }

    @Override
    public Http2Headers endHeaders() {
        return endHeaders;
    }

    @Override
    public void setEndHeaders(Http2Headers endHeaders) {
        this.endHeaders = endHeaders;
    }
}
