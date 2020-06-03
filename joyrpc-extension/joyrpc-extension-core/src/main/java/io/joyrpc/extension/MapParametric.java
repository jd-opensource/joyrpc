/**
 *
 */
package io.joyrpc.extension;

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

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Map参数
 */
public class MapParametric<K extends CharSequence, V> extends AbstractParametric {
    /**
     * 参数
     */
    protected final Map<K, V> parameters;

    protected MapParametric() {
        parameters = null;
    }

    public MapParametric(Map<K, V> parameters) {
        this.parameters = parameters;
    }

    @Override
    public <T> T getObject(final String key) {
        return parameters == null ? null : (T) parameters.get(key);
    }

    @Override
    public void foreach(final BiConsumer<String, Object> consumer) {
        if (consumer != null && parameters != null) {
            parameters.forEach((k, v) -> consumer.accept(k.toString(), v));
        }
    }
}
