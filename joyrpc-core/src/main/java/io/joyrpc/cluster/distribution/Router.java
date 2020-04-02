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

import io.joyrpc.Result;
import io.joyrpc.cluster.Candidate;
import io.joyrpc.cluster.Node;
import io.joyrpc.extension.Extensible;
import io.joyrpc.extension.Prototype;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.util.TriFunction;

import java.util.concurrent.CompletableFuture;

/**
 * 路由策略
 */
@Extensible("router")
public interface Router extends Prototype {
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
     * 广播模式
     */
    String BROADCAST = "broadcast";
    /**
     * 并行模式
     */
    String FORKING = "forking";

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
     * 广播模式插件顺序
     */
    int ORDER_BROADCAST = 130;

    /**
     * 并行调用模式插件顺序
     */
    int ORDER_FORKING = 140;

    /**
     * 进行路由操作，不能修改候选者节点列表
     *
     * @param request   请求
     * @param candidate 候选者
     * @return 结果
     */
    CompletableFuture<Result> route(RequestMessage<Invocation> request, Candidate candidate);

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
    void setLoadBalance(LoadBalance loadBalance);

    /**
     * 设置调用方法
     *
     * @param operation 调用方法
     */
    void setOperation(TriFunction<Node, Node, RequestMessage<Invocation>, CompletableFuture<Result>> operation);

    /**
     * 设置
     */
    default void setup() {

    }

}
