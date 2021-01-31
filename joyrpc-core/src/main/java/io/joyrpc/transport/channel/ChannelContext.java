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
 * 连接通道上下文
 */
public interface ChannelContext {

    /**
     * 获取连接通道
     *
     * @return 连接通道
     */
    Channel getChannel();

    /**
     * 终止处理链执行
     */
    void end();

    /**
     * 判断是否已终止
     *
     * @return 已终止标识
     */
    boolean isEnd();
}
