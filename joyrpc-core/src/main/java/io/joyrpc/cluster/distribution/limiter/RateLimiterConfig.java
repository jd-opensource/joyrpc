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

import java.io.Serializable;

/**
 * 限流配置
 *
 * 2019年4月2日 下午9:22:23
 */
public class RateLimiterConfig implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 类型
     */
    protected String type;
    /**
     * 限流周期，单位：纳秒
     */
    protected long limitPeriodNanos;
    /**
     * 等待超时，单位：纳秒
     */
    protected long waitTimeoutNanos;
    /**
     * 限流数
     */
    protected int limitCount;

    /**
     * 构造函数
     *
     * @param type
     * @param limitPeriodNanos
     * @param waitTimeoutNanos
     * @param limitCount
     */
    public RateLimiterConfig(final String type, final long limitPeriodNanos, final long waitTimeoutNanos, final int limitCount) {
        this.type = type;
        this.limitPeriodNanos = limitPeriodNanos > 0 ? limitPeriodNanos : 1000 * 1000 * 1000;
        this.waitTimeoutNanos = waitTimeoutNanos > 0 ? waitTimeoutNanos : 0;
        this.limitCount = limitCount > 0 ? limitCount : 20000;
    }

    /**
     * 构造器
     *
     * @return
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 构造器
     *
     * @param config
     * @return
     */
    public static Builder builder(final RateLimiterConfig config) {
        return new Builder(config);
    }

    public String getType() {
        return type;
    }

    public long getLimitPeriodNanos() {
        return limitPeriodNanos;
    }

    public long getWaitTimeoutNanos() {
        return waitTimeoutNanos;
    }

    public int getLimitCount() {
        return limitCount;
    }

    /**
     * 构造器
     */
    public static class Builder {

        /**
         * 类型
         */
        protected String type;
        /**
         * 限流周期，单位：纳秒
         */
        protected long limitPeriodNanos;
        /**
         * 等待超时，单位：纳秒
         */
        protected long waitTimeoutNanos;
        /**
         * 限流数
         */
        protected int limitCount;

        /**
         * 构造函数
         */
        public Builder() {
        }

        /**
         * 构造函数
         *
         * @param config
         */
        public Builder(final RateLimiterConfig config) {
            if (config != null) {
                type = config.type;
                limitCount = config.limitCount;
                limitPeriodNanos = config.limitPeriodNanos;
                waitTimeoutNanos = config.waitTimeoutNanos;
            }
        }

        public Builder type(final String type) {
            this.type = type;
            return this;
        }

        public Builder limitCount(final int limitCount) {
            this.limitCount = limitCount;
            return this;
        }

        public Builder limitPeriodNanos(final long limitPeriodNanos) {
            this.limitPeriodNanos = limitPeriodNanos;
            return this;
        }

        public Builder waitTimeoutNanos(final long waitTimeoutNanos) {
            this.waitTimeoutNanos = waitTimeoutNanos;
            return this;
        }

        /**
         * 构建
         *
         * @return
         */
        public RateLimiterConfig build() {
            return new RateLimiterConfig(type, limitPeriodNanos, waitTimeoutNanos, limitCount);
        }

    }

}
