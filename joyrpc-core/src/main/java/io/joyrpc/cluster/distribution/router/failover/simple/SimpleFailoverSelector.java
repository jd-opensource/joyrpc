package io.joyrpc.cluster.distribution.router.failover.simple;

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
import io.joyrpc.cluster.distribution.FailoverSelector;
import io.joyrpc.extension.Extension;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 淘汰制选择器
 */
@Extension("simple")
public class SimpleFailoverSelector implements FailoverSelector {

    /**
     * 默认单例
     */
    public static final SimpleFailoverSelector INSTANCE = new SimpleFailoverSelector();

    @Override
    public Candidate select(final Candidate candidate, final Node node, final int retry,
                            final List<Node> fails, final List<Node> origins) {
        List<Node> nodes = candidate.getNodes();
        if (retry == 0) {
            //TODO 优先其它机房重试
            //初次重试复制数据，后续重试不需要每次拷贝复制一份
            return new Candidate(candidate, copy(nodes, nodes.size() == 1 ? null : n -> n != node));
        } else if (origins.size() == 1) {
            //只有一个节点，则直接重试
            return candidate;
        } else if (candidate.getSize() == 1 || node == null) {
            //剩下一个节点了，或者当前负载均衡没有选择可用的节点，则恢复原始节点进行重试
            return new Candidate(candidate, new ArrayList<>(origins));
        } else {
            //TODO 删除失败节点性能损耗，最好LB返回节点和位置
            //删除该节点，用剩下的节点进行重试
            nodes.remove(node);
            return candidate;
        }
    }

    /**
     * 拷贝节点
     *
     * @param from
     * @param predicate
     */
    protected static final List<Node> copy(final List<Node> from, final Predicate<Node> predicate) {
        List<Node> result;
        //判断预测是否存在，不存在则进行内部复制，提升性能
        if (predicate == null) {
            result = new ArrayList<>(from);
        } else {
            result = new ArrayList<>(from.size());
            for (Node node : from) {
                if (predicate.test(node)) {
                    result.add(node);
                }
            }
        }
        return result;
    }
}
