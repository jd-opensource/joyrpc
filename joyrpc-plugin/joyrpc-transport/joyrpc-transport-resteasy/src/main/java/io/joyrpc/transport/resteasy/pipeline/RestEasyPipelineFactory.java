package io.joyrpc.transport.resteasy.pipeline;

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
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.netty4.pipeline.PipelineFactory;
import io.joyrpc.transport.resteasy.codec.RestEasyCodec;
import io.joyrpc.transport.resteasy.handler.RestEasyDispatcher;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.resteasy.plugins.server.netty.RequestDispatcher;
import org.jboss.resteasy.plugins.server.netty.RestEasyHttpRequestDecoder;
import org.jboss.resteasy.plugins.server.netty.RestEasyHttpResponseEncoder;

/**
 * ReastEasy处理器绑定
 */
@Extension("resteasy")
public class RestEasyPipelineFactory implements PipelineFactory {

    private static final String RESTEASY_HTTP_DECODER = "resteasy-http-decoder";

    private static final String RESTEASY_HTTP_ENCODER = "resteasy-http-encoder";

    private static final String RESTEASY_HTTP_DISPATCHER = "resteasy-http-dispatcher";

    @Override
    public void build(ChannelPipeline pipeline, Codec codec, ChannelChain chain, Channel channel) {
        RestEasyCodec resteasyCodec = (RestEasyCodec) codec;
        String root = resteasyCodec.getRoot();
        RequestDispatcher dispatcher = resteasyCodec.getDispatcher();
        pipeline.addLast(DECODER, new HttpRequestDecoder());
        pipeline.addLast(HTTP_AGGREGATOR, new HttpObjectAggregator(channel.getPayloadSize()));
        pipeline.addLast(RESTEASY_HTTP_DECODER, new RestEasyHttpRequestDecoder(dispatcher.getDispatcher(), root, RestEasyHttpRequestDecoder.Protocol.HTTP));
        pipeline.addLast(RESTEASY_HTTP_DISPATCHER, new RestEasyDispatcher(dispatcher));
        pipeline.addLast(ENCODER, new HttpResponseEncoder());
        pipeline.addLast(RESTEASY_HTTP_ENCODER, new RestEasyHttpResponseEncoder(dispatcher));
    }
}
