package io.joyrpc.cluster.distribution;

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

/**
 * 重试策略
 */
public interface FailoverPolicy {

    /**
     * 获取最大重试次数
     *
     * @return
     */
    int getMaxRetry();

    /**
     * 每个节点只调用一次
     *
     * @return
     */
    boolean isOnlyOncePerNode();

    /**
     * 超时策略
     */
    TimeoutPolicy getTimeoutPolicy();

    /**
     * 异常策略
     */
    ExceptionPolicy getExceptionPolicy();

    /**
     * 获取重试节点选择器
     *
     * @return
     */
    FailoverSelector getRetrySelector();

    /**
     * 默认重试策略
     */
    class DefaultFailoverPolicy implements FailoverPolicy {
        /**
         * 最大重试次数
         */
        protected int maxRetry;
        /**
         * 每个节点只调用一次
         */
        protected boolean onlyOncePerNode;
        /**
         * 超时策略
         */
        protected TimeoutPolicy timeoutPolicy;
        /**
         * 异常策略
         */
        protected ExceptionPolicy exceptionPolicy;
        /**
         * 重试节点选择器
         */
        protected FailoverSelector retrySelector;

        /**
         * 构造函数
         *
         * @param maxRetry
         */
        public DefaultFailoverPolicy(int maxRetry) {
            this(maxRetry, false, null, null, null);
        }

        /**
         * 构造函数
         *
         * @param maxRetry
         * @param onlyOncePerNode
         * @param timeoutPolicy
         * @param exceptionPolicy
         * @param retrySelector
         */
        public DefaultFailoverPolicy(int maxRetry, boolean onlyOncePerNode,
                                     TimeoutPolicy timeoutPolicy,
                                     ExceptionPolicy exceptionPolicy,
                                     FailoverSelector retrySelector) {
            this.maxRetry = maxRetry;
            this.onlyOncePerNode = onlyOncePerNode;
            this.timeoutPolicy = timeoutPolicy;
            this.exceptionPolicy = exceptionPolicy;
            this.retrySelector = retrySelector;
        }

        @Override
        public int getMaxRetry() {
            return maxRetry;
        }

        @Override
        public boolean isOnlyOncePerNode() {
            return onlyOncePerNode;
        }

        @Override
        public TimeoutPolicy getTimeoutPolicy() {
            return timeoutPolicy;
        }

        @Override
        public ExceptionPolicy getExceptionPolicy() {
            return exceptionPolicy;
        }

        @Override
        public FailoverSelector getRetrySelector() {
            return retrySelector;
        }
    }

}
