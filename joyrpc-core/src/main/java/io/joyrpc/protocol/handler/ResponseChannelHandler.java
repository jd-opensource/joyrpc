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

import io.joyrpc.exception.RpcException;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelContext;
import io.joyrpc.transport.channel.ChannelHandler;
import io.joyrpc.transport.channel.FutureManager;
import io.joyrpc.transport.message.Header;
import io.joyrpc.transport.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 应答处理器
 */
public class ResponseChannelHandler implements ChannelHandler {

    protected static final Logger logger = LoggerFactory.getLogger(ResponseChannelHandler.class);

    @Override
    public Object received(final ChannelContext context, final Object message) {
        if (message instanceof Message && isResponseMsg((Message<?, ?>) message)) {
            complete(context, (Message<?, ?>) message);
        }
        return message;
    }

    @Override
    public void caught(final ChannelContext context, final Throwable throwable) {
        completeExceptionally(context, throwable);
    }

    /**
     * 判定是否为响应类型消息
     *
     * @param message message 消息
     * @return 应答消息标识
     */
    protected boolean isResponseMsg(final Message<?, ?> message) {
        return !message.isRequest();
    }

    /**
     * 完成
     *
     * @param context 上下文
     * @param message 消息
     */
    protected void complete(final ChannelContext context, final Message<?, ?> message) {
        FutureManager<Long, Message> futureManager = context.getChannel().getFutureManager();
        if (futureManager != null) {
            if (!futureManager.complete(message.getMsgId(), message)) {
                logger.warn(String.format("request is timeout. id=%d, type=%d, remote=%s",
                        message.getMsgId(),
                        message.getMsgType(),
                        Channel.toString(context.getChannel().getRemoteAddress())));
            }
        }
        context.end();
    }

    /**
     * 完成
     *
     * @param context   上下文
     * @param throwable 异常
     */
    protected void completeExceptionally(final ChannelContext context, final Throwable throwable) {
        if (throwable instanceof RpcException) {
            Header header = ((RpcException) throwable).getHeader();
            if (header != null) {
                FutureManager<Long, Message> futureManager = context.getChannel().getFutureManager();
                if (futureManager != null) {
                    if (!futureManager.completeExceptionally(header.getMsgId(), throwable)) {
                        logger.warn(String.format("request is timeout. id=%d, type=%d, remote=%s",
                                header.getMsgId(),
                                header.getMsgType(),
                                Channel.toString(context.getChannel().getRemoteAddress())));
                    }
                }
            }
        }
        context.end();
    }

}
