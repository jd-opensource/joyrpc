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

import io.joyrpc.cluster.Shard;
import io.joyrpc.event.UpdateEvent;

import java.util.List;

/**
 * 集群事件
 */
public class ClusterEvent extends UpdateEvent<List<ClusterEvent.ShardEvent>> {

    public ClusterEvent(final Object source, final Object target, final UpdateType type,
                        final long version, final List<ShardEvent> shards) {
        super(source, target, type, version, shards);
    }

    /**
     * 分片事件
     */
    public static class ShardEvent {
        /**
         * 分片信息
         */
        protected Shard shard;

        /**
         * 事件类型
         */
        protected ShardEventType type;

        public ShardEvent(Shard shard, ShardEventType type) {
            this.shard = shard;
            this.type = type;
        }

        public Shard getShard() {
            return shard;
        }

        public ShardEventType getType() {
            return type;
        }
    }

    /**
     * 分片事件类型
     */
    public enum ShardEventType {
        /**
         * Add event type.
         */
        ADD(1, "服务添加"),
        /**
         * Delete event type.
         */
        DELETE(2, "服务删除"),
        /**
         * Update event type.
         */
        UPDATE(3, "服务更新");


        private int type;
        private String desc;

        ShardEventType(int type, String desc) {
            this.type = type;
            this.desc = desc;
        }

        /**
         * 获取值.
         *
         * @return the int
         */
        public int value() {
            return type;
        }

        /**
         * 获取描述
         *
         * @return
         */
        public String getDesc() {
            return desc;
        }
    }

}
