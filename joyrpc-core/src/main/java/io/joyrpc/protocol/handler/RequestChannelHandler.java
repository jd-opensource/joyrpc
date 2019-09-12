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

import io.joyrpc.exception.LafException;
import io.joyrpc.exception.RpcException;
import io.joyrpc.extension.ExtensionPoint;
import io.joyrpc.extension.ExtensionSelector;
import io.joyrpc.transport.MessageHandler;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelContext;
import io.joyrpc.transport.channel.ChannelHandler;
import io.joyrpc.transport.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * 请求消息处理器
 *
 * @date: 2019/1/14
 */
public class RequestChannelHandler<T extends MessageHandler> implements ChannelHandler {

    protected final static Logger logger = LoggerFactory.getLogger(RequestChannelHandler.class);

    /**
     * 插件选择器，采用选择器是为了可以采用数组结构来存储插件，优化性能
     */
    protected ExtensionSelector<T, Integer, Integer, T> selector;
    /**
     * 异常消费者
     */
    protected BiConsumer<ChannelContext, Throwable> throwableConsumer;

    /**
     * 构造函数
     *
     * @param handlers
     * @param throwableConsumer
     */
    public RequestChannelHandler(final ExtensionPoint<T, Integer> handlers,
                                 final BiConsumer<ChannelContext, Throwable> throwableConsumer) {
        this.selector = new ExtensionSelector<>(handlers, (extensions, condition) -> extensions.get(condition));
        this.throwableConsumer = throwableConsumer;
    }

    /**
     * 构造函数
     *
     * @param selector
     * @param throwableConsumer
     */
    public RequestChannelHandler(final ExtensionSelector<T, Integer, Integer, T> selector,
                                 final BiConsumer<ChannelContext, Throwable> throwableConsumer) {
        Objects.requireNonNull(selector);
        this.selector = selector;
        this.throwableConsumer = throwableConsumer;
    }

    @Override
    public Object received(final ChannelContext context, final Object message) {
        if (message != null && message instanceof Message) {
            Message msg = (Message) message;
            if (msg.isRequest()) {
                //请求消息，调用对应处理器
                T handler = selector.select(msg.getMsgType());
                if (handler != null) {
                    try {
                        handler.handle(context, msg);
                        return null;
                    } catch (LafException e) {
                        caught(context, e);
                        return null;
                    } catch (Throwable e) {
                        caught(context, new RpcException(msg.getHeader(), e));
                        return null;
                    }
                } else {
                    caught(context, new RpcException(
                            String.format("there is not any handler for %d from %s.", msg.getMsgType(),
                                    Channel.toString(context.getChannel()))));
                    return null;
                }
            }
        }
        return message;
    }

    @Override
    public void caught(final ChannelContext context, final Throwable cause) {
        if (throwableConsumer != null) {
            throwableConsumer.accept(context, cause);
        } else {
            logger.error(String.format("Catch %s at %s : %s", cause.getClass().getName(),
                    Channel.toString(context.getChannel()), cause.getMessage()));
        }
    }
}
