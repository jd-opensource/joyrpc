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


import io.joyrpc.InvokerAware;
import io.joyrpc.cluster.Candidate;
import io.joyrpc.cluster.Cluster;
import io.joyrpc.cluster.Node;
import io.joyrpc.cluster.distribution.LoadBalance;
import io.joyrpc.config.InterfaceOption.ConsumerMethodOption;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.URL;
import io.joyrpc.metric.*;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.joyrpc.constants.Constants.ADAPTIVE_CLUSTER_TP;
import static io.joyrpc.constants.Constants.ADAPTIVE_NODE_TP;

/**
 * 自适应负载均衡
 */
@Extension(value = "adaptive")
public class AdaptiveLoadBalance implements LoadBalance, InvokerAware, DashboardAware, AdaptiveScorer {

    public static final Function<TPSnapshot, Integer> TP30_FUNCTION = TPSnapshot::getTp30;
    public static final Function<TPSnapshot, Integer> TP50_FUNCTION = TPSnapshot::getTp50;
    public static final Function<TPSnapshot, Integer> TP90_FUNCTION = TPSnapshot::getTp90;
    public static final Function<TPSnapshot, Integer> TP99_FUNCTION = TPSnapshot::getTp99;
    public static final Function<TPSnapshot, Integer> TP999_FUNCTION = TPSnapshot::getTp999;
    public static final Function<TPSnapshot, Integer> TPAVG_FUNCTION = TPSnapshot::getAvg;

    /**
     * URL
     */
    protected URL url;
    /**
     * 统计评分信息记录函数
     */
    protected Consumer<List<NodeRank>> recorder;
    /**
     * 集群TP函数
     */
    protected Function<TPSnapshot, Integer> clusterFunction;
    /**
     * 节点TP函数
     */
    protected Function<TPSnapshot, Integer> nodeFunction;

    /**
     * 接口
     */
    protected String className;


    @Override
    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public void setUrl(URL url) {
        this.url = url;
    }

    @Override
    public void setup() {
        clusterFunction = getTpFunction(url.getString(ADAPTIVE_CLUSTER_TP), TP30_FUNCTION);
        nodeFunction = getTpFunction(url.getString(ADAPTIVE_NODE_TP), TP90_FUNCTION);
    }

    protected Function<TPSnapshot, Integer> getTpFunction(final String type, final Function<TPSnapshot, Integer> def) {
        switch (type) {
            case "avg":
                return TPAVG_FUNCTION;
            case "tp50":
                return TP50_FUNCTION;
            case "tp90":
                return TP90_FUNCTION;
            case "tp99":
                return TP99_FUNCTION;
            case "tp999":
                return TP999_FUNCTION;
            default:
                return def;
        }
    }

    @Override
    public AdaptiveConfig score(final Cluster cluster, final String method, final AdaptiveConfig config) {
        AdaptiveConfig result = new AdaptiveConfig();
        List<Node> nodes = cluster.getNodes();
        int size = nodes.size();
        long[] actives = config.concurrencyScore == null ? new long[size] : null;
        long[] requests = config.qpsScore == null ? new long[size] : null;
        long[] availability = config.availabilityScore == null ? new long[size] : null;

        int i = 0;
        TPWindow window;
        TPMetric snapshot;
        //采样数量
        int max = 100;
        for (Node node : nodes) {
            window = node.getDashboard().getMethod(method);
            snapshot = window.getSnapshot();
            if (actives != null) {
                actives[i] = window.actives().get();
            }
            if (requests != null) {
                requests[i] = window.distribution().get() + snapshot.getSnapshot().getRequests();
            }
            if (availability != null) {
                //保留3位小数
                availability[i] = (long) (snapshot.getSnapshot().getAvailability() * 1000);
            }
            if (++i > max) {
                //控制循环数量
                break;
            }
        }
        if (requests != null) {
            result.setQpsScore(AdaptiveConfig.computeQpsScore(requests));
        }
        if (actives != null) {
            result.setConcurrencyScore(AdaptiveConfig.computeConcurrencyScore(actives));
        }
        if (availability != null) {
            result.setAvailabilityScore(AdaptiveConfig.computeAvailabilityScore(actives));
        }
        if (config.tpScore == null) {
            result.setTpScore(AdaptiveConfig.computeTpScore(clusterFunction.apply(
                    cluster.getDashboard().getMethod(method).getSnapshot().getSnapshot())));
        }
        return result;
    }

