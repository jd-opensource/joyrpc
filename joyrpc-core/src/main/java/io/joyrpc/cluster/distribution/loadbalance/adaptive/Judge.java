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


/**
 * 裁判，进行投票打分
 */
public interface Judge extends Type<String> {

    /**
     * 评分
     *
     * @param metric 服务及指标
     * @param policy 策略
     * @return 评分
     */
    Rank score(NodeMetric metric, AdaptivePolicy policy);

    /**
     * 默认的权重
     *
     * @return
     */
    int ratio();

    //TODO 训练默认系数
    enum JudgeType {
        ServerStatus("ServerStatus", 10, 55),
        ZoneAware("ZoneAware", 20, 55),
        Tp99Limit("Tp99Limit", 30, 30),
        ConcurrencyLimit("ConcurrencyLimit", 40, 20),
        QpsLimit("QpsLimit", 50, 20);

        private String name;
        private int order;
        private int ratio;

        JudgeType(String name, int order, int ratio) {
            this.name = name;
            this.order = order;
            this.ratio = ratio;
        }

        public String getName() {
            return name;
        }

        public int getOrder() {
            return order;
        }

        public int getRatio() {
            return ratio;
        }
    }

}
