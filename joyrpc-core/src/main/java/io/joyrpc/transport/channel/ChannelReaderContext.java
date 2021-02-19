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
 * 读取器上下文
 */
public class ChannelReaderContext implements ChannelContext {
    /**
     * 连接通道
     */
    protected final Channel channel;
    /**
     * 处理器
     */
    protected final ChannelReader reader;
    /**
     * 链的末尾
     */
    protected final ChannelContext next;

    public ChannelReaderContext(final Channel channel, final ChannelReader reader, final ChannelContext next) {
        this.channel = channel == null ? next.getChannel() : channel;
        this.reader = reader;
        this.next = next;
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    public void fireChannelActive() {
        try {
            reader.active(next);
        } catch (Throwable e) {
            reader.caught(next, e);
        }
    }

    @Override
    public void fireChannelInactive() {
        try {
            reader.inactive(next);
        } catch (Throwable e) {
            reader.caught(next, e);
        }
    }

    @Override
    public void fireExceptionCaught(final Throwable cause) {
        reader.caught(next, cause);
    }

    @Override
    public void fireChannelRead(Object msg) {
        try {
            reader.received(next, msg);
        } catch (Throwable e) {
            reader.caught(next, e);
        }
    }

    @Override
    public CompletableFuture<Void> wrote(Object msg) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        channel.send(msg, result -> {
            if (result.isSuccess()) {
                future.complete(null);
            } else {
                future.completeExceptionally(result.getThrowable());
            }
        });
        return future;
    }

}
