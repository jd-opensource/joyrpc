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

/**
 * 根据集群指标，自动计算方法调用指标阈值
 */
public interface AdaptiveScorer {

    /**
     * 自动计算方法调用指标阈值
     *
     * @param cluster 集群
     * @param method  方法
     * @param config  配置
     * @return 方法调用指标阈值
     */
    AdaptiveConfig score(Cluster cluster, String method, AdaptiveConfig config);

}
