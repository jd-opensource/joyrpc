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
import io.joyrpc.transport.netty4.http.HttpResponseConverterHandler;
import io.joyrpc.transport.netty4.http.SimpleHttpBizHandler;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

/**
 * @date: 2019/2/20
 */
@Extension("http")
public class HttpHandlerBinder implements HandlerBinder {

    @Override
    public HandlerMeta<ChannelHandlerChain>[] handlers() {
        return new HandlerChainMeta[]{
                new HandlerChainMeta(HANDLER, (c, l) -> new SimpleHttpBizHandler(new ChainChannelHandler(c, l.getAttribute(Channel.BIZ_THREAD_POOL)), l)),
                new HandlerChainMeta(HTTP_RESPONSE_CONVERTER, (c, l) -> new HttpResponseConverterHandler(new ChainChannelHandler(c), l))
        };
    }

    @Override
    public HandlerMeta<Codec>[] decoders() {
        return new CodecMeta[]{
                new CodecMeta(DECODER, (c, l) -> new HttpRequestDecoder()),
                new CodecMeta(HTTP_AGGREGATOR, (c, l) -> new HttpObjectAggregator(65535))
        };
    }

    @Override
    public HandlerMeta<Codec>[] encoders() {
        return new CodecMeta[]{new CodecMeta(ENCODER, (c, l) -> new HttpResponseEncoder())};
    }
}
