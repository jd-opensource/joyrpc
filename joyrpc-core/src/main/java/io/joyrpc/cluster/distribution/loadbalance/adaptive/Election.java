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

import io.joyrpc.extension.Type;

import java.util.List;

/**
 * 选择器
 */
public interface Election extends Type<String> {

    int RANDOM_WEIGHT_ORDER = 100;

    /**
     * 选择服务
     *
     * @param ranks  评分
     * @param policy
     * @return
     */
    NodeRank choose(List<NodeRank> ranks, AdaptivePolicy policy);
}
