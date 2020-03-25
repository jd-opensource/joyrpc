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
import io.joyrpc.metric.Dashboard;
import io.joyrpc.metric.TPSnapshot;
import io.joyrpc.metric.TPWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;


/**
 * 服务的评分
 */
public class NodeRank extends NodeMetric {

    private final static Logger logger = LoggerFactory.getLogger(NodeRank.class);

    //裁判评分
    protected List<JudgeRank> ranks = new LinkedList<>();
    //最终评分
    protected Rank rank;

    /**
     * 构造函数
     *
     * @param node         节点
     * @param cluster      集群
     * @param function     窗口函数
     * @param nodeFunction 节点指标函数
     */
    public NodeRank(Node node, Cluster cluster,
                    Function<Dashboard, TPWindow> function,
                    Function<TPSnapshot, Integer> nodeFunction) {
        super(node, cluster, function, nodeFunction);
    }

    public List<JudgeRank> getRanks() {
        return ranks;
    }

    public Rank getRank() {
        return rank;
    }

    /**
     * 裁判打分
     *
     * @param policy 策略
     * @return
     */
    public NodeRank score(final AdaptivePolicy policy) {
        Rank rank;
        int ratio;
        String name;
        boolean noMetric = noMetric();
        for (Judge judge : policy.getJudges()) {
            name = judge.type();
            //获取当前裁判的系数
            ratio = policy.getRatio(name, judge.ratio());
            if (ratio <= 0 || noMetric && judge instanceof MetricAware) {
                //无权投票或者需要指标但是没有指标，不进行判断，默认是Good
                ranks.add(new JudgeRank(name, Rank.Good, 0));
            } else {
                //有投票权，进行评分
                rank = judge.score(this, policy);
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("ServerRank score judge:%s ratio:%d rank:%s", judge.type(), judge.ratio(), rank.getName()));
                }
                ranks.add(new JudgeRank(name, rank, ratio));
                if (rank == Rank.Disabled) {
                    //一票否决，加快速度
                    //logger.info(String.format("ServerRank score judge:%s ratio:%d rank:%s", judge.type(), judge.ratio(), rank.getName()));
                    break;
                }
            }
        }
        policy.getArbiter().score(this, ranks, policy);
        return this;
    }
}
