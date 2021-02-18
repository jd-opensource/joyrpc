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

import io.joyrpc.extension.Extensible;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelChain;
import io.joyrpc.transport.codec.Codec;
import io.netty.channel.ChannelPipeline;

/**
 * 管道工厂
 */
@Extensible("pipeline")
public interface PipelineFactory {

    String HANDLER = "handler";
    String DECODER = "decoder";
    String ENCODER = "encoder";
    String CODEC = "codec";
    String HTTP_AGGREGATOR = "http-aggregator";
    String HTTP_REQUEST_NORMALIZER = "http-request-normalizer";
    String HTTP_RESPONSE_CONVERTER = "http-response-converter";
    String HTTP_RESPONSE_NORMALIZER = "http-response-normalizer";

    /**
     * 构建处理链
     *
     * @param pipeline 管道
     * @param codec    编解码
     * @param chain    处理链
     * @param channel  连接通道
     */
    void build(final ChannelPipeline pipeline, final Codec codec, final ChannelChain chain, final Channel channel);

}
