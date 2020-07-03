package io.joyrpc.cluster.distribution.loadbalance.adaptive;


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

import io.joyrpc.cluster.Cluster;
import io.joyrpc.cluster.Node;
import io.joyrpc.metric.TPMetric;
import io.joyrpc.metric.TPSnapshot;
import io.joyrpc.metric.TPWindow;
import io.joyrpc.util.MilliPeriod;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import static io.joyrpc.cluster.distribution.loadbalance.adaptive.AdaptiveConfig.*;

/**
 * 自适应指标计算
 */
public class AdaptiveEvaluator {
    /**
     * 自适应配置
     */
    protected final AdaptiveConfig config;
    /**
     * TP函数
     */
    protected final Function<TPSnapshot, Integer> tpFunction;
    /**
     * 并发数
     */
    protected final Quota actives;
    /**
     * QPS
     */
    protected final Quota requests;
    /**
     * 可用率
     */
    protected final Quota availability;

    /**
     * 构造函数
     *
     * @param config     自适应配置
     * @param tpFunction TP函数
     */
    public AdaptiveEvaluator(AdaptiveConfig config, Function<TPSnapshot, Integer> tpFunction) {
        this.config = config;
        this.tpFunction = tpFunction;
        this.actives = config.concurrencyScore == null ? new Quota() : null;
        this.requests = config.qpsScore == null ? new Quota() : null;
        this.availability = config.availabilityScore == null ? new Quota() : null;
    }

    /**
     * 添加指标
     *
     * @param window 指标窗口
     */
    public void add(final TPWindow window) {
        if (window == null) {
            return;
        }
        MilliPeriod brokenPeriod = window.getBrokenPeriod();
        MilliPeriod weakPeriod = window.getWeakPeriod();
        if (brokenPeriod != null && brokenPeriod.between()) {
            //禁用
            addDisable(window, window.getSnapshot());
        } else if (weakPeriod != null && weakPeriod.between()) {
            //虚弱
            addPoor(window, window.getSnapshot());
        } else {
            //正常
            addGood(window, window.getSnapshot());
        }
    }

    /**
     * 正常指标
     *
     * @param window   窗口
     * @param snapshot 快照
     */
    protected void addGood(final TPWindow window, final TPMetric snapshot) {
        if (actives != null) {
            actives.addGood(window.actives().get());
        }
        if (requests != null) {
            requests.addGood(window.distribution().get() + snapshot.getSnapshot().getRequests());
        }
        if (availability != null) {
            //保留3位小数
            availability.addGood((long) (snapshot.getSnapshot().getAvailability() * 1000));
        }
    }

    /**
     * 虚弱指标
     *
     * @param window   指标窗口
     * @param snapshot 指标快照
     */
    protected void addPoor(final TPWindow window, final TPMetric snapshot) {
        if (actives != null) {
            actives.addPoor(window.actives().get());
        }
        if (requests != null) {
            requests.addPoor(window.distribution().get() + snapshot.getSnapshot().getRequests());
        }
        if (availability != null) {
            //保留3位小数
            availability.addPoor((long) (snapshot.getSnapshot().getAvailability() * 1000));
        }
    }

    /**
     * 禁用节点指标
     *
     * @param window   指标窗口
     * @param snapshot 指标快照
     */
    protected void addDisable(final TPWindow window, final TPMetric snapshot) {
        if (actives != null) {
            actives.addDisable(window.actives().get());
        }
        if (requests != null) {
            requests.addDisable(window.distribution().get() + snapshot.getSnapshot().getRequests());
        }
        if (availability != null) {
            //保留3位小数
            availability.addDisable((long) (snapshot.getSnapshot().getAvailability() * 1000));
        }
    }

    /**
     * 计算自适应评分
     *
     * @param cluster 集群
     * @param method  方法
     * @return 自适应评分
     */
    public AdaptiveConfig compute(final Cluster cluster, final String method) {
        //采样数量
        List<Node> nodes = cluster.getNodes();
        int size = Math.min(nodes.size(), 100);
        int i = 0;
        for (Node node : nodes) {
            add(node.getDashboard().getMethod(method));
            if (++i > size) {
                //控制循环数量
                break;
            }
        }
        AdaptiveConfig result = new AdaptiveConfig();
        if (requests != null) {
            result.setQpsScore(computeQpsScore(requests.compute()));
        }
        if (actives != null) {
            result.setConcurrencyScore(computeConcurrencyScore(actives.compute()));
        }
        if (availability != null) {
            result.setAvailabilityScore(computeAvailabilityScore(availability.compute()));
        }
        if (config.tpScore == null) {
            TPWindow window = cluster.getDashboard().getMethod(method);
            if (window != null) {
                result.setTpScore(computeTpScore(tpFunction.apply(window.getSnapshot().getSnapshot())));
            }
        }
        return result;
    }

    /**
     * 指标
     */
    protected static class Quota {
        /**
         * 正常节点数据
         */
        protected List<Long> good;
        /**
         * 虚弱节点数据
         */
        protected List<Long> poor;
        /**
         * 禁用节点数据
         */
        protected List<Long> disable;

        public Quota() {
            good = new LinkedList<>();
            poor = new LinkedList<>();
            disable = new LinkedList<>();
        }

        public void addGood(long value) {
            good.add(value);
        }

        public void addPoor(long value) {
            poor.add(value);
        }

        public void addDisable(long value) {
            disable.add(value);
        }

        /**
         * 计算用于评分的值
         *
         * @return 用于评分的值
         */
        public long[] compute() {
            if (!good.isEmpty()) {
                return toArray(good);
            } else if (!poor.isEmpty()) {
                return toArray(poor);
            } else {
                return toArray(disable);
            }
        }

        /**
         * 转换成数组
         *
         * @param datum 集合
         * @return 数组
         */
        protected long[] toArray(final List<Long> datum) {
            long[] result = new long[datum.size()];
            int i = 0;
            for (Long data : datum) {
                result[i++] = data;
            }
            return result;
        }
    }
}
