package io.joyrpc.context.circuit;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 跨机房访问优先访问的机房
 */
public class CircuitConfiguration {

    public static final CircuitConfiguration INSTANCE = new CircuitConfiguration();

    /**
     * 结果缓存
     */
    protected volatile Map<String, List<String>> CIRCUITS = new HashMap<>();

    /**
     * 读取跨机房访问优先访问的机房
     *
     * @param dataCenter
     * @return 结果
     */
    public List<String> get(final String dataCenter) {
        return dataCenter == null ? null : CIRCUITS.get(dataCenter);
    }

    /**
     * 修改配置
     *
     * @param circuits
     */
    public synchronized void update(final Map<String, List<String>> circuits) {
        if (circuits != null) {
            CIRCUITS = circuits;
        }
    }

}
