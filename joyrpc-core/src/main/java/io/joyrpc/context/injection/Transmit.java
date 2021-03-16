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

import io.joyrpc.Result;
import io.joyrpc.annotation.Ignore;
import io.joyrpc.extension.Extensible;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

/**
 * 上下文透传接口，包括注入和恢复
 */
@Extensible("transmit")
public interface Transmit extends ReqInjection {

    String METHOD_ON_RETURN = "onReturn";

    String METHOD_ON_COMPLETE = "onComplete";

    String METHOD_ON_SERVER_RETURN = "onServerReturn";

    String METHOD_ON_SERVER_COMPLETE = "onServerComplete";

    /**
     * 客户端方法调用结束
     *
     * @param request 请求
     */
    @Ignore
    default void onReturn(final RequestMessage<Invocation> request) {

    }

    /**
     * 消费者异步调用结果返回后或异常
     *
     * @param request 请求
     * @param result  结果
     */
    @Ignore
    default void onComplete(final RequestMessage<Invocation> request, final Result result) {

    }

    /**
     * 服务提供者收到调用请求对象，恢复上下文
     *
     * @param request 请求
     */
    void onServerReceive(RequestMessage<Invocation> request);

    /**
     * 服务提供者方法调用结束
     *
     * @param request 请求
     */
    @Ignore
    default void onServerReturn(final RequestMessage<Invocation> request) {

    }

    /**
     * 服务提供者异步调用结果返回或异常
     *
     * @param request 请求
     * @param result  结果
     */
    @Ignore
    default void onServerComplete(final RequestMessage<Invocation> request, final Result result) {

    }

}
