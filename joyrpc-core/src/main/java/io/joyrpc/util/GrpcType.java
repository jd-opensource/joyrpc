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

import java.util.function.Function;

/**
 * Grpc方法参数和结果类型
 */
public class GrpcType {

    /**
     * 应答包装对象固定字段
     */
    public static final String F_RESULT = "result";

    /**
     * 请求类型
     */
    protected ClassWrapper request;
    /**
     * 应答类型
     */
    protected ClassWrapper response;

    /**
     * 构造函数
     *
     * @param request  请求包装
     * @param response 应答包装
     */
    public GrpcType(ClassWrapper request, ClassWrapper response) {
        this.request = request;
        this.response = response;
    }

    public ClassWrapper getRequest() {
        return request;
    }

    public ClassWrapper getResponse() {
        return response;
    }

    /**
     * 类型包装器
     */
    public static final class ClassWrapper {

        /**
         * 请求类型
         */
        protected Class<?> clazz;
        /**
         * 包装请求
         */
        protected boolean wrapper;
        /**
         * 转换函数
         */
        protected GrpcConversion conversion;

        /**
         * 构造函数
         *
         * @param clazz   类型
         * @param wrapper 包装类型标识
         */
        public ClassWrapper(Class<?> clazz, boolean wrapper) {
            this.clazz = clazz;
            this.wrapper = wrapper;
            if (wrapper) {
                this.conversion = ClassUtils.getGrpcConversion(clazz);
            }
        }

        public Class<?> getClazz() {
            return clazz;
        }

        public boolean isWrapper() {
            return wrapper;
        }

        public GrpcConversion getConversion() {
            return conversion;
        }
    }

    /**
     * Grpc参数转换
     */
    public static final class GrpcConversion {
        /**
         * 参数转换成包装对象函数
         */
        protected Function<Object[], Object> toWrapper;
        /**
         * 包装对象转换成参数函数
         */
        protected Function<Object, Object[]> toParameter;

        public GrpcConversion(final Function<Object[], Object> toWrapper,
                              final Function<Object, Object[]> toParameter) {
            this.toWrapper = toWrapper;
            this.toParameter = toParameter;
        }

        public Function<Object[], Object> getToWrapper() {
            return toWrapper;
        }

        public Function<Object, Object[]> getToParameter() {
            return toParameter;
        }
    }
}
