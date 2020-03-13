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

import io.joyrpc.Result;
import io.joyrpc.cluster.distribution.ExceptionPolicy;
import io.joyrpc.cluster.distribution.ExceptionPredication;
import io.joyrpc.cluster.distribution.FailoverPolicy;
import io.joyrpc.cluster.distribution.FailoverPolicy.DefaultFailoverPolicy;
import io.joyrpc.cluster.distribution.TimeoutPolicy;
import io.joyrpc.exception.InitializationException;
import io.joyrpc.extension.URL;
import io.joyrpc.extension.URLOption;
import io.joyrpc.permission.BlackWhiteList;
import io.joyrpc.permission.ExceptionBlackWhiteList;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.util.ClassUtils;
import io.joyrpc.util.MethodOption.NameKeyOption;
import io.joyrpc.util.SystemClock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static io.joyrpc.GenericService.GENERIC;
import static io.joyrpc.Plugin.EXCEPTION_PREDICATION;
import static io.joyrpc.Plugin.FAILOVER_SELECTOR;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.constants.ExceptionCode.CONSUMER_FAILOVER_CLASS;
import static io.joyrpc.util.ClassUtils.forName;
import static io.joyrpc.util.StringUtils.SEMICOLON_COMMA_WHITESPACE;
import static io.joyrpc.util.StringUtils.split;

/**
 * 参数选项
 */
public class MethodOption {

    /**
     * 扫描Provider提供的META-INF/retry/xxx.xxx.xxx.xx文件的异常配置
     */
    protected static final Map<String, Set<String>> INNER_EXCEPTIONS = new ConcurrentHashMap<>();
    /**
     * 内置的异常类资源配置文件路径
     */
    protected static final String RETRY_RESOURCE_PATH = "META-INF/retry/";

    /**
     * 接口名称
     */
    protected String interfaceName;
    /**
     * 接口级别最大重试次数
     */
    protected int maxRetry;
    /**
     * 接口级别的每个节点重试一次
     */
    protected boolean retryOnlyOncePerNode;
    /**
     * 接口级别的重试目标节点选择器
     */
    protected String failoverSelector;
    /**
     * 异常检测
     */
    protected String failoverPredication;
    /**
     * 接口级别超时时间
     */
    protected int timeout;
    /**
     * 重试异常
     */
    protected BlackWhiteList<Class<? extends Throwable>> failoverBlackWhiteList;
    /**
     * 方法透传参数
     */
    protected NameKeyOption<Option> options;


    /**
     * 构造函数
     *
     * @param interfaceClass 接口类
     * @param interfaceName  接口名称
     * @param url            URL
     */
    public MethodOption(final Class<?> interfaceClass, final String interfaceName, final URL url) {
        this.interfaceName = interfaceName;
        this.failoverBlackWhiteList = buildFailoverBlackWhiteList(url);
        this.maxRetry = url.getInteger(RETRIES_OPTION);
        this.timeout = url.getInteger(TIMEOUT_OPTION);
        this.retryOnlyOncePerNode = url.getBoolean(RETRY_ONLY_ONCE_PER_NODE_OPTION);
        this.failoverSelector = url.getString(FAILOVER_SELECTOR_OPTION);
        this.failoverPredication = url.getString(FAILOVER_PREDICATION_OPTION);
        //方法级别的隐藏参数，保留以"."开头
        boolean generic = GENERIC.test(interfaceClass);
        this.options = new NameKeyOption<>(generic ? null : interfaceClass, generic ? interfaceName : null,
                o -> new Option(
                        url.startsWith(getKey(o, String.valueOf(HIDE_KEY_PREFIX)), (k, v) -> v.substring(k.length() - 1)),
                        url.getInteger(getKey(o, TIMEOUT_OPTION), timeout),
                        new DefaultFailoverPolicy<>(
                                url.getInteger(getKey(o, RETRIES_OPTION), maxRetry),
                                url.getBoolean(getKey(o, RETRY_ONLY_ONCE_PER_NODE_OPTION), retryOnlyOncePerNode),
                                new MyTimeoutPolicy(),
                                new MyExceptionPolicy(failoverBlackWhiteList, EXCEPTION_PREDICATION.get(failoverPredication)),
                                FAILOVER_SELECTOR.get(url.getString(getKey(o, FAILOVER_SELECTOR_OPTION), failoverSelector))))
        );
    }

    /**
     * 获取参数名称
     *
     * @param methodName 方法名称
     * @param option     选项
     * @return 参数名称
     */
    protected String getKey(final String methodName, final URLOption<?> option) {
        return METHOD_KEY_FUNC.apply(methodName, option.getName());
    }

    /**
     * 获取参数名称
     *
     * @param methodName 方法名称
     * @param name       参数
     * @return 参数名称
     */
    protected String getKey(final String methodName, final String name) {
        return METHOD_KEY_FUNC.apply(methodName, name);
    }

