package io.joyrpc.filter;

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

import io.joyrpc.Invoker;
import io.joyrpc.Result;
import io.joyrpc.cache.Cache;
import io.joyrpc.cache.CacheConfig;
import io.joyrpc.cache.CacheFactory;
import io.joyrpc.cache.CacheObject;
import io.joyrpc.constants.Constants;
import io.joyrpc.exception.CacheException;
import io.joyrpc.extension.Converts;
import io.joyrpc.extension.URL;
import io.joyrpc.filter.cache.CacheKeyGenerator;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.util.GenericMethodOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static io.joyrpc.Plugin.CACHE;
import static io.joyrpc.Plugin.CACHE_KEY_GENERATOR;
import static io.joyrpc.constants.Constants.*;


/**
 * consumer结果缓存过滤器, 需要扩展实现Cache接口
 */
public class AbstractCacheFilter extends AbstractFilter {

    private static final Logger logger = LoggerFactory.getLogger(AbstractCacheFilter.class);
    /**
     * 方法缓存配置元数据
     */
    protected GenericMethodOption<Optional<CacheMeta>> caches;

    @Override
    public void setup() {
        caches = new GenericMethodOption<>(clazz, className, (methodName) -> Optional.ofNullable(buildCacheMeta(methodName)));
    }

    /**
     * 获取缓存
     *
     * @param methodName
     * @return
     */
    protected CacheMeta buildCacheMeta(final String methodName) {
        //判断是否开启了缓存
        boolean enable = url.getBoolean(getOption(methodName, Constants.CACHE_OPTION));
        if (!enable) {
            return null;
        }
        String keyGenName = url.getString(getOption(methodName, Constants.CACHE_KEY_GENERATOR_OPTION));
        String cacheFactoryName = url.getString(CACHE_PROVIDER_OPTION);
        //获取缓存键生成器
        CacheKeyGenerator generator = CACHE_KEY_GENERATOR.get(keyGenName);
        //获取缓存实现
        CacheFactory cacheFactory = CACHE.get(cacheFactoryName);
        if (generator == null || cacheFactory == null) {
            return null;
        } else {
            boolean cacheNullable = url.getBoolean(getOption(methodName, CACHE_NULLABLE_OPTION));
            //创建缓存
            CacheConfig<Object, Object> cacheConfig = CacheConfig.builder().nullable(cacheNullable).
                    capacity(url.getInteger(getOption(methodName, CACHE_CAPACITY_OPTION))).
                    expireAfterWrite(url.getInteger(getOption(methodName, CACHE_EXPIRE_TIME_OPTION))).
                    build();
            Cache<Object, Object> cache = cacheFactory.build(methodName, cacheConfig);
            return new CacheMeta(cache, generator, cacheNullable);
        }
    }

    @Override
    public CompletableFuture<Result> invoke(final Invoker invoker, final RequestMessage<Invocation> request) {
        //获取缓存配置
        Invocation invocation = request.getPayLoad();
        Optional<CacheMeta> op = caches.get(invocation.getMethodName());
        CacheMeta meta = op == null ? null : op.orElse(null);
        Object key = meta == null ? null : meta.getKey(invocation);
        //判断是否开启缓存
        if (key != null) {
            final CompletableFuture<Result> future = new CompletableFuture<>();
            //获取缓存
            CompletableFuture<CacheObject<Object>> cacheFuture = meta.getCache(key);
            cacheFuture.whenComplete((cache, t) -> {
                if (t == null && cache != null) {
                    future.complete(new Result(request.getContext(), cache.getResult()));
                } else {
                    //没有拿到缓存
                    if (t != null) {
                        //有异常
                        logger.error(t.getMessage(), t);
                    }
                    //未命中发起远程调用
                    CompletableFuture<Result> invokerFuture = invoker.invoke(request);
                    invokerFuture.whenComplete((result, r) -> {
                        if (r != null) {
                            //远程调用异常
                            future.completeExceptionally(r);
                        } else {
                            future.complete(result);
                            if (!result.isException()) {
                                //缓存非异常结果
                                meta.putCache(key, result.getValue());
                            }
                        }
                    });
                }
            });
            return future;
        } else {
            // 该方法未开启缓存
            return invoker.invoke(request);
        }
    }

    @Override
    public boolean test(final URL url) {
        // 参数校验过滤器
        if (url.getBoolean(Constants.CACHE_OPTION)) {
            return true;
        }
        Map<String, String> caches = url.endsWith("." + Constants.CACHE_OPTION.getName());
        if (caches == null || caches.isEmpty()) {
            return false;
        }
        for (String value : caches.values()) {
            if (Converts.getBoolean(value, Boolean.FALSE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 缓存元数据
     */
    protected static class CacheMeta {
        /**
         * 缓存接口
         */
        protected Cache<Object, Object> cache;
        /**
         * 缓存键生成器
         */
        protected CacheKeyGenerator generator;

        protected boolean cacheNullable;

        public CacheMeta(Cache<Object, Object> cache, CacheKeyGenerator generator, boolean cacheNullable) {
            this.cache = cache;
            this.generator = generator;
            this.cacheNullable = cacheNullable;
        }

        public Cache<Object, Object> getCache() {
            return cache;
        }

        public CacheKeyGenerator getGenerator() {
            return generator;
        }

        /**
         * 生成Key
         *
         * @param invocation
         * @return
         */
        public Object getKey(final Invocation invocation) {
            try {
                return invocation == null ? null : generator.generate(invocation);
            } catch (CacheException e) {
                logger.error(e.getMessage(), e);
                return null;
            }
        }

        /**
         * 获取缓存
         *
         * @param key
         * @return
         */
        public CompletableFuture<CacheObject<Object>> getCache(final Object key) {
            return cache.get(key);
        }

        /**
         * 修改缓存
         *
         * @param key
         * @param value
         * @return
         */
        public CompletableFuture<Void> putCache(final Object key, final Object value) {
            if (!cacheNullable && value == null) {
                return CompletableFuture.completedFuture(null);
            }
            return cache.put(key, value);
        }
    }
}
