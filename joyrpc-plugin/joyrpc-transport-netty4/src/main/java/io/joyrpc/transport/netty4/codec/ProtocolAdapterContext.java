package io.joyrpc.transport.netty4.codec;

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

import io.joyrpc.exception.ProtocolException;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelHandlerChain;
import io.joyrpc.transport.codec.AdapterContext;
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.netty4.Plugin;
import io.joyrpc.transport.netty4.binder.HandlerBinder;
import io.netty.channel.ChannelPipeline;

/**
 * 协议转换上下文
 */
public class ProtocolAdapterContext implements AdapterContext {

    protected ChannelPipeline pipeline;
    protected Channel channel;

    public ProtocolAdapterContext(Channel channel, ChannelPipeline pipeline) {
        this.channel = channel;
        this.pipeline = pipeline;
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    public void bind(final Codec codec, final ChannelHandlerChain chain) {
        if (codec == null) {
            throw new NullPointerException("codec is not found.");
        }
        HandlerBinder binder = Plugin.HANDLER_BINDER.get(codec.binder());
        if (binder == null) {
            throw new ProtocolException(String.format("handler binder %s is not found.", codec.binder()));
        }
        binder.bind(pipeline, codec, chain, channel);
    }
}
