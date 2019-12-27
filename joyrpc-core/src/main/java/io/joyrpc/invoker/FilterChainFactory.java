package io.joyrpc.invoker;

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
import io.joyrpc.extension.Extensible;
import io.joyrpc.filter.Filter;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

import java.util.concurrent.CompletableFuture;

/**
 * 构造处理链
 */
@Extensible("filterChainFactory")
public interface FilterChainFactory {

    /**
     * 构造消费者过滤链
     *
     * @param refer 服务消费者
     * @param last  最后执行逻辑
     * @return 处理链
     */
    Invoker build(Refer refer, Invoker last);

    /**
     * 构造服务提供者过滤链
     *
     * @param exporter 服务提供者
     * @param last     最后执行逻辑
     * @return 处理链
     */
    Invoker build(Exporter exporter, Invoker last);

    /**
     * Filter的调用
     */
    class FilterInvoker implements Invoker {
        /**
         * 过滤器
         */
        protected final Filter filter;
        /**
         * 后续调用
         */
        protected final Invoker next;
        /**
         * 名称
         */
        protected final String name;

        /**
         * 构造函数
         *
         * @param filter 过滤链
         * @param next   下一个调用
         * @param name   名称
         */
        public FilterInvoker(Filter filter, Invoker next, String name) {
            this.filter = filter;
            this.next = next;
            this.name = name;
        }

        @Override
        public CompletableFuture<Result> invoke(RequestMessage<Invocation> request) {
            return filter.invoke(next, request);
        }

        @Override
        public CompletableFuture<Void> close() {
            CompletableFuture<Void> result = new CompletableFuture<>();
            filter.close().whenComplete((v, t) -> next.close().whenComplete((o, s) -> {
                if (t == null && s == null) {
                    result.complete(null);
                } else if (t != null) {
                    result.completeExceptionally(t);
                } else {
                    result.completeExceptionally(s);
                }
            }));
            return result;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
