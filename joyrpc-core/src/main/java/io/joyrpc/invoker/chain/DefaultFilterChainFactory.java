package io.joyrpc.invoker.chain;

import io.joyrpc.Invoker;
import io.joyrpc.InvokerAware;
import io.joyrpc.Result;
import io.joyrpc.config.AbstractConsumerConfig;
import io.joyrpc.config.AbstractInterfaceConfig;
import io.joyrpc.constants.Constants;
import io.joyrpc.extension.*;
import io.joyrpc.filter.ConsumerFilter;
import io.joyrpc.filter.Filter;
import io.joyrpc.filter.ProviderFilter;
import io.joyrpc.invoker.Exporter;
import io.joyrpc.invoker.FilterChainFactory;
import io.joyrpc.invoker.Refer;
import io.joyrpc.permission.StringBlackWhiteList;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static io.joyrpc.Plugin.CONSUMER_FILTER;
import static io.joyrpc.Plugin.PROVIDER_FILTER;
import static io.joyrpc.util.StringUtils.SEMICOLON_COMMA_WHITESPACE;
import static io.joyrpc.util.StringUtils.split;

/**
 * 默认处理链工厂类
 */
@Extension("default")
public class DefaultFilterChainFactory implements FilterChainFactory {

    @Override
    public Invoker build(final Refer refer, final Invoker last) {
        return build(refer.getUrl(), refer.getBuilder(), last, CONSUMER_FILTER);
    }

    @Override
    public Invoker build(final Exporter exporter, final Invoker last) {
        return build(exporter.getUrl(), exporter.getBuilder(), last, PROVIDER_FILTER);
    }

    @Override
    public String validate(final AbstractInterfaceConfig config) {
        ExtensionPoint<? extends Filter, String> point = config instanceof AbstractConsumerConfig ? CONSUMER_FILTER : PROVIDER_FILTER;
        Class<?> clazz = config instanceof AbstractConsumerConfig ? ConsumerFilter.class : ProviderFilter.class;
        String[] filters = split(config.getFilter(), SEMICOLON_COMMA_WHITESPACE);
        for (String filter : filters) {
            if (filter.charAt(0) != '-' && null == point.get(filter)) {
                //过滤掉黑名单
                return String.format("No such extension '%s' of %s. ", filter, clazz.getName());
            }
        }
        return null;
    }

    /**
     * 构造过滤链
     *
     * @param url       参数
     * @param consumer  消费者
     * @param last      最后调用器
     * @param extension 过滤链扩展点
     * @return 调用
     */
    protected <T extends Filter> Invoker build(final URL url,
                                               final Consumer<InvokerAware> consumer,
                                               final Invoker last,
                                               final ExtensionPoint<T, String> extension) {
        List<Filter> filters = getFilters(url, extension);
        Invoker next = last;
        if (!filters.isEmpty()) {
            Filter filter;
            for (int i = filters.size() - 1; i >= 0; i--) {
                filter = filters.get(i);
                consumer.accept(filter);
                //需要传入Refer或Exporter的名字，方便并发数控制
                next = new FilterInvoker(filter, next, last.getName());
            }
        }
        return next;
    }

    /**
     * 获取所有配置filter
     *
     * @param url       参数
     * @param extension 过滤链扩展点
     * @return 过滤器集合
     */
    protected <T extends Filter> List<Filter> getFilters(final URL url, final ExtensionPoint<T, String> extension) {
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
     * 过滤器调用
     */
    static class FilterInvoker implements Invoker {
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
