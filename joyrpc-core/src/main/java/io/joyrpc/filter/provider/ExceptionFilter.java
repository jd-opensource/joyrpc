package io.joyrpc.filter.provider;

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

import io.joyrpc.Invoker;
import io.joyrpc.Result;
import io.joyrpc.exception.LafException;
import io.joyrpc.exception.RpcException;
import io.joyrpc.extension.Extension;
import io.joyrpc.filter.AbstractProviderFilter;
import io.joyrpc.filter.ProviderFilter;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.MessageHeader;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.util.StringUtils;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

import static io.joyrpc.Plugin.JSON;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.util.ClassUtils.getCodeBase;
import static io.joyrpc.util.ClassUtils.isJavaClass;

/**
 * @description: 异常过滤器
 * 1.如果抛出的异常方法上有声明则返回<br>
 * 2.如果是一些已知异常，则返回<br>
 * 3.未知异常，保证为RuntimeException返回<br>
 */
@Extension(value = "exception", order = ProviderFilter.EXCEPTION_ORDER)
public class ExceptionFilter extends AbstractProviderFilter {

    @Override
    public CompletableFuture<Result> invoke(final Invoker invoker, final RequestMessage<Invocation> request) {
        // 调用成功，或者调用返回已经封装的response
        return invoker.invoke(request).thenApply(result -> convert(result, request));
    }

    /**
     * 进行转换
     *
     * @param result
     * @param request
     * @return
     */
    protected Result convert(final Result result, final RequestMessage<Invocation> request) {
        Invocation invocation = request.getPayLoad();
        if (!result.isException() || invocation.isGeneric()) {
            return result;
        }
        // 解析exception
        Throwable exception = result.getException();

        // 跨语言 特殊处理。
        MessageHeader header = request.getHeader();
        if (header.removeAttribute(HEAD_SRC_LANGUAGE) != null) {
            // 标记结果为错误
            header.addAttribute(HEAD_RESPONSE_CODE, (byte) 1);
            // 转为字符串
            String json = JSON.get().toJSONString(result.getException());
            return new Result(request.getContext(), json);
        } else if (isJavaClass(exception.getClass())) {
            // 是JDK自带的异常，直接抛出
            return result;
        } else if (exception instanceof LafException) {
            // 是本身的异常，直接抛出
            return result;
        } else if (invocation.isGeneric()) {
            // 泛化调用调用
            return new Result(request.getContext(), new RpcException(StringUtils.toString(exception)));
        } else if (header.getAttribute(HEAD_GENERIC.getKey(), (byte) 0) > 0) {
            // 兼容老版本的网关调用
            return new Result(request.getContext(), new RpcException(StringUtils.toString(exception)));
        } else if (!(exception instanceof RuntimeException) && (exception instanceof Exception)) {
            // 如果是checked异常，直接抛出
            return result;
        } else if (isDeclared(invocation.getMethod(), exception)) {
            // 在方法签名上有声明，直接抛出
            return result;
        } else if (isSameJar(invocation.getClazz(), exception)) {
            // 异常类和接口类在同一jar包里，直接抛出
            return result;
        } else {
            // 否则，包装成RuntimeException抛给客户端
            return new Result(request.getContext(), new RuntimeException(StringUtils.toString(exception)));
        }
    }

    /**
     * 判断是否在同一个Jar里面
     *
     * @param clazz
     * @param exception
     * @return
     */
    protected boolean isSameJar(final Class clazz, final Throwable exception) {
        String serviceFile = getCodeBase(clazz);
        String exceptionFile = getCodeBase(exception.getClass());
        return serviceFile == null || exceptionFile == null || serviceFile.equals(exceptionFile);
    }

    /**
     * 判断异常是否在方法上声明了
     *
     * @param method
     * @param exception
     * @return
     */
    protected boolean isDeclared(final Method method, final Throwable exception) {
        Class<?>[] exceptionTypes = method.getExceptionTypes();
        for (Class<?> exceptionType : exceptionTypes) {
            if (exception.getClass().equals(exceptionType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int type() {
        return INNER;
    }
}
