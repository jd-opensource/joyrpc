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

/**
 * @date: 2019/1/7
 */
public interface ChannelHandler {

    /**
     * 连接
     *
     * @param context
     */
    default void active(final ChannelContext context) {
    }

    /**
     * 断链
     *
     * @param context
     */
    default void inactive(final ChannelContext context) {
    }

    /**
     * 接收消息
     *
     * @param context
     * @param message
     * @return
     */
    default Object received(final ChannelContext context, final Object message) {
        return message;
    }

    /**
     * 写消息
     *
     * @param context
     * @param message
     * @return
     */
    default Object wrote(final ChannelContext context, final Object message) {
        return message;
    }

    /**
     * 异常
     *
     * @param context
     * @param throwable
     */
    default void caught(final ChannelContext context, final Throwable throwable) {

    }
}
