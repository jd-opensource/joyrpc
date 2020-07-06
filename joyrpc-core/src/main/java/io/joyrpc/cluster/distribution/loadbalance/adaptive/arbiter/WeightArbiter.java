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

import io.joyrpc.cluster.distribution.loadbalance.adaptive.AdaptivePolicy;
import io.joyrpc.cluster.distribution.loadbalance.adaptive.JudgeRank;
import io.joyrpc.cluster.distribution.loadbalance.adaptive.NodeMetric;
import io.joyrpc.cluster.distribution.loadbalance.adaptive.Rank;

import java.util.List;

/**
 * 只根据权重进行判断。过滤掉禁用的节点
 */
public class WeightArbiter extends OverallArbiter {

    @Override
    public String type() {
        return "weight";
    }

    @Override
    public int order() {
        return WEIGHT_ORDER;
    }

    @Override
    public boolean sampling() {
        //不采样，全部参与判断
        return false;
    }

    @Override
    public Rank score(final NodeMetric node, final List<JudgeRank> ranks, final AdaptivePolicy policy) {
        Rank rank = super.score(node, ranks, policy);
        switch (rank) {
            case Disabled:
                return Rank.Disabled;
            default:
                return Rank.Good;
        }
    }

}
