package io.joyrpc.cluster.discovery.config;

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
import io.joyrpc.extension.URL;


/**
 * 集群配置服务
 */
@Extensible("configure")
public interface Configure {

    /**
     * 订阅接口(主要指对一个URL变化的订阅)
     *
     * @param url     the url
     * @param handler the config event handler
     */
    boolean subscribe(URL url, ConfigHandler handler);

    /**
     * 取消订阅接口
     *
     * @param url     the url
     * @param handler
     */
    boolean unsubscribe(URL url, ConfigHandler handler);

}
