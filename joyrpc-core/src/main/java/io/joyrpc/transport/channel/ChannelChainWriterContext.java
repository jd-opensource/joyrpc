package io.joyrpc.transport.channel;

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


import java.util.concurrent.CompletableFuture;

/**
 * 输出链上下文
 */
public class ChannelChainWriterContext implements ChannelContext {
    /**
     * 连接通道
     */
    protected final Channel channel;
    /**
     * 处理器
     */
    protected final ChannelWriter[] writers;
    /**
     * 上下文
     */
    protected final ChannelContext[] contexts;
    /**
     * 索引位置
     */
    protected int pos;

    public ChannelChainWriterContext(final Channel channel, final ChannelWriter[] writers, final ChannelContext last) {
        this.channel = channel;
        this.writers = writers;
        this.contexts = new ChannelContext[writers.length];
        for (int i = 0; i < writers.length; i++) {
            if (i == writers.length - 1) {
                contexts[i] = last;
            } else {
                contexts[i] = new ChannelChainWriterContext(channel, writers, contexts, i + 1);
            }
        }
    }

    protected ChannelChainWriterContext(final Channel channel, final ChannelWriter[] writers, final ChannelContext[] contexts, final int pos) {
        this.channel = channel;
        this.writers = writers;
        this.contexts = contexts;
        this.pos = pos;
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    public void fireChannelActive() {

    }

    @Override
    public void fireChannelInactive() {

    }

    @Override
    public void fireExceptionCaught(final Throwable cause) {
        if (pos < writers.length) {
            ChannelWriter writer = writers[pos];
            ChannelContext context = contexts[pos];
            writer.caught(context, cause);
        }
    }

    @Override
    public void fireChannelRead(final Object msg) {
    }

    @Override
    public CompletableFuture<Void> wrote(final Object msg) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (pos < writers.length) {
            ChannelWriter writer = writers[pos];
            ChannelContext context = contexts[pos];
            try {
                writer.wrote(context, msg);
                future.complete(null);
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        } else {
            future.complete(null);
        }
        return future;
    }

}
