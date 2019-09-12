package io.joyrpc.context.mock;

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

import io.joyrpc.context.AbstractInterfaceConfiguration;

import java.util.Map;

/**
 * Title: Mock数据工程类<br>
 * <p/>
 * Description: 保留了配置mock的接口方法调用结果，提供更新和读取方法<br>
 * <p/>
 */
public class MockConfiguration extends AbstractInterfaceConfiguration<String, Map<String, Map<String, Object>>> {

    /**
     * 访问需求
     */
    public static final MockConfiguration MOCK = new MockConfiguration();

    /**
     * 读取Mock数据,参数是类名和方法名字符串，是为了兼容泛化调用，泛化调用的时候可能拿不到类
     *
     * @param className  类名
     * @param methodName 方法名
     * @param alias      服务别名
     * @return 结果
     */
    public Object get(final String className, final String methodName, final String alias) {
        if (className == null || methodName == null || alias == null) {
            return null;
        } else {
            Map<String, Map<String, Object>> classMock = get(className);
            if (classMock == null) {
                return null;
            }
            Map<String, Object> methodMock = classMock.get(methodName);
            return methodMock == null ? null : methodMock.get(alias);
        }
    }
}
