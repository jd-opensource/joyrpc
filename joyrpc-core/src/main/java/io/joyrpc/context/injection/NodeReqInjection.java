package io.joyrpc.context.injection;

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

import io.joyrpc.cluster.Node;
import io.joyrpc.extension.Extensible;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

/**
 * 节点注入，满足条件的节点在指定环境下注入额外的信息
 */
@Extensible("nodeReqInjection")
public interface NodeReqInjection {

    /**
     * 测试当前环境是否开启
     *
     * @return
     */
    boolean test();

    /**
     * 绑定上下文到调用对象
     *
     * @param request 请求
     * @param node    当前节点
     */
    void inject(RequestMessage<Invocation> request, Node node);

    /**
     * 注销
     *
     * @param request 请求
     * @param node    前一个节点
     */
    default void reject(final RequestMessage<Invocation> request, final Node node) {

    }
}
