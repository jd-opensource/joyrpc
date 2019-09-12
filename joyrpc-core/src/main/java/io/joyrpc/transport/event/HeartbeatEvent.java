package io.joyrpc.transport.event;

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

import io.joyrpc.extension.URL;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.message.Message;

/**
 * @date: 2019/1/10
 */
public class HeartbeatEvent extends ChannelEvent {
    /**
     * 心跳应答消息
     */
    protected Message response;
    /**
     * URL
     */
    protected URL url;

    public HeartbeatEvent(Message response, Channel channel, URL url) {
        super(channel);
        this.response = response;
        this.url = url;
    }

    public HeartbeatEvent(Channel channel, URL url, Throwable throwable) {
        super(channel, throwable);
        this.url = url;
    }

    public Message getResponse() {
        return response;
    }

    public URL getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return "HeartbeatEvent{" +
                "success=" + success +
                ", response=" + response +
                ", url=" + url +
                ", channel=" + channel +
                ", throwable=" + throwable +
                '}';
    }
}
