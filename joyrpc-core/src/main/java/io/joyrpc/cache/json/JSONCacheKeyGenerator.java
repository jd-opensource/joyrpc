package io.joyrpc.cache.json;

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

import io.joyrpc.cache.CacheKeyGenerator;
import io.joyrpc.exception.CacheException;
import io.joyrpc.exception.SerializerException;
import io.joyrpc.extension.Extension;
import io.joyrpc.protocol.message.Call;

import static io.joyrpc.Plugin.JSON;

/**
 * 参数转json
 */
@Extension(value = "json")
public class JSONCacheKeyGenerator implements CacheKeyGenerator {

    @Override
    public Object generate(final Call invocation) throws CacheException {
        Object[] args = invocation.getArgs();
        if (args == null || args.length == 0) {
            return "";
        }
        try {
            return JSON.get().toJSONString(args);
        } catch (SerializerException e) {
            throw new CacheException("Error occurs while generating cache key", e);
        }
    }

}
