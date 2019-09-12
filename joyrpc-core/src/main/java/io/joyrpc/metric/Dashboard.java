package io.joyrpc.metric;

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

import io.joyrpc.cluster.event.MetricEvent;
import io.joyrpc.event.EventHandler;

/**
 * 仪表盘，处理指标事件，返回当前指标
 */
public interface Dashboard extends EventHandler<MetricEvent> {

    /**
     * 获取指标
     *
     * @return
     */
    TPWindow getMetric();

    /**
     * 快照
     */
    default void snapshot() {
        getMetric().snapshot();
    }

    /**
     * 数据是否过期
     *
     * @return
     */
    default boolean isExpired() {
        return getMetric().isExpired();
    }

    /**
     * 设置上次快照时间
     *
     * @param lastSnapshotTime
     */
    default void setLastSnapshotTime(final long lastSnapshotTime) {
        getMetric().setLastSnapshotTime(lastSnapshotTime);
    }

    /**
     * 获取方法的性能指标
     *
     * @param methodName
     * @return
     */
    TPWindow getMethod(String methodName);

    /**
     * 面板类型
     */
    enum DashboardType {
        /**
         * 集群
         */
        Cluster,
        /**
         * 节点
         */
        Node
    }
}
