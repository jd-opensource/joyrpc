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
 * 处理链读取器
 */
public class ChannelWriterContext implements ChannelContext {
    /**
     * 连接通道
     */
    protected final Channel channel;
    /**
     * 处理器
     */
    protected final ChannelWriter writer;
    /**
     * 链的末尾
     */
    protected final ChannelContext next;

    public ChannelWriterContext(final Channel channel, final ChannelWriter writer, final ChannelContext next) {
        this.channel = channel == null ? next.getChannel() : channel;
        this.writer = writer;
        this.next = next;
    }

    @Override
    public Channel getChannel() {
        return next.getChannel();
    }

    @Override
    public void fireChannelActive() {

    }

    @Override
    public void fireChannelInactive() {

    }

    @Override
    public void fireExceptionCaught(final Throwable cause) {
        writer.caught(next, cause);
    }

    @Override
    public void fireChannelRead(final Object msg) {
    }

    @Override
    public CompletableFuture<Void> wrote(Object msg) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            writer.wrote(next, msg);
            future.complete(null);
        } catch (Throwable e) {
            writer.caught(next, e);
            future.completeExceptionally(e);
        }
        return future;
    }

}
