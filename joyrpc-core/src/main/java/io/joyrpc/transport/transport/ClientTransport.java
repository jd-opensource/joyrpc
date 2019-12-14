package io.joyrpc.transport.transport;

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


import io.joyrpc.event.Publisher;
import io.joyrpc.protocol.ClientProtocol;
import io.joyrpc.transport.Endpoint;
import io.joyrpc.transport.event.TransportEvent;
import io.joyrpc.transport.heartbeat.HeartbeatStrategy;

/**
 * 客户端通道
 */
public interface ClientTransport extends ChannelTransport, Endpoint {

    /**
     * 设置心跳策略
     *
     * @param heartbeatStrategy
     */
    void setHeartbeatStrategy(HeartbeatStrategy heartbeatStrategy);

    /**
     * 获取心跳策略
     *
     * @return
     */
    HeartbeatStrategy getHeartbeatStrategy();

    /**
     * 获取名称
     *
     * @return
     */
    String getChannelName();

    /**
     * 获取通道事件发布器
     *
     * @return
     */
    Publisher<TransportEvent> getPublisher();

    /**
     * 设置 clientTransport 的协议
     *
     * @param protocol
     */
    void setProtocol(ClientProtocol protocol);

    /**
     * 获取 clientTransport 的协议
     *
     * @return
     */
    ClientProtocol getProtocol();

    /**
     * 获取正在处理的请求数，包括正在发送和等待应答的请求
     *
     * @return
     */
    int getRequests();

}
