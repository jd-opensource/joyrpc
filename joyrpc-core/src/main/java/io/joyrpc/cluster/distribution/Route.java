package io.joyrpc.cluster.distribution;

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

import io.joyrpc.cluster.Candidate;
import io.joyrpc.cluster.Node;
import io.joyrpc.extension.Extensible;
import io.joyrpc.extension.Prototype;
import io.joyrpc.extension.URL;
import io.joyrpc.util.TriFunction;

import java.util.concurrent.CompletableFuture;

/**
 * 路由分发
 */
@Extensible("route")
public interface Route<T, R> extends Prototype {
    /**
     * failfast插件常量
     */
    String FAIL_FAST = "failfast";
    /**
     * failover插件常量
     */
    String FAIL_OVER = "failover";

    /**
     * pinpoint插件常量
     */
    String PIN_POINT = "pinpoint";

    /**
     * Failfast顺序
     */
    int ORDER_FAILFAST = 100;
    /**
     * Failover顺序
     */
    int ORDER_FAILOVER = 110;

    /**
     * pinpoint顺序
     */
    int ORDER_PINOINT = 120;

    /**
     * 调用，不能修改候选者节点列表
     *
     * @param request   请求
     * @param candidate 候选者
     * @return
     */
    CompletableFuture<R> invoke(T request, Candidate candidate);

    /**
     * 设置URL
     *
     * @param url
     */
    void setUrl(URL url);

    /**
     * 设置负载均衡
     *
     * @param loadBalance
     */
    void setLoadBalance(LoadBalance<T> loadBalance);

    /**
     * 设置调用方法
     *
     * @param function
     */
    void setFunction(TriFunction<Node, Node, T, CompletableFuture<R>> function);

    /**
     * 初始化
     */
    default void setup() {

    }

}
