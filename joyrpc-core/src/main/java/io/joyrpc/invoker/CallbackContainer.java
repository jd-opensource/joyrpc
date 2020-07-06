package io.joyrpc.invoker;

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

import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.transport.transport.ChannelTransport;

import java.io.Closeable;
import java.util.List;

/**
 * 回调容器
 */
public interface CallbackContainer extends Closeable {

    /**
     * 消费者回调请求
     *
     * @param request   请求
     * @param transport 通道
     */
    void addCallback(RequestMessage<Invocation> request, final ChannelTransport transport);

    /**
     * 移除Channel上的回调
     *
     * @param transport 通道
     * @return 回调集合
     */
    List<CallbackInvoker> removeCallback(ChannelTransport transport);

    /**
     * 移除回调
     *
     * @param callbackId 回调ID
     * @return 回调对象
     */
    CallbackInvoker removeCallback(String callbackId);

    /**
     * 获取调用
     *
     * @param callbackId 回调ID
     * @return 回调对象
     */
    CallbackInvoker getInvoker(String callbackId);

    /**
     * 清除
     */
    void close();
}
