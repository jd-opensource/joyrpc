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
 * 综合得分计算
 */
public interface Arbiter extends Type<String> {

    int OVERALL_ORDER = 100;
    int WEIGHT_ORDER = OVERALL_ORDER - 1;

    /**
     * 计算综合得分
     *
     * @param node   服务
     * @param ranks  裁判的打分
     * @param policy 策略
     * @return
     */
    Rank score(NodeMetric node, List<JudgeRank> ranks, AdaptivePolicy policy);

    /**
     * 是否支持抽样，不需要对所有节点进行打分
     *
     * @return
     */
    default boolean sampling() {
        return false;
    }

}
