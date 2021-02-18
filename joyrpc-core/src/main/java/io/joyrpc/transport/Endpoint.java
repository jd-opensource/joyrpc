package io.joyrpc.transport;

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


import io.joyrpc.util.State;

import java.util.concurrent.CompletableFuture;

/**
 * 端点
 */
public interface Endpoint<M> {

    /**
     * 打开
     *
     * @return CompletableFuture
     */
    CompletableFuture<M> open();

    /**
     * 关闭
     *
     * @return CompletableFuture
     */
    CompletableFuture<M> close();

    /**
     * 获取当前状态
     *
     * @return 状态查询
     */
    State getState();

}
