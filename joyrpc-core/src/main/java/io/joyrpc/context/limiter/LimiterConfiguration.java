package io.joyrpc.context.limiter;

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
import io.joyrpc.context.AbstractInterfaceConfiguration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流管理器
 */
public class LimiterConfiguration extends AbstractInterfaceConfiguration<String, LimiterConfiguration.ClassLimiter> {

    /**
     * 结果缓存
     */
    public static final LimiterConfiguration LIMITERS = new LimiterConfiguration();

    /**
     * 接口限流配置
     */
    public static class ClassLimiter {
        /**
         * 限流配置
         */
        protected final Map<Option, RateLimiter> limiters;
        /**
         * 最佳配置
         */
        protected final Map<Option, RateLimiter> bests = new ConcurrentHashMap<>();

        /**
         * 构造函数
         *
         * @param limiters
         */
        public ClassLimiter(final Map<Option, RateLimiter> limiters) {
            this.limiters = limiters;
        }

        /**
         * 读取限流数据
         *
         * @param option
         * @return 结果
         */
        public RateLimiter get(final Option option) {
            if (option == null || limiters.isEmpty()) {
                return null;
            }
            //从缓存的最佳配置里面获取
            return bests.computeIfAbsent(option, o -> {
                //最佳匹配算法
                Option[] options = o.application.isEmpty() ?
                        new Option[]{
                                new Option("", "", ""),
                                new Option("", o.alias, ""),
                                new Option(o.method, "", ""),
                                new Option(o.method, o.alias, "")
                        } :
                        new Option[]{
                                new Option("", "", ""),
                                new Option("", o.alias, ""),
                                new Option(o.method, "", ""),
                                new Option(o.method, o.alias, ""),
                                new Option("", "", o.application),
                                new Option("", o.alias, o.application),
                                new Option(o.method, "", o.application),
                                new Option(o.method, o.alias, o.application)
                        };
                RateLimiter limiter;
                //按照优先级获取限流配置
                for (int index = options.length - 1; index >= 0; index--) {
                    //查找限流器
                    limiter = limiters.get(options[index]);
                    if (limiter != null) {
                        //找到了最佳限流器
                        return limiter;
                    }
                }
                return null;
            });
        }

        public Map<Option, RateLimiter> getLimiters() {
            return limiters;
        }
    }

    /**
     * 配置选项
     */
    public static class Option {
        /**
         * 方法名称
         */
        protected final String method;
        /**
         * 别名
         */
        protected final String alias;
        /**
         * 应用
         */
        protected final String application;

        /**
         * 构造函数
         *
         * @param method
         * @param alias
         * @param application
         */
        public Option(final String method, final String alias, final String application) {
            this.method = method;
            this.alias = alias;
            this.application = application;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Option option = (Option) o;

            if (!method.equals(option.method)) {
                return false;
            }
            if (!alias.equals(option.alias)) {
                return false;
            }
            return application.equals(option.application);
        }

        @Override
        public int hashCode() {
            int result = method.hashCode();
            result = 31 * result + alias.hashCode();
            result = 31 * result + application.hashCode();
            return result;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Option{");
            sb.append("method='").append(method).append('\'');
            sb.append(", alias='").append(alias).append('\'');
            sb.append(", application='").append(application).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }

}
