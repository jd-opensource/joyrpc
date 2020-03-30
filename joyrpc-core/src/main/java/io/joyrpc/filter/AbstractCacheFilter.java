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
import io.joyrpc.config.InterfaceOption.CachePolicy;
import io.joyrpc.constants.Constants;
import io.joyrpc.exception.CacheException;
import io.joyrpc.extension.Converts;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;


/**
 * consumer结果缓存过滤器, 需要扩展实现Cache接口
 */
public class AbstractCacheFilter extends AbstractFilter {

    private static final Logger logger = LoggerFactory.getLogger(AbstractCacheFilter.class);

    @Override
    public CompletableFuture<Result> invoke(final Invoker invoker, final RequestMessage<Invocation> request) {
        final CachePolicy policy = request.getOption().getCachePolicy();
        if (policy == null) {
            return invoker.invoke(request);
        }
        final Cache<Object, Object> cache = policy.getCache();
        final Object key = getKey(policy, request.getPayLoad());
        if (key == null) {
            return invoker.invoke(request);
        }

        final CompletableFuture<Result> result = new CompletableFuture<>();
        //获取缓存
        cache.get(key).whenComplete((c, t) -> {
            if (t == null && c != null) {
                result.complete(new Result(request.getContext(), c.getResult()));
            } else {
                //没有拿到缓存
                if (t != null) {
                    //有异常
                    logger.error("Error occurs while reading cache,caused by " + t.getMessage(), t);
                }
                //未命中发起远程调用
                invoker.invoke(request).whenComplete((r, error) -> {
                    if (error != null) {
                        //远程调用异常
                        result.completeExceptionally(error);
                    } else {
                        if (!r.isException()) {
                            //缓存非异常结果
                            cache.put(key, r.getValue());
                        }
                        result.complete(r);
                    }
                });
            }
        });
        return result;
    }

    /**
     * 生成缓存键
     *
     * @param policy     缓存策略
     * @param invocation 键
     * @return 缓存键
     */
    protected Object getKey(CachePolicy policy, Invocation invocation) {
        try {
            return policy == null ? null : policy.getGenerator().generate(invocation);
        } catch (CacheException e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean test(final URL url) {
        // 参数校验过滤器
        if (url.getBoolean(Constants.CACHE_OPTION)) {
            return true;
        }
        Map<String, String> caches = url.endsWith("." + Constants.CACHE_OPTION.getName());
        if (caches.isEmpty()) {
            return false;
        }
        for (String value : caches.values()) {
            if (Converts.getBoolean(value, Boolean.FALSE)) {
                return true;
            }
        }
        return false;
    }
}
