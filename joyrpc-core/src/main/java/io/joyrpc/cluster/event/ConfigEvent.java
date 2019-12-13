package io.joyrpc.cluster.event;

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

import io.joyrpc.event.UpdateEvent;

import java.util.Map;


/**
 * 配置变更事件，只支持全量
 */
public class ConfigEvent extends UpdateEvent<Map<String, String>> {

    /**
     * 构造函数
     *
     * @param source
     * @param target
     * @param version
     * @param datum
     */
    public ConfigEvent(final Object source, final Object target, final long version, final Map<String, String> datum) {
        super(source, target, UpdateType.FULL, version, datum);
    }

    /**
     * 数据是否为空
     *
     * @return
     */
    public boolean isEmpty() {
        return datum == null || datum.isEmpty();
    }

}
