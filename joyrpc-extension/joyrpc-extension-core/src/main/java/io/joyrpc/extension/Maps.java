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

import java.util.concurrent.ConcurrentMap;

/**
 * Map工具类
 */
public abstract class Maps {

    /**
     * 不存在的时候创建
     *
     * @param map
     * @param key
     * @param function
     * @param <M>
     * @param <T>
     * @return
     */
    public static <M, T> T computeIfAbsent(final ConcurrentMap<M, T> map, final M key, final Function<M, T> function) {
        T result = map.get(key);
        if (result == null) {
            result = function.apply(key);
            T exists = map.putIfAbsent(key, result);
            if (exists != null) {
                result = exists;
            }
        }
        return result;
    }

    /**
     * 函数
     */
    interface Function<M, T> {

        /**
         * Applies this function to the given argument.
         *
         * @param t the function argument
         * @return the function result
         */
        T apply(M t);

    }
}
