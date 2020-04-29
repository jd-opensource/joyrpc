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

import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * Grpc方法信息
 */
public class GrpcMethod {
    /**
     * 方法
     */
    protected Method method;
    /**
     * 类型提供者
     */
    protected Supplier<GrpcType> supplier;

    public GrpcMethod(Method method, Supplier<GrpcType> supplier) {
        this.method = method;
        this.supplier = supplier;
    }

    public Method getMethod() {
        return method;
    }

    public Supplier<GrpcType> getSupplier() {
        return supplier;
    }

    public GrpcType getType() {
        return supplier.get();
    }
}
