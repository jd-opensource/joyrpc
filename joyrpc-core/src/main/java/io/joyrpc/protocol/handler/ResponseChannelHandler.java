package io.joyrpc.protocol.handler;

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
import io.joyrpc.transport.channel.ChannelContext;
import io.joyrpc.transport.channel.ChannelHandler;
import io.joyrpc.transport.channel.FutureManager;
import io.joyrpc.transport.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * 应答处理器
 */
public class ResponseChannelHandler implements ChannelHandler {

    protected static final Logger logger = LoggerFactory.getLogger(ResponseChannelHandler.class);

    @Override
    public Object received(final ChannelContext context, final Object message) {
        if (message instanceof Message && isResponseMsg((Message<?, ?>) message)) {
            complete(context, (Message<?, ?>) message, null);
        }
        return message;
    }

    @Override
    public void caught(final ChannelContext context, final Throwable throwable) {
        complete(context, null, throwable);
    }

    /**
     * 判定是否为响应类型消息
     *
     * @param message msg
     * @return boolean
     */
    protected boolean isResponseMsg(final Message<?, ?> message) {
        return !message.isRequest();
    }

    /**
     * 完成
     *
     * @param context   上下文
     * @param message   消息
     * @param throwable 异常
     */
    protected void complete(final ChannelContext context, final Message<?,?> message, final Throwable throwable) {
        FutureManager<Integer, Message> futureManager = context.getChannel().getFutureManager();
        if (futureManager != null) {
            CompletableFuture<Message> future = futureManager.remove(message.getMsgId());
            if (future != null) {
                if (throwable != null) {
                    future.completeExceptionally(throwable);
                } else {
                    future.complete(message);
                }
            } else {
                logger.warn(String.format("request is timeout. id=%d, type=%d, remote=%s",
                        message.getMsgId(),
                        message.getMsgType(),
                        Channel.toString(context.getChannel().getRemoteAddress())));
            }
        }
        context.end();
    }

}
