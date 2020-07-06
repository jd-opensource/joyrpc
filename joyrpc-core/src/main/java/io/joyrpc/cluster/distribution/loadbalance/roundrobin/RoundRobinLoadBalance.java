package io.joyrpc.cluster.distribution.loadbalance.roundrobin;

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
import io.joyrpc.cluster.distribution.LoadBalance;
import io.joyrpc.extension.Extension;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询负载均衡
 */
@Extension("roundRobin")
public class RoundRobinLoadBalance implements LoadBalance {

    protected AtomicInteger counter = new AtomicInteger(0);

    @Override
    public Node select(final Candidate candidate, final RequestMessage<Invocation> request) {
        final List<Node> shards = candidate.getNodes();
        int next;
        int current;
        int size = shards.size();
        for (; ; ) {
            current = counter.get();
            next = (current + 1) % size;
            if (counter.compareAndSet(current, next)) {
                break;
            }
        }
        return shards.get(next);
    }
}
