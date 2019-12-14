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
import io.joyrpc.constants.Constants;
import io.joyrpc.context.adaptive.AdaptiveConfiguration;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.URL;
import io.joyrpc.metric.*;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.joyrpc.Plugin.*;
import static io.joyrpc.util.StringUtils.SEMICOLON_COMMA_WHITESPACE;
import static io.joyrpc.util.StringUtils.split;

/**
 * 自适应负载均衡
 *
 * @param <T>
 */
@Extension(value = "adaptive")
public class AdaptiveLoadBalance<T> implements LoadBalance<T>, InvokerAware, DashboardAware {

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
     * 默认配置
     */
    protected AdaptiveConfig config;
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
        String[] rooms = split(url.getString(Constants.ADAPTIVE_EXCLUSION_ROOMS), SEMICOLON_COMMA_WHITESPACE);
        config = new AdaptiveConfig(
                url.getString(Constants.ADAPTIVE_ARBITER),
                url.getString(Constants.ADAPTIVE_ELECTION),
                url.getInteger(Constants.ADAPTIVE_ENOUGH_GOODS),
                computeConcurrencyScore(),
                computeQpsScore(),
                computeTpScore(),
                computeAvailabilityScore(),
                url.getLong(Constants.ADAPTIVE_DECUBATION),
                rooms == null ? null : new HashSet<>(Arrays.asList(rooms)), null);
        clusterFunction = getTpFunction(url.getString(Constants.ADAPTIVE_CLUSTER_TP), TP30_FUNCTION);
        nodeFunction = getTpFunction(url.getString(Constants.ADAPTIVE_NODE_TP), TP90_FUNCTION);
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

    /**
     * 构建配置
     *
     * @param request
     * @return
     */
    protected AdaptiveConfig build(final T request) {
        //从自适应负载均衡配置器获取配置
        AdaptiveConfig config = AdaptiveConfiguration.ADAPTIVE.get(className);
        return config == null ? this.config : config;
    }

    /**
     * 计算集群服务实例中位数的QPS及并发数量
     *
     * @param config
     * @param candidate
     * @param metricFunction
     */
    protected AdaptiveConfig compute(final AdaptiveConfig config, final Candidate candidate, final Function<Dashboard, TPWindow> metricFunction) {
        //如果用户没有设置QPS或并发阈值，都进行中位数计算；
        if (config.qpsScore != null && config.concurrencyScore != null && config.availabilityScore != null && config.tpScore != null) {
            return config;
        }
        AdaptiveConfig result = config.clone();
        List<Node> nodes = candidate.getNodes();
        int size = nodes.size();
        long[] actives = config.concurrencyScore == null ? new long[size] : null;
        long[] requests = config.qpsScore == null ? new long[size] : null;
        long[] availability = config.availabilityScore == null ? new long[size] : null;

        int i = 0;
        TPWindow window;
        TPMetric snapshot;
        for (Node node : nodes) {
            window = metricFunction.apply(node.getDashboard());
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
            i++;
        }
        if (requests != null) {
            computeQpsScore(result, requests);
        }
        if (actives != null) {
            computeConcurrencyScore(result, actives);
        }
        if (availability != null) {
            computeAvailabilityScore(result, availability);
        }
        if (result.tpScore == null) {
            result.setTpScore(computeTpScore(clusterFunction.apply(
                    metricFunction.apply(candidate.getCluster().getDashboard())
                            .getSnapshot().getSnapshot())));
        }
        return result;
    }

    /**
     * 计算TP评分
     *
     * @param config
     * @param tps
     */
    protected void computeTpScore(final AdaptiveConfig config, final long[] tps) {
        config.setTpScore(computeTpScore((int) (median(tps) / 1000)));
    }

