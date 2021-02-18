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
 * 连接通道读取器
 */
public interface ChannelReader extends ChannelHandler {

    /**
     * 连接
     *
     * @param context 上下文
     * @throws Exception
     */
    default void active(final ChannelContext context) throws Exception {
    }

    /**
     * 断链
     *
     * @param context 上下文
     * @throws Exception
     */
    default void inactive(final ChannelContext context) throws Exception {
    }

    /**
     * 接收消息
     *
     * @param context 上下文
     * @param message 消息
     * @throws Exception
     */
    default void received(final ChannelContext context, final Object message) throws Exception {

    }

}
