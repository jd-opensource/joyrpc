package io.joyrpc.filter.cache;

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

import io.joyrpc.exception.CacheException;
import io.joyrpc.exception.SerializerException;
import io.joyrpc.extension.Extension;
import io.joyrpc.protocol.message.Invocation;

import static io.joyrpc.Plugin.JSON;

/**
 * @description: 参数转json
 */
@Extension(value = "default")
public class DefaultCacheKeyGenerator implements CacheKeyGenerator {

    @Override
    public Object generate(final Invocation invocation) throws CacheException {
        try {
            return JSON.get().toJSONString(invocation.getArgs());
        } catch (SerializerException e) {
            throw new CacheException("Error occurs while generating cache key", e);
        }
    }

}
