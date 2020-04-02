package io.joyrpc.cluster.distribution.router.forking;

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
import io.joyrpc.cluster.distribution.Router;
import io.joyrpc.cluster.distribution.router.AbstractRouter;
import io.joyrpc.config.InterfaceOption.ConsumerMethodOption;
import io.joyrpc.extension.Extension;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static io.joyrpc.cluster.distribution.Router.FORKING;

/**
 * 并行调用
 */
@Extension(value = FORKING, order = Router.ORDER_FORKING)
public class ForkingRouter extends AbstractRouter {

    @Override
    public CompletableFuture<Result> route(final RequestMessage<Invocation> request, final Candidate candidate) {
        CompletableFuture<Result> result = new CompletableFuture<>();
        List<Node> nodes = candidate.getNodes();
        ConsumerMethodOption option = (ConsumerMethodOption) request.getOption();
        //并行调用的数量
        int forks = option.getForks();
        forks = forks <= 0 || forks > nodes.size() ? nodes.size() : forks;
        CompletableFuture<Result>[] futures = new CompletableFuture[forks];
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
            futures[i].whenComplete((r, error) -> {
                int remain = counter.decrementAndGet();
                if (error != null) {
                    //异常
                    if (remain == 0) {
                        result.complete(new Result(request.getContext(), error));
                    }
                } else if (r.isException()) {
                    //结果内容是异常
                    if (remain == 0) {
                        result.complete(r);
                    }
                } else {
                    result.complete(r);
                }
            });
        }
        return result;
    }
}
