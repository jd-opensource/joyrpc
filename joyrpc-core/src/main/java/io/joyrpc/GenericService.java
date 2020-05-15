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


import io.joyrpc.exception.RpcException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

/**
 * 泛化调用接口，由于没有目标类，复杂参数对象采用Map进行传输
 * 泛型调用不会出现Callback
 */
public interface GenericService {

    /**
     * 判断是否是泛化类型
     */
    Predicate<Class<?>> GENERIC = GenericService.class::isAssignableFrom;

    /**
     * 泛化调用，和老接口兼容
     *
     * @param method         方法名
     * @param parameterTypes 参数类型
     * @param args           参数列表
     * @return 返回值
     */
    default Object $invoke(final String method, final String[] parameterTypes, final Object[] args) {
        try {
            return $async(method, parameterTypes, args).get(Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RpcException(String.format("Failed invoking %s, It's interrupted.", method), e);
        } catch (ExecutionException e) {
            Throwable throwable = e.getCause() == null ? e : e.getCause();
            if (throwable instanceof RpcException) {
                throw (RpcException) throwable;
            }
            throw new RpcException(String.format("Failed invoking %s, caused by %s", method, throwable.getMessage()), throwable);
        } catch (TimeoutException e) {
            throw new RpcException(String.format("Failed invoking %s, It's timeout.", method), e);
        }
    }

    /**
     * 异步泛化调用
     *
     * @param method         方法名
     * @param parameterTypes 参数类型
     * @param args           参数列表
     * @return 返回值
     */
    CompletableFuture<Object> $async(String method, String[] parameterTypes, Object[] args);

}
