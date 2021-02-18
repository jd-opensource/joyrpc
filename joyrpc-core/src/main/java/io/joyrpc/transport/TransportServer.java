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


import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.codec.ProtocolDeduction;

import java.util.List;
import java.util.function.Consumer;

/**
 * 服务端传输通道
 */
public interface TransportServer extends Transport, Endpoint<Void> {

    /**
     * 获取所有的传输通道
     *
     * @return 传输通道集合
     */
    List<ChannelTransport> getTransports();

    /**
     * 返回所有的连接通道
     *
     * @return 连接通道集合
     */
    List<Channel> getChannels();

    /**
     * 遍历Transport
     *
     * @param consumer 消费者
     */
    default void forEach(final Consumer<ChannelTransport> consumer) {
        if (consumer != null) {
            List<ChannelTransport> transports = getTransports();
            if (transports != null) {
                transports.forEach(consumer);
            }
        }
    }

    /**
     * 设置协议推断
     *
     * @param deduction 推断
     */
    void setDeduction(ProtocolDeduction deduction);

}
