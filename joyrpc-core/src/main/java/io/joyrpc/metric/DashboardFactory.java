package io.joyrpc.metric;

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
import io.joyrpc.metric.Dashboard.DashboardType;

/**
 * Dashboard工厂类
 */
@FunctionalInterface
@Extensible("dashboardFactory")
public interface DashboardFactory {

    /**
     * 创建面板
     *
     * @param url  URL
     * @param type 类型
     * @return 面板
     */
    Dashboard create(URL url, DashboardType type);

}
