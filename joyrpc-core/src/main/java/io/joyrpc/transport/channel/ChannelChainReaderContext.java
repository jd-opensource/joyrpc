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
 * 输入链上下文
 */
public class ChannelChainReaderContext implements ChannelContext {
    /**
     * 连接通道
     */
    protected final Channel channel;
    /**
     * 处理器
     */
    protected final ChannelReader[] readers;
    /**
     * 上下文
     */
    protected final ChannelContext[] contexts;
    /**
     * 索引位置
     */
    protected int pos;

    public ChannelChainReaderContext(final Channel channel, final ChannelReader[] readers, final ChannelContext last) {
        this.channel = channel;
        this.readers = readers;
        this.contexts = new ChannelContext[readers.length];
        for (int i = 0; i < readers.length; i++) {
            if (i == readers.length - 1) {
                contexts[i] = last;
            } else {
                contexts[i] = new ChannelChainReaderContext(channel, readers, contexts, i + 1);
            }
        }
    }

    protected ChannelChainReaderContext(final Channel channel, final ChannelReader[] readers, final ChannelContext[] contexts, final int pos) {
        this.channel = channel;
        this.readers = readers;
        this.contexts = contexts;
        this.pos = pos;
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    public void fireChannelActive() {
        if (pos < readers.length) {
            ChannelReader reader = readers[pos];
            ChannelContext context = contexts[pos];
            try {
                reader.active(context);
            } catch (Throwable e) {
                reader.caught(context, e);
            }
        }
    }

    @Override
    public void fireChannelInactive() {
        if (pos < readers.length) {
            ChannelReader reader = readers[pos];
            ChannelContext context = contexts[pos];
            try {
                reader.inactive(context);
            } catch (Throwable e) {
                reader.caught(context, e);
            }
        }
    }

    @Override
    public void fireExceptionCaught(final Throwable cause) {
        if (pos < readers.length) {
            ChannelReader reader = readers[pos];
            ChannelContext context = contexts[pos];
            reader.caught(context, cause);
        }
    }

    @Override
    public void fireChannelRead(final Object msg) {
        if (pos < readers.length) {
            ChannelReader reader = readers[pos];
            ChannelContext context = contexts[pos];
            try {
                reader.received(context, msg);
            } catch (Throwable e) {
                reader.caught(context, e);
            }
        }
    }

    @Override
    public CompletableFuture<Void> wrote(final Object msg) {
        return channel.send(msg);
    }

}
