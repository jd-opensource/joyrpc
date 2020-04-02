package io.joyrpc.cluster.distribution.selector.method;

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
import io.joyrpc.cluster.distribution.NodeSelector;
import io.joyrpc.config.InterfaceOption.ConsumerMethodOption;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * 基于方法的条件路由
 */
@Extension(value = "methodRouter")
public class MethodSelector implements NodeSelector {
    /**
     * URL配置
     */
    protected URL url;
    /**
     * 接口类
     */
    protected String className;

    @Override
    public void setUrl(final URL url) {
        this.url = url;
    }

    @Override
    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public List<Node> select(final Candidate candidate, final RequestMessage<Invocation> request) {
        ConsumerMethodOption option = (ConsumerMethodOption) request.getOption();
        BiPredicate<Shard, RequestMessage<Invocation>> predicate = option.getSelector();
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
