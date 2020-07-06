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
 * 并发数评分
 */
public class ConcurrencyLimitJudge extends AbstractJudge implements MetricAware {

    public ConcurrencyLimitJudge() {
        super(JudgeType.ConcurrencyLimit);
    }

    @Override
    public Rank score(final NodeMetric metric, final AdaptivePolicy policy) {
        TPMetric snapshot = metric.getNodeSnapshot();
        //集群并发数阈值，如果没有设置默认为中位数
        return score(policy.getConcurrencyScore(), snapshot.getActives(), RankScore.LONG_DESCENDING);
    }

}
