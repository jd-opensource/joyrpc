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
import io.joyrpc.transport.channel.ChannelReader;
import io.joyrpc.transport.channel.FutureManager;
import io.joyrpc.transport.message.Header;
import io.joyrpc.transport.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 读取数据通道中的应答消息，并调用相应的处理器进行处理
 */
public class ResponseReceiver implements ChannelReader {

    protected static final Logger logger = LoggerFactory.getLogger(ResponseReceiver.class);

    @Override
    public void received(final ChannelContext context, final Object message) throws Exception {
        if (isResponse(message)) {
            Message<?, ?> msg = (Message<?, ?>) message;
            FutureManager<Long, Message> futureManager = context.getChannel().getFutureManager();
            if (futureManager != null && !futureManager.complete(msg.getMsgId(), msg)) {
                logger.warn(String.format("request is timeout. id=%d, type=%d, remote=%s",
                        msg.getMsgId(),
                        msg.getMsgType(),
                        Channel.toString(context.getChannel().getRemoteAddress())));
            }
        } else {
            context.fireChannelRead(message);
        }
    }

    @Override
    public void caught(final ChannelContext context, final Throwable throwable) {
        if (throwable instanceof RpcException) {
            Header header = ((RpcException) throwable).getHeader();
            if (header != null) {
                FutureManager<Long, Message> futureManager = context.getChannel().getFutureManager();
                if (futureManager != null && !futureManager.completeExceptionally(header.getMsgId(), throwable)) {
                    logger.warn(String.format("request is timeout. id=%d, type=%d, remote=%s",
                            header.getMsgId(),
                            header.getMsgType(),
                            Channel.toString(context.getChannel().getRemoteAddress())));
                }
            }
        }

    }

    /**
     * 判定是否为响应类型消息
     *
     * @param message 消息
     * @return 应答消息标识
     */
    protected boolean isResponse(final Object message) {
        return message instanceof Message && !((Message) message).isRequest();
    }

}