    /**
     * 计算TP评分
     *
     * @param fair
     */
    protected RankScore<Integer> computeTpScore(final int fair) {
        switch (fair) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
                return new RankScore<>(4, 8, 12);
            case 5:
            case 6:
            case 7:
            case 8:
                return new RankScore<>(8, 12, 16);
            default:
                return new RankScore<>((int) (fair * 1.2), (int) (fair * 1.5), fair * 2);
        }
    }

    /**
     * 计算TP评分
     */
    protected RankScore<Integer> computeTpScore() {
        Integer fair = url.getInteger(Constants.ADAPTIVE_TP_FAIR);
        Integer poor = url.getInteger(Constants.ADAPTIVE_TP_POOR);
        Integer disable = url.getInteger(Constants.ADAPTIVE_TP_DISABLE);
        if (fair == null && poor == null && disable == null) {
            return null;
        } else if (fair != null && poor == null && disable == null) {
            return computeTpScore(fair);
        } else {
            return new RankScore<>(fair, poor, disable);
        }
    }

    /**
     * 计算可用率评分
     *
     * @param config
     * @param availability
     */
    protected void computeAvailabilityScore(final AdaptiveConfig config, final long[] availability) {
        config.setAvailabilityScore(computeAvailabilityScore(median(availability) / 1000));
    }

    /**
     * 计算可用率评分
     *
     * @param fair
     */
    protected RankScore<Double> computeAvailabilityScore(final double fair) {
        return new RankScore<>(fair - 0.1, fair - 1d, fair - 5d);
    }

    /**
     * 计算可用率评分
     */
    protected RankScore<Double> computeAvailabilityScore() {
        Double fair = url.getDouble(Constants.ADAPTIVE_AVAILABILITY_FAIR);
        Double poor = url.getDouble(Constants.ADAPTIVE_AVAILABILITY_POOR);
        Double disable = url.getDouble(Constants.ADAPTIVE_AVAILABILITY_DISABLE);
        if (fair == null && poor == null && disable == null) {
            return null;
        } else if (fair != null && poor == null && disable == null) {
            return computeAvailabilityScore(fair);
        } else {
            return new RankScore<>(fair, poor, disable);
        }
    }

    /**
     * 计算并发数评分
     *
     * @param config
     * @param actives
     */
    protected void computeConcurrencyScore(final AdaptiveConfig config, final long[] actives) {
        config.setConcurrencyScore(computeConcurrencyScore(median(actives)));
    }

    /**
     * 计算并发评分
     *
     * @param fair
     */
    protected RankScore<Long> computeConcurrencyScore(final long fair) {
        if (fair <= 0) {
            return new RankScore<>(100L, null, null);
        }
        return new RankScore<>(fair, fair * 2, null);
    }

    /**
     * 计算并发评分
     */
    protected RankScore<Long> computeConcurrencyScore() {
        Long fair = url.getLong(Constants.ADAPTIVE_CONCURRENCY_FAIR);
        Long poor = url.getLong(Constants.ADAPTIVE_CONCURRENCY_POOR);
        if (fair == null && poor == null) {
            return null;
        } else if (fair != null && poor == null) {
            return computeConcurrencyScore(fair);
        } else {
            return new RankScore<>(fair, poor, null);
        }
    }

    /**
     * 计算Qps评分
     *
     * @param config
     * @param requests
     */
    protected void computeQpsScore(final AdaptiveConfig config, final long[] requests) {
        //中位数
        config.setQpsScore(computeQpsScore(median(requests)));
    }

    /**
     * 计算Qps评分
     *
     * @param fair
     */
    protected RankScore<Long> computeQpsScore(final long fair) {
        if (fair <= 0) {
            return new RankScore<>(1000L, null, null);
        }
        return new RankScore<>(fair, fair * 2, null);
    }

    /**
     * 计算并发评分
     */
    protected RankScore<Long> computeQpsScore() {
        Long fair = url.getLong(Constants.ADAPTIVE_QPS_FAIR);
        Long poor = url.getLong(Constants.ADAPTIVE_QPS_POOR);
        if (fair == null && poor == null) {
            return null;
        } else if (fair != null && poor == null) {
            return computeQpsScore(fair);
        } else {
            return new RankScore<>(fair, poor, null);
        }
    }

    /**
     * 获取中位数，做了优化
     *
     * @param nums
     * @return
     */
    protected static long median(final long[] nums) {
        switch (nums.length) {
            case 1:
                return nums[0];
            case 2:
                return (nums[0] + nums[1]) / 2;
            default:
                return partition(nums, 0, nums.length - 1);
        }
    }

    /**
     * 利用快排查找
     *
     * @param nums
     * @param start
     * @param end
     * @return
     */
    protected static long partition(final long[] nums, final int start, final int end) {
        int left = start;
        int right = end + 1;

        long point = nums[start];
        long tmp;
        while (true) {
            while (left < right && nums[--right] >= point) {
            }
            while (left < right && nums[++left] <= point) {
            }
            if (left == right) {
                break;
            } else {
                tmp = nums[left];
                nums[left] = nums[right];
                nums[right] = tmp;
            }
        }
        nums[start] = nums[left];
        nums[left] = point;

        int median = (nums.length - 1) / 2;
        if (left == median) {
            return nums[left];
        } else if (left > median) {
            return partition(nums, start, left - 1);
        } else {
            return partition(nums, left + 1, end);
        }
    }

    @Override
    public Node select(final Candidate candidate, final T request) {
        //获取LB的服务列表
        List<Node> candidates = candidate.getNodes();
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        //得到指标获取函数，增对不同的场景可能是节点或方法的
        Function<Dashboard, TPWindow> metricFunction = apply(request);
        //构建配置,计算并发及QPS的中位数
        AdaptiveConfig config = compute(build(request), candidate, metricFunction);

        AdaptiveLoadBalance.ClusterRank clusterRank = new AdaptiveLoadBalance.ClusterRank(candidate.getCluster(), config, metricFunction, nodeFunction);
        int size = candidates.size();
        //抽样随机打散，避免每次都拿到固定的节点
        boolean sampling = clusterRank.enoughGoods > 0 && clusterRank.arbiter.sampling();
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
    protected Function<Dashboard, TPWindow> apply(final T request) {
        if (request instanceof RequestMessage) {
            Object payload = ((RequestMessage) request).getPayLoad();
            if (payload != null && payload instanceof Invocation) {
                String method = ((Invocation) payload).getMethodName();
                return o -> o instanceof Dashboard ? o.getMethod(method) : o.getMetric();
            }
        }
        return o -> o.getMetric();
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
        protected AdaptiveConfig config;
        //打分器
        protected Iterable<Judge> juges;
        //综合评分器
        protected Arbiter arbiter;
        //选择器
        protected Election selector;
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
         * @param cluster
         * @param config
         * @param metricFunction
         * @param nodeFunction
         */
        public ClusterRank(final Cluster cluster, final AdaptiveConfig config,
                           final Function<Dashboard, TPWindow> metricFunction,
                           final Function<TPSnapshot, Integer> nodeFunction) {
            this.cluster = cluster;
            this.config = config;
            this.metricFunction = metricFunction;
            this.nodeFunction = nodeFunction;
            //插件列表有缓存
            this.juges = JUDGE.extensions();
            //综合评分插件
            this.arbiter = ARBITER.getOrDefault(config.getArbiter());
            //根据评分结果进行选择的插件
            this.selector = ELECTION.getOrDefault(config.getElection());
            this.enoughGoods = config.getEnoughGoods();
        }

        /**
         * 对候选者进行评分
         *
         * @param candidates
         * @return
         */
        public boolean score(final List<Node> candidates) {
            //遍历服务列表进行评分
            for (Node node : candidates) {
                if (!config.exclude(node) && score(node)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * 对单个服务实例进行评分
         *
         * @param node
         * @return
         */
        protected boolean score(final Node node) {
            //评分
            NodeRank rank = new NodeRank(node, cluster, metricFunction, nodeFunction)
                    .score(juges, config)
                    .score(arbiter, config);
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
         * @return
         */
        public NodeRank select() {
            switch (bestRanks.size()) {
                case 0:
                    return null;
                case 1:
                    return bestRanks.getFirst();
                default:
                    return selector.choose(bestRanks, config);
            }
        }

    }

}
