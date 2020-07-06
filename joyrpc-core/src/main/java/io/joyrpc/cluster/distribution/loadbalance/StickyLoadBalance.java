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
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

import java.util.List;

/**
 * 粘连算法
 */
public class StickyLoadBalance extends DecoratorLoadBalance {
    /**
     * 上一次的选择结果
     */
    protected Node last;

    /**
     * 构造函数
     *
     * @param delegate
     */
    public StickyLoadBalance(final LoadBalance delegate) {
        super(delegate, null);
        this.predicate = o -> last == null || last.getName().equals(o.getName());
    }

    @Override
    protected List<Node> prefer(final List<Node> nodes) {
        return last == null ? nodes : super.prefer(nodes);
    }

    @Override
    public Node select(Candidate candidate, RequestMessage<Invocation> request) {
        last = super.select(candidate, request);
        return last;
    }
}
