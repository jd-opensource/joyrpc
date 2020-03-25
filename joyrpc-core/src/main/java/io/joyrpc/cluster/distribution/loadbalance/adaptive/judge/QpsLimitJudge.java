package io.joyrpc.cluster.distribution.loadbalance.adaptive.judge;

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
import io.joyrpc.metric.TPMetric;


/**
 * 每秒请求数评分
 */
public class QpsLimitJudge extends AbstractJudge implements MetricAware {

    public QpsLimitJudge() {
        super(JudgeType.QpsLimit);
    }

    @Override
    public Rank score(final NodeMetric metric, final AdaptivePolicy policy) {
        //服务,集群指标
        TPMetric nodeSnapshot = metric.getNodeSnapshot();
        //当前服务请求数指标+当前服务已经分发
        long requests = nodeSnapshot.getSnapshot().getRequests() + nodeSnapshot.getDistribution();
        //可以设置qps阈值，如果未设置默认计算集群QPS中位数；
        return score(policy.getQpsScore(), requests, RankScore.LONG_DESCENDING);
    }

}
