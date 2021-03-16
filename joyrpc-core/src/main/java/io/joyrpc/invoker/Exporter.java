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

import io.joyrpc.InvokerAware;
import io.joyrpc.Result;
import io.joyrpc.cluster.discovery.config.ConfigHandler;
import io.joyrpc.cluster.discovery.config.Configure;
import io.joyrpc.cluster.discovery.registry.Registry;
import io.joyrpc.config.ConfigAware;
import io.joyrpc.option.MethodOption;
import io.joyrpc.option.ProviderMethodOption;
import io.joyrpc.config.ProviderConfig;
import io.joyrpc.config.Warmup;
import io.joyrpc.constants.Constants;
import io.joyrpc.constants.ExceptionCode;
import io.joyrpc.context.RequestContext;
import io.joyrpc.event.Publisher;
import io.joyrpc.exception.InitializationException;
import io.joyrpc.exception.ShutdownExecption;
import io.joyrpc.extension.URL;
import io.joyrpc.invoker.ExporterEvent.EventType;
import io.joyrpc.permission.Authentication;
import io.joyrpc.permission.Authorization;
import io.joyrpc.permission.Identification;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.proxy.MethodCaller;
import io.joyrpc.transport.DecoratorServer;
import io.joyrpc.transport.Server;
import io.joyrpc.transport.TransportServer;
import io.joyrpc.util.Close;
import io.joyrpc.util.Futures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static io.joyrpc.Plugin.*;
import static io.joyrpc.constants.Constants.FILTER_CHAIN_FACTORY_OPTION;

/**
 * @date: 15/1/2019
 */
public class Exporter extends AbstractService {

    private static final Logger logger = LoggerFactory.getLogger(Exporter.class);
    /**
     * 配置
     */
    protected ProviderConfig<?> config;
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
     * 回调容器
     */
    protected CallbackContainer container;
    /**
     * 端口
     */
    protected int port;
    /**
     * 实现对象
     */
    protected Object ref;

    /**
     * 注册中心
     */
    protected List<Registry> registries;
    /**
     * 订阅配置的注册中心
     */
    protected Registry subscribe;
    /**
     * 身份提供者
     */
    protected Identification identification;
    /**
     * 身份认证
     */
    protected Authentication authentication;
    /**
     * 权限认证
     */
    protected Authorization authorization;
    /**
     * 预热
     */
    protected Warmup warmup;
    /**
     * 事件通知器
     */
    protected Publisher<ExporterEvent> publisher;

    protected Exporter() {
    }

    /**
     * 构造函数
     *
     * @param name          名称
     * @param url           服务URL
     * @param config        服务提供者配置
     * @param registries    注册中心
     * @param registerUrls  注册的URL
     * @param configure     配置
     * @param subscribeUrl  订阅配置的URL
     * @param configHandler 配置监听器
     * @param server        服务
     * @param container     回调容器
     * @param publisher     事件总线
     * @param closing       关闭消费者
     */
    protected Exporter(final String name,
                       final URL url,
                       final ProviderConfig<?> config,
                       final List<Registry> registries,
                       final List<URL> registerUrls,
                       final Configure configure,
                       final URL subscribeUrl,
                       final ConfigHandler configHandler,
                       final Server server,
                       final CallbackContainer container,
                       final Publisher<ExporterEvent> publisher,
                       final Consumer<Exporter> closing) {
        this.name = name;
        this.config = config;
        this.registries = registries;
        this.registerUrls = registerUrls;
        this.configure = configure;
        this.url = url;
        this.subscribeUrl = subscribeUrl;
        this.configHandler = configHandler;
        this.server = server;
        this.container = container;
        this.closing = closing;

        //别名
        this.alias = url.getString(Constants.ALIAS_OPTION);
        //代理接口
        this.interfaceClass = config.getProxyClass();
        //真实的接口名字
        this.interfaceName = url.getPath();
        //保留全局的配置变更处理器，订阅和取消订阅对象一致
        this.ref = config.getRef();
        this.warmup = config.getWarmup();
        this.port = url.getPort();
        this.compress = url.getString(Constants.COMPRESS_OPTION.getName());
        this.option = INTERFACE_OPTION_FACTORY.get().create(interfaceClass, interfaceName, url, ref);
        this.chain = FILTER_CHAIN_FACTORY.getOrDefault(url.getString(FILTER_CHAIN_FACTORY_OPTION))
                .build(this, this::invokeMethod);
        this.identification = IDENTIFICATION.get(url.getString(Constants.IDENTIFICATION_OPTION));
        this.authentication = AUTHENTICATOR.get(url.getString(Constants.AUTHENTICATION_OPTION));
        this.authorization = AUTHORIZATION.get(url.getString(Constants.AUTHORIZATION_OPTION));
        this.publisher = publisher;
        this.publisher.offer(new ExporterEvent(EventType.INITIAL, name, this));
        //设置权限认证信息
        if (authentication != null && authentication instanceof InvokerAware) {
            setup((InvokerAware) authentication);
        }
        if (authorization != null && authorization instanceof InvokerAware) {
            setup((InvokerAware) authorization);
        }
    }

