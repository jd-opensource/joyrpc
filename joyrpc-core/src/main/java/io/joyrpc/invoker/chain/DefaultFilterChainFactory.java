package io.joyrpc.invoker.chain;

import io.joyrpc.Invoker;
import io.joyrpc.cluster.Cluster;
import io.joyrpc.cluster.ClusterAware;
import io.joyrpc.constants.Constants;
import io.joyrpc.extension.*;
import io.joyrpc.filter.Filter;
import io.joyrpc.invoker.Exporter;
import io.joyrpc.invoker.FilterChainFactory;
import io.joyrpc.invoker.Refer;
import io.joyrpc.permission.StringBlackWhiteList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static io.joyrpc.Plugin.CONSUMER_FILTER;
import static io.joyrpc.Plugin.PROVIDER_FILTER;

/**
 * 默认处理链工厂类
 */
@Extension("default")
public class DefaultFilterChainFactory implements FilterChainFactory {

    @Override
    public Invoker build(final Refer refer, final Invoker last) {
        return build(refer.getCluster(), refer.getInterfaceClass(), refer.getInterfaceName(), refer.getUrl(), last, CONSUMER_FILTER);
    }

    @Override
    public Invoker build(final Exporter exporter, final Invoker last) {
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
    protected <T extends Filter> Invoker build(final Cluster cluster,
                                               final Class clazz,
                                               final String className,
                                               final URL url,
                                               final Invoker last,
                                               final ExtensionPoint<T, String> extension) {
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
        return next;
    }

    /**
     * 获取所有配置filter
     *
     * @param url
     * @param extension
     * @return
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
}
