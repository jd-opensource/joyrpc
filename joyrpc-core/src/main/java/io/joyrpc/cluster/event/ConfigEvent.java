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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;


/**
 * 配置变更事件
 */
public class ConfigEvent extends UpdateEvent<Map<String, String>> {

    /**
     * 构造函数
     *
     * @param source
     * @param target
     * @param type
     * @param version
     * @param datum
     */
    public ConfigEvent(final Object source, final Object target, final UpdateType type,
                       final long version, final Map<String, String> datum) {
        super(source, target, type, version, datum);
    }

    /**
     * 数据是否为空
     *
     * @return
     */
    public boolean isEmpty() {
        return datum == null || datum.isEmpty();
    }

    /**
     * 获取修改的值
     *
     * @param exclude 过滤掉的Key
     * @param old     获取老的值
     * @return
     */
    public Map<String, String> changed(final Predicate<String> exclude, final Function<String, String> old) {
        int size = datum == null ? 0 : datum.size();
        Map<String, String> result = new HashMap<>(size);
        if (size > 0) {
            String key;
            String newValue;
            for (Map.Entry<String, String> entry : datum.entrySet()) {
                key = entry.getKey();
                newValue = entry.getValue();
                if (exclude == null || !exclude.test(key)) {
                    if (!Objects.equals(old.apply(key), newValue)) {
                        result.put(key, newValue);
                    }
                }
            }
        }
        return result;
    }

}
