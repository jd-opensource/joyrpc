package io.joyrpc.cluster.distribution.loadbalance.adaptive.arbiter;

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

import io.joyrpc.cluster.distribution.loadbalance.adaptive.*;
import io.joyrpc.extension.Ordered;

import java.util.List;

import static io.joyrpc.constants.Constants.DEFAULT_DECUBATION;

/**
 * 计算综合得分，并调整权重
 */
public class OverallArbiter implements Arbiter, Ordered {

    @Override
    public Rank score(final NodeMetric node, final List<JudgeRank> ranks, final AdaptivePolicy policy) {
        //计算综合得分
        Rank rank = compute(ranks);
        //调整权重
        weight(node, rank, policy);
        return rank;
    }

    /**
     * 根据评价调整权重
     *
     * @param node
     * @param rank
     */
    protected void weight(final NodeMetric node, final Rank rank, final AdaptivePolicy policy) {
        switch (rank) {
            case Good:
                break;
            case Fair:
                //相对应Good，再降低权限
                int weight = node.getWeight() * 2 / 3;
                node.setWeight(weight > 1 ? weight : 1);
                break;
            case Poor:
                node.setWeight(1);
                //继续虚弱，内部处理并发频繁修改
                node.weak(policy.getDecubation() == null || policy.getDecubation() <= 0 ? DEFAULT_DECUBATION : policy.getDecubation());
                break;
            case Disabled:
                node.setWeight(0);
                if (!node.isBroken()) {
                    //虚弱，内部处理并发频繁修改
                    node.weak(policy.getDecubation() == null || policy.getDecubation() <= 0 ? DEFAULT_DECUBATION : policy.getDecubation());
                }
        }
    }

    /**
     * 计算综合得分
     *
     * @param ranks
     * @return
     */
    protected Rank compute(final List<JudgeRank> ranks) {
        int score = 0;
        int total = 0;
        int ratio;
        for (JudgeRank rank : ranks) {
            //一票否定
            if (rank.getRank() == Rank.Disabled) {
                return Rank.Disabled;
            }
            //获取裁判系数，判断是否有投票权
            ratio = rank.getRatio();
            if (ratio > 0) {
                //有投票权
                score += rank.getRank().getMin() * ratio;
                total += ratio;
            }
        }
        //所有的ratio都为0，说明过滤链无效，都返回Good
        if (total == 0) {
            return Rank.Good;
        } else {
            score = score / total;
            //防止被禁用
            return Rank.valueOf(score == 0 ? 1 : score);
        }
    }

    @Override
    public String type() {
        return "overall";
    }

    @Override
    public int order() {
        return OVERALL_ORDER;
    }

    @Override
    public boolean sampling() {
        return true;
    }
}
