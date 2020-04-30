package io.joyrpc.proxy;

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

import io.joyrpc.exception.ProxyException;
import io.joyrpc.util.GrpcType;
import io.joyrpc.util.GrpcType.ClassWrapper;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static io.joyrpc.util.ClassUtils.isJavaClass;

/**
 * 抽象的gRPC工厂类
 */
public abstract class AbstractGrpcFactory implements GrpcFactory {

    public static final String REQUEST_SUFFIX = "Request";
    public static final String RESPONSE_SUFFIX = "Response";

    @Override
    public GrpcType generate(final Class<?> clz, final Method method) throws ProxyException {
        try {
            ClassWrapper request = getRequestWrapper(clz, method, () -> getSuffix(method, REQUEST_SUFFIX));
            ClassWrapper response = getResponseWrapper(clz, method, () -> getSuffix(method, RESPONSE_SUFFIX));
            return new GrpcType(request, response);
        } catch (ProxyException e) {
            throw e;
        } catch (Throwable e) {
            throw new ProxyException(String.format("Error occurs while building grpcType of %s.%s",
                    clz.getName(), method.getName()), e);
        }
    }

    /**
     * 获取后缀名称
     *
     * @param method 方法
     * @param suffix 后缀
     * @return 名称
     */
    protected String getSuffix(final Method method, final String suffix) {
        String methodName = method.getName();
        return new StringBuilder(100)
                .append(Character.toUpperCase(methodName.charAt(0)))
                .append(methodName.substring(1))
                .append(suffix)
                .toString();
    }

    /**
     * 获取应答包装类型
     *
     * @param clz    类
     * @param method 方法
     * @param naming 方法名称提供者
     * @return
     * @throws Exception
     */
    protected ClassWrapper getResponseWrapper(final Class<?> clz, final Method method, final Supplier<String> naming) throws Exception {
        Class clazz = method.getReturnType();
        if (CompletableFuture.class.isAssignableFrom(clz)) {
            //异步支持
            Type type = method.getGenericReturnType();
            Type[] actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
            type = actualTypeArguments[0];
            if (type instanceof Class) {
                clazz = (Class) type;
            } else {
                throw new ProxyException(String.format("unsupported generic type of %s.%s", clz.getName(), method.getName()));
            }
        }
        if (clazz == void.class) {
            return null;
        } else if (isPojo(clazz)) {
            return new ClassWrapper(clazz, false);
        } else {
            return new ClassWrapper(buildResponseClass(clz, method, naming), true);
        }
    }

    /**
     * 包装应答类型
     *
     * @param method 方法
     * @param naming 方法名称提供者
     * @return
     * @throws Exception
     */
    protected abstract Class<?> buildResponseClass(Class<?> clz, Method method, Supplier<String> naming) throws Exception;

    /**
     * 构建请求包装类型
     *
     * @param clz    类
     * @param method 方法
     * @param suffix 方法名后缀提供者
     * @return 包装的类
     * @throws Exception 异常
     */
    protected ClassWrapper getRequestWrapper(final Class<?> clz, final Method method, final Supplier<String> suffix) throws Exception {
        Parameter[] parameters = method.getParameters();
        switch (parameters.length) {
            case 0:
                return null;
            case 1:
                Class<?> clazz = parameters[0].getType();
                if (isPojo(clazz)) {
                    return new ClassWrapper(clazz, false);
                }
            default:
                return new ClassWrapper(buildRequestClass(clz, method, suffix), true);
        }
    }

    /**
     * 构建请求类型
     *
     * @param method 方法
     * @param suffix 方法名后缀提供者
     * @return
     * @throws Exception
     */
    protected abstract Class<?> buildRequestClass(final Class<?> clz, final Method method, final Supplier<String> suffix) throws Exception;

    /**
     * 是否是POJO类
     *
     * @param clazz 类
     * @return
     */
    protected boolean isPojo(final Class<?> clazz) {
        return !isJavaClass(clazz);
    }

}
