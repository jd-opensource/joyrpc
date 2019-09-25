package io.joyrpc.context;

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

import io.joyrpc.extension.Extensible;

import java.util.Map;

/**
 * 接口配置变化监听器
 *
 * @date: 2019/6/21
 */
@Extensible("configEventHandler")
public interface ConfigEventHandler {

    /**
     * 权限鉴别相关配置事件order，较高优先级
     */
    int ZERO_ORDER = 0;

    /**
     * 全局配置事件order
     */
    int GLOBAL_ORDER = 100;

    /**
     * 跨机房访问首选机房配置顺序
     */
    int CIRCUIT_ORDER = GLOBAL_ORDER - 1;

    /**
     * 接口级业务相关配置事件order
     */
    int BIZ_ORDER = 200;

    /**
     * 接口级全部配置事件order，由于可能覆盖业务相关配置，优先级排在最后
     */
    int IFACE_ORDER = Short.MAX_VALUE;

    /**
     * 处理配置
     *
     * @param className 接口类名
     * @param attrs     扩展属性
     */
    void handle(final String className, final Map<String, String> attrs);

}
