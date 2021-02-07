package io.joyrpc.transport.netty4.pipeline;

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

import io.joyrpc.extension.Extension;
import io.joyrpc.transport.channel.ChannelChainHandler;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelChain;
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.netty4.handler.ChannelHandlerAdapter;
import io.joyrpc.transport.netty4.handler.MessageDecoder;
import io.joyrpc.transport.netty4.handler.MessageEncoder;
import io.netty.channel.ChannelHandler;

import java.util.function.BiFunction;

/**
 * 默认管道工厂
 */
@Extension("default")
public class DefaultPipelineFactory implements PipelineFactory {

    /**
     * 函数
     */
    public static final BiFunction<ChannelChain, Channel, ChannelHandler> FUNCTION = (c, l) ->
            new ChannelHandlerAdapter(new ChannelChainHandler(c, l.getAttribute(Channel.BIZ_THREAD_POOL)), l);

    @Override
    public HandlerDefinition<ChannelChain>[] handlers() {
        return new HandlerChainMeta[]{new HandlerChainMeta(HANDLER, FUNCTION)};
    }

    @Override
    public HandlerDefinition<Codec>[] decoders() {
        return new CodecDefinition[]{new CodecDefinition(DECODER, MessageDecoder.FUNCTION)};
    }

    @Override
    public HandlerDefinition<Codec>[] encoders() {
        return new CodecDefinition[]{new CodecDefinition(ENCODER, MessageEncoder.FUNCTION)};
    }
}
