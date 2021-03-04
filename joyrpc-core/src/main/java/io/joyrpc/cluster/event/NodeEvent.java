package io.joyrpc.cluster.event;

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

import io.joyrpc.cluster.Node;
import io.joyrpc.event.Event;

/**
 * 节点事件
 */
public class NodeEvent implements Event {

    //事件类型
    protected EventType type;
    //节点
    protected Node node;
    //心跳的数据
    protected Object payload;

    public NodeEvent(EventType type, Node node) {
        this(type, node, null);
    }

    public NodeEvent(EventType type, Node node, Object payload) {
        this.type = type;
        this.node = node;
        this.payload = payload;
    }

    public EventType getType() {
        return type;
    }

    public Node getNode() {
        return node;
    }

    public <T> T getPayload() {
        return (T) payload;
    }

    /**
     * 时间类型
     */
    public enum EventType {
        /**
         * 连接事件
         */
        CONNECT(0, "connect"),
        /**
         * 连接断开事件
         */
        DISCONNECT(1, "disconnect"),
        /**
         * 心跳事件
         */
        HEARTBEAT(3, "heartbeat"),
        /**
         * 正准备优雅关闭连接
         */
        CLOSING(4, "closing"),
        /**
         * 服务端下线事件
         */
        OFFLINE(5, "offline"),
        /**
         * 重连
         */
        RECONNECT(6, "reconnect");

        private int type;
        private String desc;

        EventType(int type, String desc) {
            this.type = type;
            this.desc = desc;
        }

        public int value() {
            return type;
        }

        public String getDesc() {
            return desc;
        }
    }


}