    @Override
    protected CompletableFuture<Void> doOpen() {
        CompletableFuture<Void> result = new CompletableFuture<>();

        warmup().whenComplete((v, t) -> {
            if (t != null) {
                //预热失败，则自动退出
                result.completeExceptionally(t);
            } else {
                if (warmup != null) {
                    logger.info("Success warmuping provider " + name);
                }
                server.open().whenComplete((c, e) -> {
                    if (e != null) {
                        result.completeExceptionally(new InitializationException(String.format("Error occurs while open server : %s error", name), e));
                    } else {
                        //如果服务感知配置
                        configAware().whenComplete((o, s) -> {
                            if (s != null) {
                                result.completeExceptionally(new InitializationException(String.format("Error occurs while setup server : %s error", name), s));
                            } else {
                                //注册服务
                                //当多个接口使用同一的服务名称S进行注册，A接口注册后，B接口还没有准备好，客户端拿到服务S的地址，调用B的请求到达，B还没有就绪
                                Futures.chain(doRegister(registries), result);
                            }
                        });
                    }
                });
            }
        });
        return result;
    }

    /**
     * 预热
     *
     * @return CompletableFuture
     */
    protected CompletableFuture<Void> warmup() {
        return warmup == null ? CompletableFuture.completedFuture(null) : warmup.setup(config);
    }

