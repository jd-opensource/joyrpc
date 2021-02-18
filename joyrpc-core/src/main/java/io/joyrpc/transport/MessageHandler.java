package io.joyrpc.transport;

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

import io.joyrpc.exception.HandlerException;
import io.joyrpc.extension.Type;
import io.joyrpc.transport.channel.ChannelContext;
import io.joyrpc.transport.message.Message;

/**
 * 消息命令
 *
 * @param <T>
 */
public interface MessageHandler<T extends Message> extends Type<Integer> {

    /**
     * 处理具体的消息命令
     *
     * @param context 上下文
     * @param message 消息
     * @throws HandlerException
     */
    void handle(final ChannelContext context, final T message) throws HandlerException;
}
