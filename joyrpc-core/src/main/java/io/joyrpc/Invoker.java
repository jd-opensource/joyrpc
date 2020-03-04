package io.joyrpc;

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

import io.joyrpc.constants.Constants;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.extension.MapParametric;
import io.joyrpc.extension.Parametric;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

import java.util.concurrent.CompletableFuture;

/**
 * 调用器
 *
 * @date: 9/1/2019
 */
@FunctionalInterface
public interface Invoker {

    /**
     * 调用
     *
     * @param request 请求
     * @return
     */
    CompletableFuture<Result> invoke(RequestMessage<Invocation> request);

    /**
     * 关闭
     *
     * @return
     */
    default CompletableFuture<Void> close() {
        Parametric parametric = new MapParametric(GlobalContext.getContext());
        return close(parametric.getBoolean(Constants.GRACEFULLY_SHUTDOWN_OPTION));
    }

    /**
     * 关闭
     *
     * @param gracefully 是否优雅关闭
     * @return
     */
    default CompletableFuture<Void> close(boolean gracefully) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 名称
     *
     * @return
     */
    default String getName() {
        return null;
    }

}
