package io.joyrpc.transport.channel;

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

import io.joyrpc.transport.transport.ChannelTransport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @date: 2019/3/6
 */
public class ServerChannelContext {

    protected Map<Channel, ChannelTransport> transports = new ConcurrentHashMap<>();

    /**
     * 返回所有的Channel
     *
     * @return
     */
    public List<Channel> getChannels() {
        return new ArrayList<>(transports.keySet());
    }

    /**
     * 返回所有的Transport
     *
     * @return
     */
    public List<ChannelTransport> getChannelTransports() {
        return new ArrayList<>(transports.values());
    }

    /**
     * 绑定Channel和Transport
     *
     * @param channel
     * @param transport
     */
    public void addChannel(final Channel channel, final ChannelTransport transport) {
        if (channel != null && transport != null) {
            transports.put(channel, transport);
        }
    }

    /**
     * 删除Channel
     *
     * @param channel
     */
    public void removeChannel(final Channel channel) {
        if (channel != null) {
            transports.remove(channel);
        }
    }
}
