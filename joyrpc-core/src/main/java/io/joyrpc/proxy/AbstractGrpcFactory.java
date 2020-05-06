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
    public GrpcType generate(final Class<?> clz, final Method method, final Supplier<String> suffix) throws ProxyException {
        String methodName = method.getName();
        try {
            ClassWrapper request = getRequestWrapper(clz, method, new Naming(clz, method, REQUEST_SUFFIX, suffix));
            ClassWrapper response = getResponseWrapper(clz, method, new Naming(clz, method, RESPONSE_SUFFIX, suffix));
            return new GrpcType(request, response);
        } catch (ProxyException e) {
            throw e;
        } catch (Throwable e) {
            throw new ProxyException(String.format("Error occurs while building grpcType of %s.%s",
                    clz.getName(), method.getName()), e);
        }
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
    protected ClassWrapper getResponseWrapper(final Class<?> clz, final Method method, final Naming naming) throws Exception {
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
    protected abstract Class<?> buildResponseClass(Class<?> clz, Method method, Naming naming) throws Exception;

    /**
     * 构建请求包装类型
     *
     * @param clz    类
     * @param method 方法
     * @param naming 方法名提供者
     * @return 包装的类
     * @throws Exception 异常
     */
    protected ClassWrapper getRequestWrapper(final Class<?> clz, final Method method, final Naming naming) throws Exception {
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
                return new ClassWrapper(buildRequestClass(clz, method, naming), true);
        }
    }

    /**
     * 构建请求类型
     *
     * @param clz    类
     * @param method 方法
     * @param naming 方法名提供者
     * @return
     * @throws Exception
     */
    protected abstract Class<?> buildRequestClass(final Class<?> clz, final Method method, final Naming naming) throws Exception;

    /**
     * 是否是POJO类
     *
     * @param clazz 类
     * @retrn
     */
    protected boolean isPojo(final Class<?> clazz) {
        return !isJavaClass(clazz);
    }

    /**
     * 名称
     */
    protected static class Naming {

        /**
         * 类
         */
        protected final Class<?> clazz;
        /**
         * 方法
         */
        protected final Method method;
        /**
         * 固定后缀名
         */
        protected final String fixSuffix;
        /**
         * 随机后缀名提供者
         */
        protected final Supplier<String> randomSuffix;
        /**
         * 随机后缀名
         */
        protected String random;

        public Naming(Class<?> clazz, Method method, String fixSuffix, Supplier<String> randomSuffix) {
            this.clazz = clazz;
            this.method = method;
            this.fixSuffix = fixSuffix;
            this.randomSuffix = randomSuffix;
        }

        protected String getRandom() {
            if (random == null) {
                random = randomSuffix == null ? "" : randomSuffix.get();
            }
            return random;
        }

        /**
         * 获取全路径名称
         *
         * @return 全路径名称
         */
        public String getFullName() {
            String methodName = method.getName();
            return new StringBuilder(100).
                    append(clazz.getName()).append('$').
                    append(Character.toUpperCase(methodName.charAt(0))).append(methodName.substring(1)).
                    append(fixSuffix).append(getRandom()).
                    toString();
        }

        /**
         * 获取简单名称
         *
         * @return 简单名称
         */
        public String getSimpleName() {
            String methodName = method.getName();
            return new StringBuilder(100).
                    append(clazz.getSimpleName()).append('$').
                    append(Character.toUpperCase(methodName.charAt(0))).append(methodName.substring(1)).
                    append(fixSuffix).append(getRandom()).
                    toString();
        }

    }

}
