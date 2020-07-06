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

import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

/**
 * 隐式参数配置
 */
public interface ReqInjection {

    /**
     * 绑定上下文到调用对象
     *
     * @param request 请求
     */
    void inject(RequestMessage<Invocation> request);

    /**
     * 取消绑定的上下文，用在重试，不同的节点有不同的协议，注入不同的隐式参数
     *
     * @param request 请求
     */
    default void reject(RequestMessage<Invocation> request) {

    }
}
