package io.joyrpc.context.auth;

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

/**
 * IP访问权限控制配置
 */
public final class IPPermissionConfiguration extends AbstractInterfaceConfiguration<String, IPPermission> {

    /**
     * 访问需求
     */
    public static final IPPermissionConfiguration IP_PERMISSION = new IPPermissionConfiguration();

    /**
     * 判断时候合法请求
     *
     * @param className 接口
     * @param alias     分组别名
     * @param remoteIp  远程地址
     * @return boolean
     */
    public boolean permit(final String className, final String alias, final String remoteIp) {
        //获取接口黑白名单
        IPPermission permission = get(className);
        return permission == null || permission.permit(alias, remoteIp);
    }


}
