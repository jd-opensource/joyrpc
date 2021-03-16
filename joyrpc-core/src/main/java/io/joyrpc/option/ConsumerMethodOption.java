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

import io.joyrpc.cluster.Shard;
import io.joyrpc.cluster.distribution.CircuitBreaker;
import io.joyrpc.cluster.distribution.FailoverPolicy;
import io.joyrpc.cluster.distribution.Router;
import io.joyrpc.cluster.distribution.loadbalance.adaptive.AdaptivePolicy;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

import java.util.Map;
import java.util.function.BiPredicate;

/**
 * 消费者方法选项
 */
public interface ConsumerMethodOption extends MethodOption {
    /**
     * 获取并行度
     *
     * @return 并行度
     */
    int getForks();

    /**
     * 节点选择器算法
     *
     * @return 节点选择器算法
     */
    BiPredicate<Shard, RequestMessage<Invocation>> getSelector();

    /**
     * 获取分发策略
     *
     * @return 分发策略
     */
    Router getRouter();

    /**
     * 故障切换策略
     *
     * @return 故障切换策略
     */
    FailoverPolicy getFailoverPolicy();

    /**
     * 获取自适应负载均衡配置
     *
     * @return 自适应负载均衡配置
     */
    AdaptivePolicy getAdaptivePolicy();

    /**
     * 获取熔断器
     *
     * @return
     */
    CircuitBreaker getCircuitBreaker();

    /**
     * 返回方法的Mock数据
     *
     * @return 方法的Mock数据
     */
    Map<String, Object> getMock();

    /**
     * 设置是否开启自动计算方法指标阈值
     *
     * @param autoScore 自动计算方法指标阈值标识
     */
    void setAutoScore(boolean autoScore);

}
