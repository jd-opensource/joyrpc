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
 * 默认http2消息
 */
public class AbstractHttp2Message implements Http2Message {
    /**
     * 流ID
     */
    protected int streamId;
    /**
     * 消息ID
     */
    protected long msgId;
    /**
     * 开始头
     */
    protected Http2Headers headers;
    /**
     * 数据包
     */
    protected byte[] content;
    /**
     * 结束头
     */
    protected Http2Headers endHeaders;
    /**
     * 结束标识
     */
    protected boolean end;

    public AbstractHttp2Message(int streamId, long msgId, Http2Headers headers, byte[] content, Http2Headers endHeaders, boolean end) {
        this.streamId = streamId;
        this.msgId = msgId;
        this.headers = headers;
        this.content = content;
        this.endHeaders = endHeaders;
        this.end = end;
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
        return headers;
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
    public boolean isEnd() {
        return end;
    }
}
