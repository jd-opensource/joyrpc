package io.joyrpc.transport.netty4.util;

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

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.concurrent.CompletableFuture;

/**
 * Future包装器
 */
public class FutureAdapter<T> implements GenericFutureListener<Future<Void>> {

    /**
     * Future
     */
    protected final CompletableFuture<T> target;
    /**
     * 对象
     */
    protected final T object;

    public FutureAdapter(final CompletableFuture<T> target) {
        this.target = target;
        this.object = null;
    }

    public FutureAdapter(final CompletableFuture<T> target, final T object) {
        this.target = target;
        this.object = object;
    }

    @Override
    public void operationComplete(final Future<Void> future) throws Exception {
        if (future.isSuccess()) {
            target.complete(object);
        } else {
            target.completeExceptionally(future.cause());
        }
    }
}
