package io.joyrpc.cluster.distribution.loadbalance.adaptive.election;

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

import io.joyrpc.cluster.distribution.loadbalance.RandomWeight;
import io.joyrpc.cluster.distribution.loadbalance.adaptive.AdaptivePolicy;
import io.joyrpc.cluster.distribution.loadbalance.adaptive.Election;
import io.joyrpc.cluster.distribution.loadbalance.adaptive.NodeRank;
import io.joyrpc.extension.Ordered;

import java.util.List;


/**
 * 带权重的随机负载均衡算法
 */
public class RandomWeightElection implements Election, Ordered {

    @Override
    public NodeRank choose(final List<NodeRank> ranks, final AdaptivePolicy policy) {
        return RandomWeight.select(ranks);
    }

    @Override
    public String type() {
        return "randomWeight";
    }

    @Override
    public int order() {
        return RANDOM_WEIGHT_ORDER;
    }
}
