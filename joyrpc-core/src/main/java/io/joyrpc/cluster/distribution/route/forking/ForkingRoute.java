package io.joyrpc.cluster.distribution.route.forking;

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
import io.joyrpc.cluster.distribution.Route;
import io.joyrpc.cluster.distribution.route.AbstractRoute;
import io.joyrpc.constants.Constants;
import io.joyrpc.extension.Extension;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static io.joyrpc.cluster.distribution.Route.BROADCAST;

/**
 * 并行调用
 */
@Extension(value = BROADCAST, order = Route.ORDER_BROADCAST)
public class ForkingRoute<T, R> extends AbstractRoute<T, R> {

    @Override
    public CompletableFuture<R> invoke(final T request, final Candidate candidate) {
        CompletableFuture<R> result = new CompletableFuture<>();
        List<Node> nodes = candidate.getNodes();
        //并行调用的数量
        int forks = url.getInteger(Constants.FORKS_OPTION);
        forks = forks <= 0 || forks > nodes.size() ? nodes.size() : forks;
        CompletableFuture<R>[] futures = new CompletableFuture[forks];
        int total = 0;
        if (forks == nodes.size()) {
            //全部调用
            for (Node node : nodes) {
                futures[total++] = operation.apply(node, null, request);
            }
        } else if (forks == 1) {
            //调用一个
            Node node = loadBalance.select(candidate, request);
            if (node != null) {
                futures[total++] = operation.apply(node, null, request);
            }
        } else {
            //选择指定数量的节点进行调用
            nodes = new LinkedList<>(nodes);
            Node node = null;
            for (int i = 0; i < forks; i++) {
                node = loadBalance.select(node == null ? candidate : new Candidate(candidate, nodes.remove(node) ? nodes : nodes), request);
                if (node != null) {
                    total++;
                    futures[i] = operation.apply(node, null, request);
                } else {
                    break;
                }
            }
        }
        AtomicInteger counter = new AtomicInteger(total);
        for (int i = 0; i < total; i++) {
            futures[i].whenComplete((value, error) -> {
                int remain = counter.decrementAndGet();
                if (error != null) {
                    //异常
                    if (remain == 0) {
                        result.completeExceptionally(error);
                    }
                } else if (judge.test(value)) {
                    //结果内容是异常
                    if (remain == 0) {
                        result.complete(value);
                    }
                } else {
                    result.complete(value);
                }
            });
        }
        return result;
    }

}
