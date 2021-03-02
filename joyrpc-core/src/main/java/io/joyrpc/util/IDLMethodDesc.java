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
 * 方法的接口描述语言类型描述
 */
public class IDLMethodDesc {

    /**
     * 应答包装对象固定字段
     */
    public static final String F_RESULT = "result";
    /**
     * 请求类型
     */
    protected IDLType request;
    /**
     * 应答类型
     */
    protected IDLType response;

    /**
     * 构造函数
     *
     * @param request  请求包装
     * @param response 应答包装
     */
    public IDLMethodDesc(IDLType request, IDLType response) {
        this.request = request;
        this.response = response;
    }

    public IDLType getRequest() {
        return request;
    }

    public IDLType getResponse() {
        return response;
    }

}
