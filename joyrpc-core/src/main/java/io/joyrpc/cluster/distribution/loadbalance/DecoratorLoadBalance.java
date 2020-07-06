package io.joyrpc.cluster.distribution.loadbalance;

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
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 装饰器负载均衡
 */
public class DecoratorLoadBalance implements LoadBalance {

    protected LoadBalance delegate;
    protected Predicate<Node> predicate;

    public DecoratorLoadBalance(final LoadBalance delegate, final Predicate<Node> predicate) {
        this.delegate = delegate;
        this.predicate = predicate;
    }

    @Override
    public Node select(final Candidate candidate, final RequestMessage<Invocation> request) {
        List<Node> nodes = candidate.getNodes();
        int size = nodes == null ? 0 : nodes.size();
        switch (size) {
            case 0:
                return null;
            case 1:
                return nodes.get(0);
            default:
                //判断是否支持优选
                if (predicate == null) {
                    return delegate.select(candidate, request);
                }
                List<Node> prefers = prefer(nodes);
                int preferSize = prefers.size();
                switch (preferSize) {
                    case 0:
                        return delegate.select(candidate, request);
                    case 1:
                        return prefers.get(0);
                    default:
                        return delegate.select(preferSize == size ? candidate : new Candidate(candidate, prefers), request);
                }
        }
    }

    /**
     * 获取优先节点
     *
     * @param nodes
     * @return
     */
    protected List<Node> prefer(final List<Node> nodes) {
        List<Node> prefers = new LinkedList<>();
        for (Node node : nodes) {
            //判断本地地址
            if (predicate.test(node)) {
                prefers.add(node);
            }
        }
        return prefers;
    }

    @Override
    public void setUrl(URL url) {
        delegate.setUrl(url);
    }

    @Override
    public void setup() {
        delegate.setup();
    }
}
