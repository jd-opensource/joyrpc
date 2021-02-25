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


import io.joyrpc.event.Publisher;
import io.joyrpc.protocol.ClientProtocol;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.event.TransportEvent;
import io.joyrpc.transport.heartbeat.HeartbeatStrategy;

/**
 * 传输通道客户端
 */
//TODO Endpoint的泛型是否要改成TransportClient
public interface TransportClient extends ChannelTransport, Endpoint<Channel> {

    /**
     * 设置心跳策略
     *
     * @param heartbeatStrategy 心跳策略
     */
    void setHeartbeatStrategy(HeartbeatStrategy heartbeatStrategy);

    /**
     * 获取心跳策略
     *
     * @return 心跳策略
     */
    HeartbeatStrategy getHeartbeatStrategy();

    /**
     * 获取名称
     *
     * @return 名称
     */
    String getName();

    /**
     * 获取通道事件发布器
     *
     * @return 事件发布器
     */
    Publisher<TransportEvent> getPublisher();

    /**
     * 设置客户端协议
     *
     * @param protocol 客户端协议
     */
    void setProtocol(ClientProtocol protocol);

    /**
     * 获取客户端的协议
     *
     * @return 客户端协议
     */
    ClientProtocol getProtocol();

    /**
     * 获取正在处理的请求数，包括正在发送和等待应答的请求
     *
     * @return 正在处理的请求数
     */
    int getRequests();

}
