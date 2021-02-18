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
import io.joyrpc.transport.netty4.handler.ChannelOperatorAdapter;
import io.joyrpc.transport.netty4.handler.ChannelReaderAdapter;
import io.joyrpc.transport.netty4.handler.ChannelWriterAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.util.concurrent.EventExecutorGroup;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 抽象的管道工程
 */
public abstract class AbstractPipelineFactory implements PipelineFactory {

    /**
     * 构建管道
     *
     * @param pipeline 管道
     * @param codec    编解码
     * @param channel  连接通道
     * @param group    线程池
     */
    protected abstract void build(ChannelPipeline pipeline, Codec codec, Channel channel, EventExecutorGroup group);

    /**
     * 构建管道
     *
     * @param pipeline 管道
     * @param chain    处理链
     * @param channel  连接通道
     * @param group    线程池
     */
    protected void build(final ChannelPipeline pipeline, final ChannelChain chain, final Channel channel, final EventExecutorGroup group) {
        //处理链
        if (chain != null) {
            chain.getHandlers().forEach((handler -> {
                if (handler instanceof ChannelReader) {
                    if (handler instanceof ChannelWriter) {
                        pipeline.addLast(group, handler.name(), new ChannelOperatorAdapter((ChannelReader) handler, (ChannelWriter) handler, channel));
                    } else {
                        pipeline.addLast(group, handler.name(), new ChannelReaderAdapter((ChannelReader) handler, channel));
                    }
                } else if (handler instanceof ChannelWriter) {
                    pipeline.addLast(group, handler.name(), new ChannelWriterAdapter((ChannelWriter) handler, channel));
                }
            }));
        }
    }

    @Override
    public void build(final ChannelPipeline pipeline, final Codec codec, final ChannelChain chain, final Channel channel) {
        //TODO 业务线程池
        ThreadPoolExecutor executor = channel.getAttribute(Channel.BIZ_THREAD_POOL);
        EventExecutorGroup group = executor == null ? null : null;
        if (codec != null) {
            //解码器
            build(pipeline, codec, channel, group);
        }
        build(pipeline, chain, channel, group);
    }
}
