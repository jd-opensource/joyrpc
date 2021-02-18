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
import io.joyrpc.transport.channel.ChannelChain;
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.codec.DeductionContext;
import io.joyrpc.transport.netty4.pipeline.PipelineFactory;
import io.netty.channel.ChannelPipeline;

import static io.joyrpc.transport.netty4.Plugin.PIPELINE_FACTORY;

/**
 * 协议推断上下文
 */
public class NettyDeductionContext implements DeductionContext {
    /**
     * 管道
     */
    protected final ChannelPipeline pipeline;
    /**
     * 连接通道
     */
    protected final Channel channel;

    public NettyDeductionContext(Channel channel, ChannelPipeline pipeline) {
        this.channel = channel;
        this.pipeline = pipeline;
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    public void bind(final Codec codec, final ChannelChain chain) {
        if (codec == null) {
            throw new NullPointerException("codec is not found.");
        }
        PipelineFactory factory = PIPELINE_FACTORY.get(codec.pipeline());
        if (factory == null) {
            throw new ProtocolException(String.format("handler binder %s is not found.", codec.pipeline()));
        }
        factory.build(pipeline, codec, chain, channel);
    }

    /**
     * 构建方法
     *
     * @param channel  连接通道
     * @param pipeline 管道
     * @return 推断上下文
     */
    public static DeductionContext create(final Channel channel, final ChannelPipeline pipeline) {
        return new NettyDeductionContext(channel, pipeline);
    }
}
