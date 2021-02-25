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

import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelChain;
import io.joyrpc.transport.channel.ChannelReader;
import io.joyrpc.transport.channel.ChannelWriter;
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.netty4.handler.ChannelChainReaderAdapter;
import io.joyrpc.transport.netty4.handler.ChannelChainWriterAdapter;
import io.netty.channel.ChannelPipeline;

/**
 * 抽象的管道工程
 */
public abstract class AbstractPipelineFactory implements PipelineFactory {

    public static final String CHANNEL_CHAIN_READER = "channelChainReader";
    public static final String CHANNEL_CHAIN_WRITER = "channelChainWriter";

    /**
     * 构建管道
     *
     * @param pipeline 管道
     * @param codec    编解码
     * @param channel  连接通道
     */
    protected abstract void build(ChannelPipeline pipeline, Codec codec, Channel channel);

    /**
     * 构建管道
     *
     * @param pipeline 管道
     * @param chain    处理链
     * @param channel  连接通道
     */
    protected void build(final ChannelPipeline pipeline, final ChannelChain chain, final Channel channel) {
        //处理链
        if (chain != null) {
            ChannelReader[] readers = chain.getReaders();
            ChannelWriter[] writers = chain.getWriters();
            if (readers != null && readers.length > 0) {
                pipeline.addLast(CHANNEL_CHAIN_READER, new ChannelChainReaderAdapter(readers, channel));
            }
            if (writers != null && writers.length > 0) {
                pipeline.addLast(CHANNEL_CHAIN_WRITER, new ChannelChainWriterAdapter(writers, channel));
            }
        }
    }

    @Override
    public void build(final ChannelPipeline pipeline, final Codec codec, final ChannelChain chain, final Channel channel) {
        if (codec != null) {
            //解码器
            build(pipeline, codec, channel);
        }
        build(pipeline, chain, channel);
    }
}
