package io.joyrpc.cluster.distribution.limiter;

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

import io.joyrpc.cluster.distribution.RateLimiter;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/**
 * 非阻塞平滑限流器,漏桶算法
 */
public class LeakyBucketRateLimiter implements RateLimiter {

    /**
     * 启动时间=限流器生效时间-限流周期
     */
    protected volatile long startTimeNanos;
    /**
     * 限流配置
     */
    protected volatile LeakyBucketLimiterConfig limiter;
    /**
     * 当前时间，及许可次数
     */
    protected final AtomicReference<Cycle> curCycle = new AtomicReference<>(new Cycle(0.0d, 0L, true));

    @Override
    public String type() {
        return "leakyBucket";
    }

    /**
     * 构造函数
     */
    public LeakyBucketRateLimiter() {
    }

    /**
     * 构造函数
     *
     * @param config
     */
    public LeakyBucketRateLimiter(final RateLimiterConfig config) {
        reload(config, false);
    }

    @Override
    public boolean getPermission() {
        Cycle current;
        Cycle next;
        double curPermissions;
        long lastPermissionMicros;
        boolean permitted;
        long curMicros;
        double newPermissions;
        LeakyBucketLimiterConfig config;
        do {
            current = curCycle.get();
            curPermissions = current.curPermissions;
            lastPermissionMicros = current.lastPermissionMicros;
            //运行持续微妙时间
            curMicros = duration();
            if (curMicros > lastPermissionMicros) {
                config = limiter;
                //按照时间计算许可，时间越长，许可越多
                newPermissions = (curMicros - lastPermissionMicros) / config.intervalMicros;
                //最大限制，防止长时间没有调用，上面的算法累积了很多许可
                curPermissions = Math.min(config.limitCount, curPermissions + newPermissions);
                lastPermissionMicros = curMicros;
            }
            if (curPermissions >= 1) {
                permitted = true;
                curPermissions--;
            } else {
                permitted = false;
            }
            next = new Cycle(curPermissions, lastPermissionMicros, permitted);
        } while (!compareAndSet(current, next));
        return next.permitted;
    }

    protected void syncPermission() {
        Cycle current;
        Cycle next;
        double curPermissions;
        long lastPermissionMicros;
        boolean permitted;
        long curMicros;
        double newPermissions;
        LeakyBucketLimiterConfig config;
        do {
            current = curCycle.get();
            curMicros = duration();
            curPermissions = current.curPermissions;
            lastPermissionMicros = current.lastPermissionMicros;
            permitted = current.permitted;
            if (curMicros > lastPermissionMicros) {
                config = limiter;
                newPermissions = (curMicros - lastPermissionMicros) / config.intervalMicros;
                curPermissions = Math.min(config.limitCount, curPermissions + newPermissions);
                lastPermissionMicros = curMicros;
            }
            next = new Cycle(curPermissions, lastPermissionMicros, permitted);
        } while (!compareAndSet(current, next));
    }

    /**
     * 运行时间
     *
     * @return
     */
    protected long duration() {
        //转换成微妙
        return TimeUnit.MICROSECONDS.convert(System.nanoTime() - startTimeNanos, TimeUnit.NANOSECONDS);
    }

    /**
     * 比较设置
     *
     * @param current
     * @param next
     * @return
     */
    protected boolean compareAndSet(final Cycle current, final Cycle next) {
        if (curCycle.compareAndSet(current, next)) {
            return true;
        }
        LockSupport.parkNanos(1);
        return false;
    }

    @Override
    public boolean reload(final RateLimiterConfig config) {
        return reload(config, true);
    }

    /**
     * 重新加载配置
     *
     * @param config
     * @param sync
     * @return
     */
    protected boolean reload(final RateLimiterConfig config, final boolean sync) {
        if (config == null) {
            return false;
        } else if (limiter != null && config.limitCount == limiter.limitCount && config.limitPeriodNanos == limiter.limitPeriodNanos) {
            //配置没有发生变化
            return true;
        }
        if (startTimeNanos > 0 && sync) {
            syncPermission();
        }
        this.limiter = new LeakyBucketLimiterConfig(config);
        //初始化启动时间，确保刚创建的时候就有maxPermissions个许可
        if (startTimeNanos == 0) {
            startTimeNanos = System.nanoTime() - limiter.limitPeriodNanos;
        }
        return true;
    }

    /**
     * 限流配置
     */
    protected static class LeakyBucketLimiterConfig extends RateLimiterConfig {
        /**
         * 产生一个许可的微秒数
         */
        protected transient double intervalMicros = -1;

        public LeakyBucketLimiterConfig(RateLimiterConfig config) {
            super(config.type, config.limitPeriodNanos, config.waitTimeoutNanos, config.limitCount);
            this.intervalMicros = TimeUnit.NANOSECONDS.toMicros(limitPeriodNanos) / (double) limitCount;
        }

        public double getIntervalMicros() {
            return intervalMicros;
        }
    }

    /**
     * 限流循环周期
     * <p>
     * 2019年4月3日 下午1:30:08
     */
    protected static class Cycle {
        //许可个数
        protected final double curPermissions;
        //上一次的时间
        protected final long lastPermissionMicros;
        //是否允许
        protected final boolean permitted;

        public Cycle(final double curPermissions, final long lastPermissionMicros, final boolean permitted) {
            this.curPermissions = curPermissions;
            this.lastPermissionMicros = lastPermissionMicros;
            this.permitted = permitted;
        }

    }

}
