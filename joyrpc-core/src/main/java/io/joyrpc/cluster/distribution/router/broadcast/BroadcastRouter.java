package io.joyrpc.cluster.distribution.router.broadcast;

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
import io.joyrpc.extension.Extension;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static io.joyrpc.cluster.distribution.Router.BROADCAST;

/**
 * 广播模式，遍历每个可用节点进行调用，如果有一个失败则返回失败，否则返回最后一个节点的调用结果
 */
@Extension(value = BROADCAST, order = Router.ORDER_BROADCAST)
public class BroadcastRouter extends AbstractRouter {

    @Override
    public CompletableFuture<Result> route(final RequestMessage<Invocation> request, final Candidate candidate) {
        List<Node> nodes = candidate.getNodes();
        int size = nodes.size();
        CompletableFuture<Result>[] futures = new CompletableFuture[size];
        int i = 0;
        for (Node node : nodes) {
            futures[i] = operation.apply(node, null, request);
        }
        return CompletableFuture.allOf(futures).handle((v, error) -> {
            if (error != null) {
                //有异常
                return new Result(request.getContext(), error);
            }
            //遍历结果
            Result result = null;
            for (CompletableFuture<Result> future : futures) {
                try {
                    result = future.join();
                    //结果是异常
                    if (result.isException()) {
                        break;
                    }
                } catch (Throwable e) {
                    return new Result(request.getContext(), e);
                }
            }
            return result;
        });
    }

}