    @Override
    public Node select(final Candidate candidate, final RequestMessage<Invocation> request) {
        //获取LB的服务列表
        List<Node> candidates = candidate.getNodes();
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        //得到指标获取函数，增对不同的场景可能是节点或方法的
        Function<Dashboard, TPWindow> metricFunction = apply(request);

        ConsumerMethodOption option = (ConsumerMethodOption) request.getOption();
        AdaptivePolicy policy = option.getAdaptivePolicy();
        ClusterRank clusterRank = new ClusterRank(candidate.getCluster(), policy, metricFunction, nodeFunction);
        int size = candidates.size();
        //抽样随机打散，避免每次都拿到固定的节点
        boolean sampling = clusterRank.enoughGoods > 0 && policy.getArbiter().sampling();
        if (!sampling) {
            //节点全选
            clusterRank.enoughGoods = 0;
            clusterRank.score(candidates);
        } else {
            int random = size > clusterRank.enoughGoods ? (int) (Math.random() * size) : -1;
            if (random > 0) {
                //判断是否有足够的最佳节点
                if (!clusterRank.score(candidates.subList(random, size))) {
                    clusterRank.score(candidates.subList(0, random));
                }
            } else {
                clusterRank.score(candidates);
            }
        }

        //如果需要，对评分信息进行记录
        if (recorder != null) {
            recorder.accept(clusterRank.ranks);
        }
        //选择最佳评分节点
        NodeRank rank = clusterRank.select();
        if (rank != null) {
            rank.distribution();
            return rank.getNode();
        }
        return null;
    }

    /**
     * 生成请求的指标，例如可以获取方法的指标
     *
     * @param request
     * @return
     */
    protected Function<Dashboard, TPWindow> apply(final RequestMessage<Invocation> request) {
        return o -> o.getMethod(request.getPayLoad().getMethodName());
    }

    /**
     * 集群评分
     */
    protected static class ClusterRank {
        //集群
        protected Cluster cluster;
        //所有服务计算的统计得分,用于向外输出
        protected List<NodeRank> ranks = new LinkedList<>();
        //最高级别评分
        protected LinkedList<NodeRank> bestRanks = new LinkedList<>();
        //上一次的服务评分
        protected NodeRank last = null;
        //最高评分
        protected Rank best = Rank.Disabled;
        //上下文
        protected AdaptivePolicy policy;
        //足够的最佳候选人数量
        protected int enoughGoods;
        //指标请求
        protected Function<Dashboard, TPWindow> metricFunction;
        /**
         * 节点TP函数
         */
        protected Function<TPSnapshot, Integer> nodeFunction;

        /**
         * 构造函数
         *
         * @param cluster        集群
         * @param policy         自适应策略
         * @param metricFunction 窗口函数
         * @param nodeFunction   节点指标函数
         */
        public ClusterRank(final Cluster cluster, final AdaptivePolicy policy,
                           final Function<Dashboard, TPWindow> metricFunction,
                           final Function<TPSnapshot, Integer> nodeFunction) {
            this.cluster = cluster;
            this.policy = policy;
            this.metricFunction = metricFunction;
            this.nodeFunction = nodeFunction;
            this.enoughGoods = policy.getEnoughGoods() == null ? 0 : policy.getEnoughGoods();
        }

        /**
         * 对候选者进行评分
         *
         * @param candidates 节点
         * @return 是否有足够的候选者
         */
        public boolean score(final List<Node> candidates) {
            //遍历服务列表进行评分
            String dc;
            for (Node node : candidates) {
                dc = node.getDataCenter();
                if ((policy.exclusionRooms == null || dc == null || !policy.exclusionRooms.contains(dc)) && score(node)) {
                    //有足够的候选者
                    return true;
                }
            }
            return false;
        }

        /**
         * 对单个服务实例进行评分
         *
         * @param node 节点
         * @return 是否有足够的候选者了
         */
        protected boolean score(final Node node) {
            //评分
            NodeRank rank = new NodeRank(node, cluster, metricFunction, nodeFunction).score(policy);
            ranks.add(rank);
            //评分比较
            int result = last == null ? -1 : rank.getRank().compareTo(best);
            if (result == 0) {
                //优先级相同
                bestRanks.add(rank);
            } else if (result < 0) {
                //优先级更高
                if (!bestRanks.isEmpty()) {
                    bestRanks.clear();
                }
                bestRanks.add(rank);
                best = rank.getRank();
            }
            //判断是否有足够多的优秀候选者
            if (result <= 0 && best == Rank.Good && enoughGoods > 0 && bestRanks.size() >= enoughGoods) {
                return true;
            }
            last = rank;
            return false;
        }

        /**
         * 根据打分结果进行选择
         *
         * @return 选择节点
         */
        public NodeRank select() {
            switch (bestRanks.size()) {
                case 0:
                    return null;
                case 1:
                    return bestRanks.getFirst();
                default:
                    return policy.election.choose(bestRanks, policy);
            }
        }

    }

}
