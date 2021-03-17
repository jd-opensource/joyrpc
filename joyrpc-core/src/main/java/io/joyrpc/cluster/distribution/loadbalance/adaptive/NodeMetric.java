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
import io.joyrpc.cluster.Weighter;
import io.joyrpc.apm.metric.Dashboard;
import io.joyrpc.apm.metric.TPMetric;
import io.joyrpc.apm.metric.TPSnapshot;
import io.joyrpc.apm.metric.TPWindow;
import io.joyrpc.util.MilliPeriod;

import java.util.function.Function;

/**
 * 服务指标
 */
public class NodeMetric implements Weighter {

    public static final Function<TPSnapshot, Integer> TP50_FUNCTION = TPSnapshot::getTp50;
    public static final Function<TPSnapshot, Integer> TP90_FUNCTION = TPSnapshot::getTp90;

    /**
     * 节点
     */
    protected Node node;
    /**
     * 集群
     */
    protected Cluster cluster;
    /**
     * 节点指标窗口
     */
    protected TPWindow nodeWindow;
    /**
     * 节点指标快照
     */
    protected TPMetric nodeSnapshot;
    /**
     * 虚弱期
     */
    protected MilliPeriod weakPeriod;
    /**
     * 集群指标
     */
    protected TPWindow clusterWindow;
    /**
     * 集群指标快照
     */
    protected TPMetric clusterSnapshot;
    /**
     * 节点TP函数
     */
    protected Function<TPSnapshot, Integer> nodeFunction;
    /**
     * 服务权重
     */
    protected int weight;
    /**
     * 是否在虚弱期
     */
    protected boolean weak;

    /**
     * 构造函数
     *
     * @param node
     * @param cluster
     */
    public NodeMetric(final Node node, final Cluster cluster) {
        this(node, cluster, null, null);
    }

    /**
     * 构造函数
     *
     * @param node
     * @param cluster
     * @param function
     * @param nodeFunction
     */
    public NodeMetric(final Node node, final Cluster cluster,
                      final Function<Dashboard, TPWindow> function,
                      final Function<TPSnapshot, Integer> nodeFunction) {
        this(node, cluster,
                function == null ? node.getDashboard().getMetric() : function.apply(node.getDashboard()),
                function == null ? cluster.getDashboard().getMetric() : function.apply(cluster.getDashboard()),
                nodeFunction == null ? TP90_FUNCTION : nodeFunction);
    }

    /**
     * 构造函数
     *
     * @param node
     * @param cluster
     * @param nodeWindow
     * @param clusterWindow
     * @param nodeFunction
     */
    public NodeMetric(final Node node, final Cluster cluster,
                      final TPWindow nodeWindow,
                      final TPWindow clusterWindow,
                      final Function<TPSnapshot, Integer> nodeFunction) {
        this.node = node;
        this.cluster = cluster;
        this.nodeWindow = nodeWindow;
        this.nodeSnapshot = nodeWindow.getSnapshot();
        this.weakPeriod = nodeWindow.getWeakPeriod();
        this.clusterWindow = clusterWindow;
        this.clusterSnapshot = clusterWindow.getSnapshot();
        this.weight = node.getWeight();
        this.nodeFunction = nodeFunction;
        this.weak = weakPeriod != null && weakPeriod.between();
        this.weight = !weak ? node.getWeight() : (int) (node.getWeight() * weakPeriod.ratio());
    }

    public Node getNode() {
        return node;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public TPWindow getNodeWindow() {
        return nodeWindow;
    }

    public TPMetric getNodeSnapshot() {
        return nodeSnapshot;
    }

    public MilliPeriod getWeakPeriod() {
        return weakPeriod;
    }

    public TPWindow getClusterWindow() {
        return clusterWindow;
    }

    public TPMetric getClusterSnapshot() {
        return clusterSnapshot;
    }

    public Function<TPSnapshot, Integer> getNodeFunction() {
        return nodeFunction;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    /**
     * 判断是否有请求
     *
     * @return
     */
    public boolean hasRequest() {
        return nodeWindow == null ? true : nodeWindow.hasRequest();
    }

    /**
     * 是否熔断
     *
     * @return
     */
    public boolean isBroken() {
        MilliPeriod period = nodeWindow == null ? null : nodeWindow.getBrokenPeriod();
        return period == null ? false : period.between();
    }

    /**
     * 是否在虚弱期
     *
     * @return
     */
    public boolean isWeak() {
        return weak;
    }

    /**
     * 进入虚弱阶段，在指定时间短里面平滑恢复权重
     *
     * @param duration 虚弱时间
     * @return
     */
    public void weak(final long duration) {
        nodeWindow.weak(weakPeriod, duration);
    }

    /**
     * 是否有指标
     *
     * @return
     */
    public boolean noMetric() {
        return nodeWindow == null || clusterWindow == null;
    }

    /**
     * 派发流量
     */
    public void distribution() {
        if (nodeWindow != null) {
            nodeWindow.distribution().incrementAndGet();
        }
        if (clusterWindow != null) {
            clusterWindow.distribution().incrementAndGet();
        }
    }
}
