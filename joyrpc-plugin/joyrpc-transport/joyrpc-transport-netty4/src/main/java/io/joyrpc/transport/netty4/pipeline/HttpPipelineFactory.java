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
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelChain;
import io.joyrpc.transport.channel.ChannelChainHandler;
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.netty4.http.HttpRequestHandler;
import io.joyrpc.transport.netty4.http.HttpResponseHandler;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

/**
 * http管道工厂
 */
@Extension("http")
public class HttpPipelineFactory implements PipelineFactory {

    @Override
    public HandlerDefinition<ChannelChain>[] handlers() {
        return new HandlerChainMeta[]{
                new HandlerChainMeta(HANDLER,
                        (chain, channel) -> new HttpRequestHandler(new ChannelChainHandler(chain, channel.getAttribute(Channel.BIZ_THREAD_POOL)), channel)),
                new HandlerChainMeta(HTTP_RESPONSE_CONVERTER,
                        (chain, channel) -> new HttpResponseHandler(new ChannelChainHandler(chain), channel))
        };
    }

    @Override
    public HandlerDefinition<Codec>[] decoders() {
        return new CodecDefinition[]{
                new CodecDefinition(DECODER, (codec, channel) -> new HttpRequestDecoder()),
                new CodecDefinition(HTTP_AGGREGATOR, (codec, channel) -> new HttpObjectAggregator(65535))
        };
    }

    @Override
    public HandlerDefinition<Codec>[] encoders() {
        return new CodecDefinition[]{new CodecDefinition(ENCODER, (codec, channel) -> new HttpResponseEncoder())};
    }
}
