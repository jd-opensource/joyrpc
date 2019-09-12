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
public class DefaultHttp2ResponseMessage implements Http2ResponseMessage {

    protected int streamId;
    protected int bizMsgId;
    protected Http2Headers httpHeaders;
    protected Http2Headers endHeaders;
    protected byte[] content;


    public DefaultHttp2ResponseMessage(int streamId, byte[] content) {
        this(streamId, null, content);
    }

    public DefaultHttp2ResponseMessage(int streamId, Http2Headers headers, byte[] content) {
        this(streamId, 0, headers, content);
    }

    public DefaultHttp2ResponseMessage(int streamId, int bizMsgId, byte[] content) {
        this(streamId, bizMsgId, null, content);
    }

    public DefaultHttp2ResponseMessage(int streamId, int bizMsgId, Http2Headers headers, byte[] content) {
        this(streamId, bizMsgId, headers, content, null);
    }

    public DefaultHttp2ResponseMessage(int streamId, int bizMsgId, Http2Headers headers, byte[] content, Http2Headers endHeaders) {
        this.streamId = streamId;
        this.bizMsgId = bizMsgId;
        this.content = content;
        this.httpHeaders = headers == null ? new DefaultHttp2Headers() : headers;
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
    public int getBizMsgId() {
        return bizMsgId;
    }

    @Override
    public Http2Headers headers() {
        return this.httpHeaders;
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