    /**
     * 构建异常重试类
     *
     * @param url url
     * @return 异常黑白名单
     */
    protected BlackWhiteList<Class<? extends Throwable>> buildFailoverBlackWhiteList(final URL url) {
        //内置的异常类名
        Set<String> names = new HashSet<>(INNER_EXCEPTIONS.computeIfAbsent(interfaceName, this::getInnerExceptions));
        //当前URL配置的异常
        String value = url.getString(FAILOVER_WHEN_THROWABLE_OPTION);
        if (value != null && !value.isEmpty()) {
            String[] classes = split(value, SEMICOLON_COMMA_WHITESPACE);
            Collections.addAll(names, classes);
        }
        Set<Class<? extends Throwable>> failoverClass = new HashSet<>();
        Class<?> c;
        for (String name : names) {
            try {
                c = forName(name);
                if (!Throwable.class.isAssignableFrom(c)) {
                    throw new InitializationException(String.format("Class is not extends throwable, %s", name), CONSUMER_FAILOVER_CLASS);
                }
                failoverClass.add((Class<? extends Throwable>) c);
            } catch (ClassNotFoundException e) {
                throw new InitializationException(e.getMessage(), CONSUMER_FAILOVER_CLASS);
            }
        }
        return new ExceptionBlackWhiteList(failoverClass, null, false);
    }

    /**
     * 获取方法选项
     *
     * @param methodName 方法名称
     * @return 方法选项
     */
    public Option getOption(final String methodName) {
        return options.get(methodName);
    }

    /**
     * 读取内置的异常配置信息
     *
     * @param interfaceName 接口名称
     * @return 异常类名
     */
    protected Set<String> getInnerExceptions(final String interfaceName) {
        Set<String> names = new HashSet<>();
        ClassLoader loader = ClassUtils.getCurrentClassLoader();
        String line;
        try {
            Enumeration<java.net.URL> urls = loader.getResources(RETRY_RESOURCE_PATH + interfaceName);
            while ((urls.hasMoreElements())) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(urls.nextElement().openStream(), StandardCharsets.UTF_8))) {
                    while ((line = reader.readLine()) != null) {
                        names.add(line);
                    }
                } catch (IOException e) {
                    throw new InitializationException(e.getMessage(), CONSUMER_FAILOVER_CLASS);
                }
            }
        } catch (IOException e) {
            throw new InitializationException(e.getMessage(), CONSUMER_FAILOVER_CLASS);
        }
        return names;

    }

    /**
     * 方法选项
     */
    public static class Option {
        /**
         * 隐式传参
         */
        protected Map<String, ?> implicits;
        /**
         * 超时时间
         */
        protected int timeout;
        /**
         * 重试策略
         */
        protected FailoverPolicy<RequestMessage<Invocation>, Result> failoverPolicy;

        /**
         * 构造函数
         *
         * @param implicits      隐式传参
         * @param timeout        超时时间
         * @param failoverPolicy 重试策略
         */
        public Option(Map<String, ?> implicits, int timeout,
                      FailoverPolicy<RequestMessage<Invocation>, Result> failoverPolicy) {
            this.implicits = implicits;
            this.timeout = timeout;
            this.failoverPolicy = failoverPolicy;
        }

        public Map<String, ?> getImplicits() {
            return implicits;
        }

        public int getTimeout() {
            return timeout;
        }

        public FailoverPolicy<RequestMessage<Invocation>, Result> getFailoverPolicy() {
            return failoverPolicy;
        }
    }

    /**
     * 异常策略
     */
    public static class MyExceptionPolicy implements ExceptionPolicy<Result> {
        /**
         * 异常黑白名单
         */
        protected BlackWhiteList<Class<? extends Throwable>> failoverBlackWhiteList;
        /**
         * 异常检测
         */
        protected ExceptionPredication exceptionPredication;

        /**
         * 构造函数
         *
         * @param failoverBlackWhiteList 异常黑白名单
         * @param exceptionPredication   异常断言
         */
        public MyExceptionPolicy(final BlackWhiteList<Class<? extends Throwable>> failoverBlackWhiteList,
                                 final ExceptionPredication exceptionPredication) {
            this.failoverBlackWhiteList = failoverBlackWhiteList;
            this.exceptionPredication = exceptionPredication;
        }

        @Override
        public Throwable getThrowable(final Result result) {
            return result.getException();
        }

        @Override
        public boolean test(final Throwable throwable) {
            //暂时不需要增加动态配置支持，这些一般都需要提前测试配置好。
            return failoverBlackWhiteList.isValid(throwable.getClass()) || (exceptionPredication != null && exceptionPredication.test(throwable));
        }
    }

    /**
     * 超时策略
     */
    public static class MyTimeoutPolicy implements TimeoutPolicy<RequestMessage<Invocation>> {

        @Override
        public boolean test(final RequestMessage<Invocation> request) {
            return request.isTimeout();
        }

        @Override
        public void reset(final RequestMessage<Invocation> request) {
            request.getHeader().setTimeout((int) (request.getTimeout() + request.getCreateTime() - SystemClock.now()));
        }
    }

}
