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

import io.joyrpc.cluster.distribution.loadbalance.adaptive.Judge;
import io.joyrpc.extension.Ordered;

/**
 * 抽象的裁判
 */
public abstract class AbstractJudge implements Judge, Ordered {
    /**
     * 类型
     */
    protected JudgeType type;

    public AbstractJudge(JudgeType type) {
        this.type = type;
    }

    @Override
    public String type() {
        return type.getName();
    }

    @Override
    public int order() {
        return type.getOrder();
    }

    @Override
    public int ratio() {
        return type.getRatio();
    }

}
