package io.joyrpc.protocol;

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

import io.joyrpc.extension.Extensible;
import io.joyrpc.extension.URL;
import io.joyrpc.transport.Client;
import io.joyrpc.transport.message.Message;
import io.joyrpc.transport.session.DefaultSession;
import io.joyrpc.transport.session.Session;


/**
 * 客户端协议
 */
@Extensible("clientProtocol")
public interface ClientProtocol extends Protocol {

    /**
     * 创建协商消息
     *
     * @param clusterUrl 集群url
     * @param client     客户端
     * @return Message
     */
    Message negotiate(URL clusterUrl, Client client);

    /**
     * 会话心跳，保持会话连接
     *
     * @param clusterUrl 集群url
     * @param client     客户端
     * @return Message
     */
    Message sessionbeat(URL clusterUrl, Client client);

    /**
     * 构造身份认证消息
     *
     * @param clusterUrl 集群url
     * @param client     客户端
     * @return
     */
    default Message authenticate(URL clusterUrl, Client client) {
        return null;
    }

    /**
     * 构造session对象
     *
     * @param clusterUrl 集群url
     * @param client     客户端
     * @return Session
     */
    default Session session(final URL clusterUrl, Client client) {
        return new DefaultSession();
    }

    /**
     * 创建心跳消息，保持物理连接
     *
     * @param clusterUrl 集群url
     * @param client     客户端
     * @return Message
     */
    Message heartbeat(URL clusterUrl, Client client);

}
