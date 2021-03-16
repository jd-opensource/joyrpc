package io.joyrpc.option;

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

import io.joyrpc.cluster.distribution.loadbalance.adaptive.AdaptiveConfig;
import io.joyrpc.cluster.distribution.loadbalance.adaptive.AdaptivePolicy;
import io.joyrpc.cluster.distribution.loadbalance.adaptive.Judge;

import java.util.List;

/**
 * 方法自适应配置
 */
public class MethodAdaptiveOption {
    /**
     * 接口静态配置
     */
    protected final AdaptiveConfig interfaceConfig;
    /**
     * 接口动态配置
     */
    protected AdaptiveConfig dynamicInterfaceConfig;
    /**
     * 方法静态配置
     */
    protected final AdaptiveConfig methodConfig;
    /**
     * 裁决者
     */
    protected final List<Judge> judges;
    /**
     * 方法动态评分
     */
    protected AdaptiveConfig score;
    /**
     * 配置合并
     */
    protected volatile AdaptiveConfig config;
    /**
     * 最终配置，包括自动计算评分的结果
     */
    protected volatile AdaptivePolicy policy;

    /**
     * 构造函数
     *
     * @param interfaceConfig        接口静态配置
     * @param methodConfig      方法静态配置
     * @param dynamicInterfaceConfig 接口动态配置
     * @param judges            裁判
     */
    public MethodAdaptiveOption(final AdaptiveConfig interfaceConfig, final AdaptiveConfig methodConfig,
                                final AdaptiveConfig dynamicInterfaceConfig, final List<Judge> judges) {
        this.interfaceConfig = interfaceConfig;
        this.methodConfig = methodConfig;
        this.dynamicInterfaceConfig = dynamicInterfaceConfig;
        this.judges = judges;
        this.policy = new AdaptivePolicy(interfaceConfig, judges);
        update();
    }

    public void setDynamicInterfaceConfig(AdaptiveConfig dynamicInterfaceConfig) {
        if (dynamicInterfaceConfig != this.dynamicInterfaceConfig) {
            this.dynamicInterfaceConfig = dynamicInterfaceConfig;
            update();
        }
    }

    public void setScore(AdaptiveConfig score) {
        if (score != this.score) {
            this.score = score;
            update();
        }
    }

    public AdaptivePolicy getPolicy() {
        return policy;
    }

    public AdaptiveConfig getConfig() {
        return config;
    }

    /**
     * 更新
     */
    protected synchronized void update() {
        AdaptiveConfig result = new AdaptiveConfig(interfaceConfig);
        result.merge(dynamicInterfaceConfig);
        result.merge(methodConfig);
        //配置合并
        config = new AdaptiveConfig(result);
        //加上自动计算的评分
        result.merge(score);
        policy = new AdaptivePolicy(result, judges);
    }
}
