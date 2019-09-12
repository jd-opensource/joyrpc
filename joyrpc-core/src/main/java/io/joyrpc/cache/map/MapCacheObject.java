package io.joyrpc.cache.map;

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

import io.joyrpc.cache.CacheObject;
import io.joyrpc.util.SystemClock;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存数据对象
 *
 * @param <V>
 */
public class MapCacheObject<V> extends CacheObject<V> {
    //过期时间，毫秒数
    protected long expireTime;
    //计数器
    protected AtomicLong counter = new AtomicLong(0);

    /**
     * 构造函数
     *
     * @param result
     */
    public MapCacheObject(V result) {
        super(result);
    }

    /**
     * 构造函数
     *
     * @param result
     * @param expireTime
     */
    public MapCacheObject(V result, long expireTime) {
        super(result);
        this.expireTime = expireTime;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public AtomicLong getCounter() {
        return counter;
    }

    /**
     * 是否过期
     *
     * @return
     */
    public boolean isExpire() {
        return expireTime > 0 && SystemClock.now() > expireTime;
    }

    /**
     * 是否过期
     *
     * @param now 当前毫秒数
     * @return
     */
    public boolean isExpire(long now) {
        return expireTime > 0 && now > expireTime;
    }
}
