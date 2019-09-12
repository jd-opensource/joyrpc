package io.joyrpc.metric.mc;

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

import io.joyrpc.metric.TPMetric;
import io.joyrpc.metric.TPSnapshot;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能指标实现
 */
public class McTPMetric implements TPMetric {
    /**
     * 连续失败次数
     */
    protected AtomicLong successiveFailures;
    /**
     * 并发数
     */
    protected AtomicLong actives;
    /**
     * 分发的数量（请求数+当前分发待请求的数量）
     */
    protected AtomicLong distribution;
    /**
     * 是否熔断中
     */
    protected boolean broken;
    /**
     * 快照
     */
    protected McTPSnapshot snapshot;

    /**
     * 构造函数
     *
     * @param successiveFailures
     * @param actives
     * @param distribution
     * @param broken
     * @param snapshot
     */
    public McTPMetric(final AtomicLong successiveFailures, final AtomicLong actives,
                      final AtomicLong distribution, final boolean broken, final McTPSnapshot snapshot) {
        this.successiveFailures = successiveFailures;
        this.actives = actives;
        this.distribution = distribution;
        this.broken = broken;
        this.snapshot = snapshot;
    }

    @Override
    public long getSuccessiveFailures() {
        return successiveFailures.get();
    }

    @Override
    public long getActives() {
        return actives.get();
    }

    @Override
    public long getDistribution() {
        return distribution.get();
    }

    @Override
    public boolean isBroken() {
        return broken;
    }

    @Override
    public TPSnapshot getSnapshot() {
        return snapshot;
    }

    @Override
    public String toString() {
        StringBuilder sbuilder = new StringBuilder();
        sbuilder.append("successiveFailures::").append(this.successiveFailures.longValue()).append("_")
                .append("actives::").append(this.actives.longValue()).append("_")
                .append("distribution::").append(this.distribution.longValue()).append("_")
                .append(this.snapshot.toString());
        return sbuilder.toString();
    }

}
