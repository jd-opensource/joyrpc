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
import io.joyrpc.cluster.Cluster;
import io.joyrpc.cluster.Node;
import io.joyrpc.cluster.Region;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * 组合分发
 */
public class Distribution<T, R> {
    /**
     * 集群
     */
    protected final Cluster cluster;
    /**
     * 路由器
     */
    protected final Router<T> router;
    /**
     * 路由策略
     */
    protected final Route<T, R> route;
    /**
     * 节点为空的提供者
     */
    protected final Function<T, CompletableFuture<R>> emptySupplier;

    /**
     * 构造函数
     *
     * @param cluster
     * @param router
     * @param route
     * @param emptySupplier
     */
    public Distribution(final Cluster cluster, final Router<T> router, final Route<T, R> route,
                        final Function<T, CompletableFuture<R>> emptySupplier) {
        this.cluster = cluster;
        this.router = router;
        this.route = route;
        this.emptySupplier = emptySupplier;
    }

    /**
     * 分发
     *
     * @param request 请求
     * @return
     */
    public CompletableFuture<R> distribute(final T request) {

        //地域
        Region region = cluster.getRegion();
        //集群节点
        List<Node> routes = cluster.getNodes();
        if (!routes.isEmpty()) {
            //候选者
            if (router != null) {
                //路由选择
                routes = router.route(new Candidate(cluster, region, routes, routes.size()), request);
            }
        }
        if (routes == null || routes.isEmpty()) {
            //节点为空
            return emptySupplier.apply(request);
        }

        Candidate candidate = new Candidate(cluster, region, routes, routes.size());
        return route.invoke(request, candidate);

    }
}
