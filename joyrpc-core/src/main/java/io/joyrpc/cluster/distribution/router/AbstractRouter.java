package io.joyrpc.cluster.distribution.router;

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
import io.joyrpc.cluster.Node;
import io.joyrpc.cluster.distribution.LoadBalance;
import io.joyrpc.cluster.distribution.Router;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.util.TriFunction;

import java.util.concurrent.CompletableFuture;

/**
 * 抽象的Route实现
 */
public abstract class AbstractRouter implements Router {

    /**
     * 负载均衡
     */
    protected LoadBalance loadBalance;
    /**
     * 路由操作
     */
    protected TriFunction<Node, Node, RequestMessage<Invocation>, CompletableFuture<Result>> operation;
    /**
     * URL
     */
    protected URL url;

    @Override
    public void setLoadBalance(LoadBalance loadBalance) {
        this.loadBalance = loadBalance;
    }

    @Override
    public void setOperation(TriFunction<Node, Node, RequestMessage<Invocation>, CompletableFuture<Result>> operation) {
        this.operation = operation;
    }

    @Override
    public void setUrl(URL url) {
        this.url = url;
    }

}
