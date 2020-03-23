package io.joyrpc.config;

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

import io.joyrpc.cache.Cache;
import io.joyrpc.cache.CacheKeyGenerator;
import io.joyrpc.cluster.distribution.FailoverPolicy;
import io.joyrpc.cluster.distribution.Route;
import io.joyrpc.context.auth.IPPermission;
import io.joyrpc.permission.BlackWhiteList;

import javax.validation.Validator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 接口运行时选项，抽取出接口是方便第三方在扩展Filter的时候也可以扩展实现方法选项，用于提前绑定相关参数
 */
public interface InterfaceOption {

    /**
     * 根据方法名称返回选项
     *
     * @param methodName 方法名称
     * @return 选项
     */
    MethodOption getOption(String methodName);

    /**
     * 关闭，释放资源，例如移除监听器
     */
    default void close() {

    }


    /**
     * 方法选项
     */
    interface MethodOption {

        /**
         * 方法级别隐式传参，合并了接口的隐藏参数
         *
         * @return 合并了接口的方法级隐式传参数
         */
        Map<String, ?> getImplicits();

        /**
         * 获取超时时间
         *
         * @return 超时时间
         */
        int getTimeout();

        /**
         * 获取并行度
         *
         * @return 并行度
         */
        int getForks();

        /**
         * 获取分发策略
         *
         * @return 分发策略
         */
        Route getRoute();

        /**
         * 并发配置
         *
         * @return 并发配置
         */
        Concurrency getConcurrency();

        /**
         * 故障切换策略
         *
         * @return 故障切换策略
         */
        FailoverPolicy getFailoverPolicy();

        /**
         * 缓存策略
         *
         * @return 缓存策略
         */
        CachePolicy getCachePolicy();

        /**
         * 方法黑白名单
         *
         * @return 方法黑白名单
         */
        BlackWhiteList<String> getMethodBlackWhiteList();

        /**
         * 参数验证
         *
         * @return 参数验证
         */
        Validator getValidator();

        /**
         * 令牌
         *
         * @return 令牌
         */
        String getToken();

        /**
         * 获取IP限制
         *
         * @return IP限制
         */
        IPPermission getIPPermission();

    }

    /**
     * 并发数指标
     */
    class Concurrency {

        /**
         * 最大并发数
         */
        protected int max;

        /**
         * 活动并发
         */
        protected AtomicLong actives = new AtomicLong();

        public Concurrency(int max) {
            this.max = max;
        }

        public int getMax() {
            return max;
        }

        /**
         * 当前并发数
         *
         * @return
         */
        public long getActives() {
            return actives.get();
        }

        /**
         * 增加
         */
        public void add() {
            actives.incrementAndGet();
        }

        /**
         * 减少并发数
         */
        public void decrement() {
            actives.decrementAndGet();
        }

        /**
         * 唤醒
         */
        public void wakeup() {
            synchronized (this) {
                // 调用结束 通知等待的人
                notifyAll();
            }
        }

        /**
         * 等到
         *
         * @param time
         * @return
         */
        public boolean await(final long time) {
            if (time <= 0) {
                return true;
            }
            synchronized (this) {
                try {
                    // 等待执行
                    wait(time);
                    return true;
                } catch (InterruptedException e) {
                    return false;
                }
            }
        }

    }

    /**
     * 缓存策略
     */
    class CachePolicy {
        /**
         * 缓存接口
         */
        protected final Cache<Object, Object> cache;
        /**
         * 缓存键生成器
         */
        protected final CacheKeyGenerator generator;

        public CachePolicy(Cache<Object, Object> cache, CacheKeyGenerator generator) {
            this.cache = cache;
            this.generator = generator;
        }

        public Cache<Object, Object> getCache() {
            return cache;
        }

        public CacheKeyGenerator getGenerator() {
            return generator;
        }
    }

}
