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
     * 头
     */
    protected Http2Headers headers;
    /**
     * 结束标识
     */
    protected boolean end;
    /**
     * 数据包
     */
    protected byte[] content;

    public AbstractHttp2Message(int streamId, long msgId, Http2Headers headers, byte[] content) {
        this(streamId, msgId, headers, content, false);
    }

    public AbstractHttp2Message(int streamId, long msgId, Iterable<Map.Entry<CharSequence, CharSequence>> headers, byte[] content) {
        this(streamId, msgId, new DefaultHttp2Headers(headers), content, false);
    }

    public AbstractHttp2Message(int streamId, long msgId, Http2Headers headers, byte[] content, boolean end) {
        this.streamId = streamId;
        this.msgId = msgId;
        this.content = content;
        this.headers = headers == null ? new DefaultHttp2Headers() : headers;
        this.end = end;
    }

    public AbstractHttp2Message(int streamId, long msgId, Iterable<Map.Entry<CharSequence, CharSequence>> headers, byte[] content, boolean end) {
        this(streamId, msgId, new DefaultHttp2Headers(headers), content, end);
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
    public boolean isEnd() {
        return end;
    }
}
