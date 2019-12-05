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
import io.joyrpc.cluster.discovery.config.ConfigHandler;
import io.joyrpc.cluster.discovery.config.Configure;
import io.joyrpc.cluster.discovery.registry.Registry;
import io.joyrpc.config.ProviderConfig;
import io.joyrpc.constants.Constants;
import io.joyrpc.constants.ExceptionCode;
import io.joyrpc.context.RequestContext;
import io.joyrpc.exception.InitializationException;
import io.joyrpc.exception.ShutdownExecption;
import io.joyrpc.extension.URL;
import io.joyrpc.permission.Authenticator;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.transport.Server;
import io.joyrpc.util.Close;
import io.joyrpc.util.Futures;
import io.joyrpc.util.MethodOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.joyrpc.Plugin.AUTHENTICATOR;
import static io.joyrpc.constants.Constants.HIDE_KEY_PREFIX;
import static io.joyrpc.util.ClassUtils.isReturnFuture;

/**
 * @date: 15/1/2019
 */
public class Exporter<T> extends AbstractInvoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(Exporter.class);
    /**
     * 配置
     */
    protected ProviderConfig config;
    /**
     * 压缩类型
     */
    protected String compress;
    /**
     * 注册的URL
     */
    protected List<URL> registerUrls;
    /**
     * 关闭事件处理器
     */
    protected Consumer<Exporter> closing;
    /**
     * 配置变更处理器
     */
    protected ConfigHandler configHandler;
    /**
     * 服务
     */
    protected Server server;
    /**
     * 端口
     */
    protected int port;
    /**
     * 实现对象
     */
    protected T ref;
    /**
     * 接口透传参数
     */
    protected Map<String, String> interfaceImplicits;
    /**
     * 方法传参数
     */
    protected MethodOption<String, Map<String, String>> methodImplicits;
    /**
     * 调用链
     */
    protected FilterChain chain;
    /**
     * 注册中心
     */
    protected List<Registry> registries;
    /**
     * 订阅配置的注册中心
     */
    protected Registry subscribe;

    /**
     * 认证插件
     */
    protected Authenticator authenticator;

    /**
     * 构造函数
     *
     * @param name
     * @param url
     * @param config
     * @param server
     * @param closing
     */
    protected Exporter(final String name,
                       final URL url,
                       final ProviderConfig<T> config,
                       final Configure configure,
                       final URL subscribeUrl,
                       final Server server,
                       final Consumer<Exporter> closing) {
        this.name = name;
        this.url = url;
        this.config = config;
        this.configure = configure;
        this.subscribeUrl = subscribeUrl;
        this.server = server;
        this.closing = closing;

        //别名
        this.alias = url.getString(Constants.ALIAS_OPTION);
        //代理接口
        this.interfaceClass = config.getProxyClass();
        this.interfaceName = config.getInterfaceClazz();
        //保留全局的配置变更处理器，订阅和取消订阅对象一致
        this.configHandler = config.getConfigHandler();
        this.ref = config.getRef();
        this.port = url.getPort();
        this.compress = url.getString(Constants.COMPRESS_OPTION.getName());
        //接口透传参数
        this.interfaceImplicits = url.startsWith(String.valueOf(HIDE_KEY_PREFIX));
        //方法透传参数
        this.methodImplicits = new MethodOption.NameKeyOption<>(interfaceClass, m -> url.startsWith(
                Constants.METHOD_KEY.apply(m.getName(), String.valueOf(HIDE_KEY_PREFIX)), true));
        this.chain = FilterChain.producer(this, this::invokeMethod);
        this.authenticator = AUTHENTICATOR.get(url.getString(Constants.AUTHENTICATION_OPTION));
        this.registries = config.getRegistries();
        //往注册中心注册的URL
        this.registerUrls = registries.stream().map(registry -> normalize(registry, url, config.getProxyClass())).collect(Collectors.toList());
    }

    @Override
    protected CompletableFuture<Void> doOpen() {
        CompletableFuture<Void> result = new CompletableFuture<>();
        server.open(r -> {
            if (!r.isSuccess()) {
                result.completeExceptionally(new InitializationException(String.format("Open server : %s error", url), r.getThrowable()));
            } else {
                //注册服务
                Futures.chain(doRegister(registries), result);
            }
        });
        return result;
    }

    @Override
    protected CompletableFuture<Void> doClose() {
        CompletableFuture<Void> result = new CompletableFuture<>();
        if (closing != null) {
            closing.accept(this);
        }
        //关闭服务
        if (server != null) {
            server.close(o -> {
                //在这里安全关闭外部线程池
                Close.close(server.getBizThreadPool(), 0);
                if (o.isSuccess()) {
                    Futures.chain(deregister(), result);
                } else {
                    deregister().whenComplete((v, t) -> result.completeExceptionally(o.getThrowable()));
                }
            });
        } else {
            Futures.chain(deregister(), result);
        }
        return result;
    }

    @Override
    protected Throwable shutdownException() {
        return new ShutdownExecption("provider is shutdown", ExceptionCode.PROVIDER_OFFLINE, true);
    }

    @Override
    protected CompletableFuture<Result> doInvoke(final RequestMessage<Invocation> request) {
        Invocation invocation = request.getPayLoad();
        //设置调用的对象，便于Validate
        invocation.setObject(ref);
        //设置透传标识
        RequestContext context = request.getContext();
        context.setAsync(isReturnFuture(invocation.getClazz(), invocation.getMethod()));
        context.setProvider(true);
        context.setAttachments(interfaceImplicits).setAttachments(methodImplicits.get(invocation.getMethod()));

        //执行调用链
        CompletableFuture<Result> future = chain.invoke(request);

        return future;
    }

    /**
     * 调用方法
     *
     * @param request
     * @return
     */
    protected CompletableFuture<Result> invokeMethod(final RequestMessage<Invocation> request) {

        Invocation invocation = request.getPayLoad();

        CompletableFuture<Result> resultFuture;
        //恢复上下文，因为过滤链（缓存）这些是异步的
        RequestContext context = request.getContext();
        RequestContext.restore(context);
        try {
            // 反射 真正调用业务代码
            resultFuture = CompletableFuture.completedFuture(new Result(context, invocation.invoke(ref)));
        } catch (IllegalArgumentException e) { // 非法参数，可能是实现类和接口类不对应
            resultFuture = CompletableFuture.completedFuture(new Result(context, e));
        } catch (InvocationTargetException e) { // 业务代码抛出异常
            resultFuture = CompletableFuture.completedFuture(new Result(context, e.getCause()));
        } catch (IllegalAccessException e) {
            resultFuture = CompletableFuture.completedFuture(new Result(context, e));
        } finally {
            RequestContext.remove();
        }

        return resultFuture;
    }

    /**
     * 注册
     *
     * @return
     */
    protected CompletableFuture<Void> doRegister(final List<Registry> registries) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        if (!url.getBoolean(Constants.REGISTER_OPTION)) {
            result.complete(null);
        } else {
            //多注册中心
            CompletableFuture<URL>[] futures = new CompletableFuture[registries.size()];
            for (int i = 0; i < registries.size(); i++) {
                futures[i] = registries.get(i).register(url);
            }

            //所有注册成功
            CompletableFuture.allOf(futures).whenComplete((v, t) -> {
                if (t == null) {
                    result.complete(null);
                } else {
                    result.completeExceptionally(new InitializationException(String.format("Open registry : %s error", url), t));
                }
            });
        }
        return result;
    }

    /**
     * 注销
     *
     * @return
     */
    protected CompletableFuture<Void> deregister() {
        logger.info(String.format("deregister provider config : %s", url.toString(false, false)));
        if (registries == null || registries.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        } else {
            CompletableFuture<URL>[] futures = new CompletableFuture[registries.size()];
            if (url.getBoolean(Constants.SUBSCRIBE_OPTION)) {
                //取消订阅
                // todo 不能保证执行成功
                configure.unsubscribe(subscribeUrl, configHandler);
            }
            //取消注册
            for (int i = 0; i < registries.size(); i++) {
                futures[i] = registries.get(i).deregister(registerUrls.get(i));
            }
            return CompletableFuture.allOf(futures);
        }
    }

    public ProviderConfig getConfig() {
        return config;
    }

    public String getCompress() {
        return compress;
    }

    public Server getServer() {
        return server;
    }

    public List<Registry> getRegistries() {
        List<Registry> result = registries == null ? config.getRegistries() : registries;
        return result == null ? new ArrayList<>(0) : result;
    }

    public int getPort() {
        return port;
    }

    public Authenticator getAuthenticator() {
        return authenticator;
    }
}
