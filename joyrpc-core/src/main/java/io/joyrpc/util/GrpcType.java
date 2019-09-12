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

/**
 * Grpc方法参数和结果类型
 */
public class GrpcType {

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
     * @param request
     * @param response
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
        protected Class clazz;
        /**
         * 包装请求
         */
        protected boolean wrapper;

        public ClassWrapper(Class clazz, boolean wrapper) {
            this.clazz = clazz;
            this.wrapper = wrapper;
        }

        public Class getClazz() {
            return clazz;
        }

        public boolean isWrapper() {
            return wrapper;
        }
    }
}
