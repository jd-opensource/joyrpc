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

import io.joyrpc.context.RequestContext;
import io.joyrpc.extension.Extensible;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.transport.session.Session.RpcSession;

/**
 * 上下文透传接口，包括注入和恢复
 */
@Extensible("transmit")
public interface Transmit extends ReqInjection {

    /**
     * 服务提供者收到调用请求对象时候，恢复上下文
     *
     * @param request 请求
     * @param session 会话
     */
    void restoreOnReceive(RequestMessage<Invocation> request, RpcSession session);

    /**
     * 服务消费者异步调用完成后，在线程切换的时候进行恢复
     *
     * @param request 请求
     */
    void restoreOnComplete(RequestMessage<Invocation> request);

    /**
     * 本地调用前，修改上下文
     *
     * @param source 当前上下文
     * @param target 目标上下文
     */
    void injectLocal(RequestContext source, RequestContext target);

}
