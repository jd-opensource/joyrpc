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

import java.util.function.Function;

/**
 * 支持重试路由分发
 *
 * @param <T>
 * @param <R>
 */
public interface RouteFailover<T, R> extends Route<T, R> {
    /**
     * 设置重试次数
     *
     * @param retryFunction 重试函数
     */
    void setRetryFunction(Function<T, FailoverPolicy<T, R>> retryFunction);
}
