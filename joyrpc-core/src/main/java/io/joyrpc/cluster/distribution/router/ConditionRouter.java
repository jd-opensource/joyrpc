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

import io.joyrpc.cluster.Candidate;
import io.joyrpc.cluster.Node;
import io.joyrpc.cluster.Shard;
import io.joyrpc.cluster.distribution.Router;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

/**
 * 条件路由
 */
public class ConditionRouter<T> implements Router<T> {

    protected Supplier<BiPredicate<Shard, T>> supplier;

    public ConditionRouter() {
    }

    public ConditionRouter(Supplier<BiPredicate<Shard, T>> supplier) {
        this.supplier = supplier;
    }

    @Override
    public List<Node> route(final Candidate candidate, final T request) {
        BiPredicate<Shard, T> predicate = supplier == null ? null : supplier.get();
        if (predicate == null) {
            return candidate.getNodes();
        }
        List<Node> result = new LinkedList<>();
        List<Node> nodes = candidate.getNodes();
        //先遍历服务列表
        for (Node node : nodes) {
            //在遍历路由规则
            if (predicate.test(node, request)) {
                result.add(node);
            }
        }
        return result;
    }

}
