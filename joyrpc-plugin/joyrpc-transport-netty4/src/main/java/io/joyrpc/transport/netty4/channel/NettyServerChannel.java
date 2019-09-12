package io.joyrpc.transport.netty4.channel;

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

import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ServerChannel;
import io.joyrpc.transport.channel.ServerChannelContext;

import java.util.List;

/**
 * @date: 2019/3/6
 */
public class NettyServerChannel extends NettyChannel implements ServerChannel {

    protected ServerChannelContext context;

    public NettyServerChannel(io.netty.channel.Channel channel, ServerChannelContext context) {
        super(channel);
        this.context = context;
    }

    @Override
    public List<Channel> getChannels() {
        return context.getChannels();
    }

}
