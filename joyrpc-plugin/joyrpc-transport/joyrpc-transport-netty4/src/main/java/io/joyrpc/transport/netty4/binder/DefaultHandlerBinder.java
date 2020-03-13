package io.joyrpc.transport.netty4.binder;

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
import io.joyrpc.transport.channel.ChainChannelHandler;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelHandlerChain;
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.netty4.handler.SimpleBizHandler;
import io.joyrpc.transport.netty4.handler.SimpleDecodeHandler;
import io.joyrpc.transport.netty4.handler.SimpleEncodeHandler;
import io.netty.channel.ChannelHandler;

import java.util.function.BiFunction;

@Extension("default")
public class DefaultHandlerBinder implements HandlerBinder {

    /**
     * 函数
     */
    public static final BiFunction<ChannelHandlerChain, Channel, ChannelHandler> FUNCTION = (c, l) ->
            new SimpleBizHandler(new ChainChannelHandler(c, l.getAttribute(Channel.BIZ_THREAD_POOL)), l);

    @Override
    public HandlerMeta<ChannelHandlerChain>[] handlers() {
        return new HandlerChainMeta[]{new HandlerChainMeta(HANDLER, FUNCTION)};
    }

    @Override
    public HandlerMeta<Codec>[] decoders() {
        return new CodecMeta[]{new CodecMeta(DECODER, SimpleDecodeHandler.FUNCTION)};
    }

    @Override
    public HandlerMeta<Codec>[] encoders() {
        return new CodecMeta[]{new CodecMeta(ENCODER, SimpleEncodeHandler.FUNCTION)};
    }
}
