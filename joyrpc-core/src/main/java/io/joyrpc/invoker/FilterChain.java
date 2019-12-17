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
import io.joyrpc.cluster.Cluster;
import io.joyrpc.cluster.ClusterAware;
import io.joyrpc.constants.Constants;
import io.joyrpc.extension.ExtensionMeta;
import io.joyrpc.extension.ExtensionPoint;
import io.joyrpc.extension.Name;
import io.joyrpc.extension.URL;
import io.joyrpc.filter.Filter;
import io.joyrpc.permission.StringBlackWhiteList;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import static io.joyrpc.Plugin.CONSUMER_FILTER;
import static io.joyrpc.Plugin.PROVIDER_FILTER;

/**
 * 调用链
 */
public class FilterChain implements Invoker {
    /**
     * 调用
     */
    protected Invoker invoker;

    @Override
    public CompletableFuture<Result> invoke(RequestMessage<Invocation> request) {
        return invoker.invoke(request);
    }

    @Override
    public CompletableFuture<Void> close() {
        return invoker == null ? CompletableFuture.completedFuture(null) : invoker.close();
    }

    /**
     * 构造消费者过滤链
     *
     * @param refer
     * @param last
     * @return
     */
    public static FilterChain consumer(final Refer refer, final Invoker last) {
        return build(refer.getCluster(), refer.getInterfaceClass(), refer.getInterfaceName(), refer.getUrl(), last, CONSUMER_FILTER);
    }

    /**
     * 构造生产者过滤链
     *
     * @param exporter
     * @param last
     * @return
     */
    public static FilterChain producer(final Exporter exporter, final Invoker last) {
        return build(null, exporter.getInterfaceClass(), exporter.getInterfaceName(), exporter.getUrl(), last, PROVIDER_FILTER);
    }

    /**
     * 构造过滤链
     *
     * @param cluster
     * @param clazz
     * @param className
     * @param url
     * @param last
     * @param extension
     * @return
     */
    public static <T extends Filter> FilterChain build(final Cluster cluster,
                                                       final Class clazz,
                                                       final String className,
                                                       final URL url,
                                                       final Invoker last,
                                                       final ExtensionPoint<T, String> extension) {
        FilterChain result = new FilterChain();
        List<Filter> filters = getFilters(url, extension);
        Invoker next = last;
        if (!filters.isEmpty()) {
            Filter filter;
            for (int i = filters.size() - 1; i >= 0; i--) {
                filter = filters.get(i);
                filter.setClass(clazz);
                filter.setClassName(className);
                filter.setUrl(url);
                if (cluster != null && filter instanceof ClusterAware) {
                    ((ClusterAware) filter).setCluster(cluster);
                }
                filter.setup();
                //需要传入Refer或Exporter的名字，方便并发数控制
                next = new FilterInvoker(filter, next, last.getName());
            }
        }
        result.invoker = next;
        return result;
    }

    /**
     * 获取所有配置filter
     *
     * @param url
     * @param extension
     * @return
     */
    protected static <T extends Filter> List<Filter> getFilters(final URL url, final ExtensionPoint<T, String> extension) {
        // 获取url里面所有-XXX需要排除的filter
        StringBlackWhiteList blackWhiteList = new StringBlackWhiteList(url.getString(Constants.FILTER_OPTION));
        //禁用默认系统插件
        boolean disableDefault = blackWhiteList.inBlack("default");
        //禁用系统插件判断
        Predicate<Filter> black = (t) -> disableDefault && (t.type() & Filter.SYSTEM) != 0;
        //全局插件
        Predicate<Filter> white = (t) -> (t.type() & Filter.GLOBAL) > 0;

        List<Filter> result = new ArrayList<>(10);
        Name<? extends T, String> name;
        Filter filter;
        for (ExtensionMeta<T, String> meta : extension.metas()) {
            name = meta.getExtension();
            filter = meta.getTarget();
            //如果是系统内置必须的或者满足黑白名单
            if (filter.test(url) && (filter.type() == Filter.INNER
                    || (blackWhiteList.isWhite(name.getName()) || white.test(filter))
                    && !blackWhiteList.isBlack(name.getName()) && !black.test(filter))) {
                //该插件通过了配置，并且是系统内置必须的，或者在白名单里面并且不在黑名单里面
                result.add(filter);
            }
        }

        return result;

    }

    /**
     * Filter的调用
     */
    protected static class FilterInvoker implements Invoker {
        /**
         * 过滤器
         */
        protected Filter filter;
        /**
         * 后续调用
         */
        protected Invoker next;
        /**
         * 名称
         */
        protected String name;

        /**
         * 构造函数
         *
         * @param filter
         * @param next
         * @param name
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
