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
     * 快速失败算法
     */
    String FAIL_FAST = "failfast";
    /**
     * 异常重试算法
     */
    String FAIL_OVER = "failover";

    /**
     * 定点调用算法
     */
    String PIN_POINT = "pinpoint";

    /**
     * 快速失败插件顺序
     */
    int ORDER_FAILFAST = 100;
    /**
     * 异常重试插件顺序
     */
    int ORDER_FAILOVER = 110;

    /**
     * 定点调用插件顺序
     */
    int ORDER_PINPOINT = 120;

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
     * @param url url
     */
    void setUrl(URL url);

    /**
     * 设置负载均衡
     *
     * @param loadBalance 负责均衡
     */
    void setLoadBalance(LoadBalance<T> loadBalance);

    /**
     * 设置调用方法
     *
     * @param function 调用方法
     */
    void setFunction(TriFunction<Node, Node, T, CompletableFuture<R>> function);

    /**
     * 初始化
     */
    default void setup() {

    }

}
