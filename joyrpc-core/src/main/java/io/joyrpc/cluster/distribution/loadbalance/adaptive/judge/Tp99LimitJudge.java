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
import io.joyrpc.metric.TPSnapshot;

/**
 * TP99评分
 */
public class Tp99LimitJudge extends AbstractJudge implements MetricAware {

    public Tp99LimitJudge() {
        super(JudgeType.Tp99Limit);
    }

    @Override
    public Rank score(final NodeMetric metric, final AdaptivePolicy policy) {
        //服务,集群指标
        TPSnapshot nodeTp = metric.getNodeSnapshot().getSnapshot();
        Rank result;
        Rank score;
        if (nodeTp.getRequests() == 0 && metric.isWeak()) {
            //当虚弱的时候，由于没有数据，容易判断出Good，进行修正
            result = Rank.Fair;
        } else {
            result = score(policy.getTpScore(), metric.getNodeFunction().apply(nodeTp), RankScore.INT_DESCENDING);
        }
        //先考虑TP，再考虑可用率
        switch (result) {
            case Good:
                return score(policy.getAvailabilityScore(), nodeTp.getAvailability(), RankScore.DOUBLE_ASCENDING);
            case Fair:
                score = score(policy.getAvailabilityScore(), nodeTp.getAvailability(), RankScore.DOUBLE_ASCENDING);
                return score.ordinal() <= Rank.Fair.ordinal() ? Rank.Fair : score;
            case Poor:
                score = score(policy.getAvailabilityScore(), nodeTp.getAvailability(), RankScore.DOUBLE_ASCENDING);
                return score.ordinal() <= Rank.Poor.ordinal() ? Rank.Poor : score;
            default:
                return Rank.Disabled;
        }
    }


}
