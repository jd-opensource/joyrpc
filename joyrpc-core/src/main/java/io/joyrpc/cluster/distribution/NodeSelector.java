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

import io.joyrpc.InvokerAware;
import io.joyrpc.cluster.Candidate;
import io.joyrpc.cluster.Node;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.Prototype;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

import java.util.List;

/**
 * 路由目标节点选择
 */
@Extension("nodeSelector")
public interface NodeSelector extends InvokerAware, Prototype {

    /**
     * 选择目标节点
     *
     * @param candidate 候选者
     * @param request   请求
     * @return List<Node>  路由结果
     */
    List<Node> select(Candidate candidate, RequestMessage<Invocation> request);
}