    /**
     * 配置感知
     *
     * @return CompletableFuture
     */
    protected CompletableFuture<Void> configAware() {
        if (server instanceof ConfigAware) {
            return ((ConfigAware) server).setup(config);
        } else if (server instanceof DecoratorServer) {
            TransportServer transport = ((DecoratorServer) server).getTransport();
            while (transport != null) {
                if (transport instanceof ConfigAware) {
                    return ((ConfigAware) transport).setup(config);
                } else if (!(transport instanceof DecoratorServer)) {
                    return CompletableFuture.completedFuture(null);
                } else {
                    transport = ((DecoratorServer) transport).getTransport();
                }
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected CompletableFuture<Void> doClose() {
        //关闭一下接口选项，释放额外的资源
        option.close();
        publisher.offer(new ExporterEvent(EventType.CLOSE, name, this));
        CompletableFuture<Void> future1 = deregister().whenComplete((v, t) -> logger.info("Success deregister provider config " + name));
        CompletableFuture<Void> future2 = unsubscribe().whenComplete((v, t) -> logger.info("Success unsubscribe provider config " + name));
        //关闭服务
        CompletableFuture<Void> future3 = new CompletableFuture<>();
        if (server != null) {
            server.close().whenComplete((v, e) -> {
                //在这里安全关闭外部线程池
                Close.close(server.getWorkerPool(), 0);
                future3.complete(null);
            });
        } else {
            future3.complete(null);
        }
        return CompletableFuture.allOf(future1, future2, future3).whenComplete((v, t) -> {
            if (closing != null) {
                closing.accept(this);
            }
            logger.info("Success close provider config " + name);
        });

    }

    @Override
    protected Throwable shutdownException() {
        return new ShutdownExecption("provider is shutdown", ExceptionCode.PROVIDER_OFFLINE, true);
    }

    @Override
    public void setup(final RequestMessage<Invocation> request) {
        Invocation invocation = request.getPayLoad();
        MethodOption option = this.option.getOption(invocation.getMethodName());
        //类名，如果不存在则从会话里面获取
        invocation.setClazz(interfaceClass);
        invocation.setMethod(option.getMethod());
        invocation.setGenericMethod(option.getGenericMethod());
        //设置调用的对象，便于Validate
        invocation.setObject(ref);
        //注入身份认证信息和鉴权
        request.setAuthentication(authentication);
        request.setIdentification(identification);
        request.setAuthorization(authorization);
        request.setOption(option);

        //设置透传标识
        RequestContext context = request.getContext();
        context.setAsync(option.isAsync());
        context.setProvider(true);
        //方法透传参数，整合了接口级别的参数
        context.setAttachments(option.getImplicits());
        //恢复参数
        transmit.onServerReceive(request);
        //处理回调
        if (option.getCallback() != null) {
            container.addCallback(request, request.getTransport());
        }
    }

    /**
     * 调用方法
     *
     * @param request 请求
     * @return Future对象
     */
    protected CompletableFuture<Result> invokeMethod(final RequestMessage<Invocation> request) {
        final CompletableFuture<Result> result = new CompletableFuture<>();
        //恢复上下文，因为过滤链（缓存）这些是异步的
        request.restore(() -> {
            try {
                Invocation invocation = request.getPayLoad();
                MethodCaller caller = ((ProviderMethodOption) request.getOption()).getCaller();
                // 反射 真正调用业务代码
                Object value = caller != null ? caller.invoke(invocation.getArgs()) : invocation.invoke(ref);
                result.complete(new Result(request.getContext(), value));
            } catch (IllegalArgumentException | IllegalAccessException e) { // 非法参数，可能是实现类和接口类不对应
                result.complete(new Result(request.getContext(), e));
            } catch (InvocationTargetException e) { // 业务代码抛出异常
                result.complete(new Result(request.getContext(), e.getCause()));
            }
        });
        return result;
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
            CompletableFuture<?>[] futures = new CompletableFuture[registries.size()];
            for (int i = 0; i < registries.size(); i++) {
                futures[i] = registries.get(i).register(url);
            }
            //所有注册成功
            CompletableFuture.allOf(futures).whenComplete((v, t) -> {
                if (t == null) {
                    publisher.offer(new ExporterEvent(EventType.OPEN, name, this));
                    result.complete(null);
                } else {
                    result.completeExceptionally(new InitializationException(String.format("Open registry : %s error", url), t));
                }
            });
        }
        return result;
    }

    /**
     * 取消配置订阅
     *
     * @return
     */
    protected CompletableFuture<Void> unsubscribe() {
        if (url.getBoolean(Constants.SUBSCRIBE_OPTION)) {
            //取消订阅
            // todo 不能保证执行成功
            configure.unsubscribe(subscribeUrl, configHandler);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 注销
     *
     * @return
     */
    protected CompletableFuture<Void> deregister() {
        if (registries == null || registries.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        } else {
            CompletableFuture<?>[] futures = new CompletableFuture[registries.size()];
            //取消注册，最多重试1次
            for (int i = 0; i < registries.size(); i++) {
                futures[i] = registries.get(i).deregister(registerUrls.get(i), 1);
            }
            return CompletableFuture.allOf(futures);
        }
    }

    public ProviderConfig<?> getConfig() {
        return config;
    }

    public String getCompress() {
        return compress;
    }

    public Server getServer() {
        return server;
    }

    public List<Registry> getRegistries() {
        return registries;
    }

    public int getPort() {
        return port;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public Identification getIdentification() {
        return identification;
    }

    public Authorization getAuthorization() {
        return authorization;
    }
}
