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

import io.joyrpc.cluster.Node;
import io.joyrpc.cluster.Region;
import io.joyrpc.cluster.distribution.loadbalance.adaptive.AdaptivePolicy;
import io.joyrpc.cluster.distribution.loadbalance.adaptive.NodeMetric;
import io.joyrpc.cluster.distribution.loadbalance.adaptive.Rank;

/**
 * 地域机房评分
 */
public class ZoneAwareJudge extends AbstractJudge {

    public ZoneAwareJudge() {
        super(JudgeType.ZoneAware);
    }

    @Override
    public Rank score(final NodeMetric metric, final AdaptivePolicy policy) {
        Node node = metric.getNode();
        Region region = metric.getCluster().getRegion();
        MatchType area = match(region == null ? null : region.getRegion(), node.getRegion());
        if (area == MatchType.None) {
            //地域不匹配
            return Rank.Poor;
        }
        MatchType room = match(region == null ? null : region.getDataCenter(), node.getDataCenter());
        if (area == MatchType.Exact && room == MatchType.Exact) {
            //地域和机房都匹配
            return Rank.Good;
        }
        //地域匹配
        return Rank.Fair;
    }

    /**
     * 匹配
     *
     * @param source
     * @param target
     * @return
     */
    protected MatchType match(final String source, final String target) {
        if (source == null || source.isEmpty() || target == null || target.isEmpty()) {
            return MatchType.Fuzzy;
        }
        return source.equals(target) ? MatchType.Exact : MatchType.None;
    }

    /**
     * 匹配类型
     */
    protected enum MatchType {
        /**
         * 精确
         */
        Exact,
        /**
         * 模糊匹配
         */
        Fuzzy,
        /**
         * 不匹配
         */
        None;
    }

}
