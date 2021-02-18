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
 * 通道写入器
 */
public interface ChannelWriter extends ChannelHandler {

    /**
     * 写消息
     *
     * @param context 上下文
     * @param message 消息
     * @return 转换的消息
     * @throws Exception
     */
    default void wrote(final ChannelContext context, final Object message) throws Exception {
        context.write(message);
    }

}
