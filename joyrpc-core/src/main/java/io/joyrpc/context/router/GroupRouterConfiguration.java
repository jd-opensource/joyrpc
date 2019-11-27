package io.joyrpc.context.router;

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
 * 分组路由配置信息
 */
public class GroupRouterConfiguration extends AbstractInterfaceConfiguration<String, Map<String, Map<String, String>>> {

    /**
     * 结果缓存
     */
    public static final GroupRouterConfiguration GROUP_ROUTER = new GroupRouterConfiguration();


    /**
     * 读取路由的分组
     *
     * @param className 类名
     * @param method    接口
     * @param param     路由参数
     * @return 结果
     */
    public String get(final String className, final String method, final String param) {
        if (className == null || method == null || param == null) {
            return null;
        } else {
            Map<String, Map<String, String>> routers = configs.get(className);
            if (routers == null) {
                return null;
            }
            Map<String, String> methodRouters = routers.get(method);
            String alias = methodRouters == null ? null : methodRouters.get(param);
            if (alias == null || alias.isEmpty()) {
                methodRouters = routers.get("*");
                alias = methodRouters == null ? null : methodRouters.get(param);
            }
            return alias;
        }
    }

}
