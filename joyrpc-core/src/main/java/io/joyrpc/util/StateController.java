package io.joyrpc.util;

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


import java.util.concurrent.CompletableFuture;

/**
 * 状态控制器，用于处理打开和关闭的业务逻辑
 */
public interface StateController<T> {

    /**
     * 打开
     *
     * @return CompletableFuture
     */
    CompletableFuture<T> open();

    /**
     * 优雅关闭
     *
     * @param gracefully 优雅关闭标识
     * @return CompletableFuture
     */
    CompletableFuture<T> close(boolean gracefully);

    /**
     * 关闭前进行中断
     */
    default void fireClose() {

    }

    /**
     * 增强状态控制器
     *
     * @param <T>
     */
    interface ExStateController<T> extends StateController<T> {

        /**
         * 导出
         *
         * @return CompletableFuture
         */
        CompletableFuture<T> export();
    }

}
