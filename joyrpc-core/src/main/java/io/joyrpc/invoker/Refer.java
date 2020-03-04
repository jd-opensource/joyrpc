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
import io.joyrpc.cluster.Node;
import io.joyrpc.cluster.discovery.config.ConfigHandler;
import io.joyrpc.cluster.discovery.config.Configure;
import io.joyrpc.cluster.discovery.registry.Registry;
import io.joyrpc.cluster.distribution.*;
import io.joyrpc.config.ConsumerConfig;
import io.joyrpc.constants.Constants;
import io.joyrpc.constants.HeadKey;
import io.joyrpc.context.injection.NodeReqInjection;
import io.joyrpc.context.injection.Transmit;
import io.joyrpc.exception.NoAliveProviderException;
import io.joyrpc.exception.ShutdownExecption;
import io.joyrpc.exception.TransportException;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.ClientProtocol;
import io.joyrpc.protocol.Protocol.MessageConverter;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.MessageHeader;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.protocol.message.ResponsePayload;
import io.joyrpc.transport.Client;
import io.joyrpc.transport.message.Message;
import io.joyrpc.transport.session.Session;
import io.joyrpc.util.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static io.joyrpc.Plugin.*;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.constants.ExceptionCode.CONSUMER_NO_ALIVE_PROVIDER;
import static io.joyrpc.invoker.InvokerManager.NAME;

/**
 * 引用
 *
 * @date: 2019/1/10
 */
public class Refer extends AbstractInvoker {
    private static final Logger logger = LoggerFactory.getLogger(Refer.class);
    /**
     * 消费配置
     */
    protected ConsumerConfig config;
    /**
     * 注册中心
     */
    protected Registry registry;
    /**
     * 往注册中心订阅注册的URL
     */
    protected URL registerUrl;
    /**
     * 集群
     */
    protected Cluster cluster;
    /**
     * 接口透传参数
     */
    protected Map<String, String> interfaceImplicits;
    /**
     * 配置变更处理器
     */
    protected ConfigHandler configHandler;
    /**
     * 集群分发策略
     */
    protected Distribution<RequestMessage<Invocation>, Result> distribution;
    /**
     * 过滤链
     */
    protected Invoker chain;
    /**
     * 回调容器
     */
    protected CallbackContainer container;
    /**
     * 关闭的消费者
     */
    protected BiConsumer<Refer, ? super Throwable> closing;
    /**
     * 是否优先本地JVM调用
     */
    protected boolean inJvm;
    /**
     * 本地服务的名称
     */
    protected String exporter;
    /**
     * 方法选项
     */
    protected MethodOption options;
    /**
     * 透传插件
     */
    protected Iterable<Transmit> transmits;
    /**
     * 条件透传
     */
    protected Iterable<NodeReqInjection> injections;
    /**
     * 分发异常处理插件
     */
    protected Iterable<ExceptionHandler> exceptionHandlers;


    /**
     * 构造函数
     *
     * @param name
     * @param url
     * @param config
     * @param registry
     * @param configure
     * @param subscribeUrl
     * @param configHandler
     * @param cluster
     * @param loadBalance
     * @param container
     * @param closing
     */
    protected Refer(final String name,
                    final URL url,
                    final ConsumerConfig config,
                    final Registry registry,
                    final URL registerUrl,
                    final Configure configure,
                    final URL subscribeUrl,
                    final ConfigHandler configHandler,
                    final Cluster cluster,
                    final LoadBalance loadBalance,
                    final CallbackContainer container,
                    final BiConsumer<Refer, ? super Throwable> closing) {
        this.name = name;
        this.url = url;
        this.config = config;
        this.registry = registry;
        this.registerUrl = registerUrl;
        this.configure = configure;
        this.subscribeUrl = subscribeUrl;
        //保留全局的配置变更处理器
        this.configHandler = configHandler;
        this.cluster = cluster;
        this.container = container;
        this.closing = closing;
        this.system = url.getBoolean(Constants.SYSTEM_OPTION);
        //别名
        this.alias = url.getString(Constants.ALIAS_OPTION);
        //代理接口
        this.interfaceClass = config.getProxyClass();
        //真实接口名称
        this.interfaceName = url.getPath();

        this.inJvm = url.getBoolean(Constants.IN_JVM_OPTION);
        this.exporter = NAME.apply(interfaceName, alias);

        //接口级别的隐藏参数，保留以"."开头
        this.interfaceImplicits = url.startsWith(String.valueOf(HIDE_KEY_PREFIX));
        this.options = new MethodOption(interfaceClass, interfaceName, url);
        this.cluster.addHandler(config);
        this.distribution = buildDistribution(buildRouter(url, interfaceClass), buildRoute(url, loadBalance));
        //处理链
        this.chain = FILTER_CHAIN_FACTORY.getOrDefault(url.getString(FILTER_CHAIN_FACTORY_OPTION))
                .build(this, this::distribute);
        //加载符合条件的透传插件，例如在MESH环境加载MeshTransmit
        this.transmits = TRANSMIT.extensions();
        this.injections = NODE_REQUEST_INJECTION.extensions(o -> o.test());
        this.exceptionHandlers = EXCEPTION_HANDLER.extensions();
    }

    /**
     * 获取请求的重试策略
     *
     * @param request
     * @return
     */
    protected FailoverPolicy getFailoverPolicy(final RequestMessage<Invocation> request) {
        return request.getFailoverPolicy();
    }

    /**
     * 构建分发对象
     *
     * @param router
     * @param route
     * @return
     */
    protected Distribution<RequestMessage<Invocation>, Result> buildDistribution(final Router<RequestMessage<Invocation>> router,
                                                                                 final Route<RequestMessage<Invocation>, Result> route) {
        return new Distribution<>(cluster, router, route,
                request -> CompletableFuture.completedFuture(new Result(request.getContext(),
                        new NoAliveProviderException(String.format("No alive provider found. class=%s alias=%s", interfaceName, alias),
                                CONSUMER_NO_ALIVE_PROVIDER))));
    }

    /**
     * 调用远程
     *
     * @param node    当前节点
     * @param last    前一个节点
     * @param request 请求
     * @return
     */
    protected CompletableFuture<Result> invokeRemote(final Node node, final Node last, final RequestMessage<Invocation> request) {
        Client client = node.getClient();
        if (client == null) {
            //选择完后，节点可能被其它线程断开连接了
            throw new TransportException("Error occurs while sending message. caused by client is null.", true);
        }
        Session session = client.session();
        //header 使用协商结果
        MessageHeader header = request.getHeader();
        header.copy(session);
        //条件透传注入
        for (NodeReqInjection injection : injections) {
            if (last != null) {
                //取消上一个节点注入的参数
                injection.reject(request, last);
            }
            //注入当前节点参数
            injection.inject(request, node);
        }
        //绑定回调，调用异常会删除注册的callback，避免造成垃圾数据
        if (container != null) {
            container.addCallback(request, client);
        }
        //异步发起调用
        CompletableFuture<Message> msgFuture = client.async(request, header.getTimeout());

        //返回future
        return msgFuture.handle((msg, err) -> {
            Result result;
            //线程恢复统一改在consumerInvokerHandler里面
            if (err != null) {
                result = new Result(request.getContext(), err, msg);
            } else {
                result = buildResult(request, msg, client.getProtocol());
            }
            if (result.isException()) {
                //异常处理
                onException(request, result, client);
            }

            return result;
        });
    }

    /**
     * 出了异常
     *
     * @param request
     * @param result
     * @param client
     */
    protected void onException(final RequestMessage<Invocation> request, final Result result, final Client client) {
        if (container != null) {
            //失败注销callback
            MessageHeader header = request.getHeader();
            container.removeCallback((String) header.getAttribute(HeadKey.callbackInsId));
        }
        //处理异常
        if (exceptionHandlers != null) {
            exceptionHandlers.forEach(h -> h.handle(client, result.getException()));
        }
    }

    /**
     * 转换成结果对象
     *
     * @param request  请求
     * @param response 应答
     * @param protocol 协议
     * @return
     */
    protected Result buildResult(final RequestMessage<Invocation> request, final Message response,
                                 final ClientProtocol protocol) {
        //拿到Response对象
        ResponsePayload payLoad = (ResponsePayload) response.getPayLoad();
        //返回值为空或为void，response可能为空
        if (payLoad == null) {
            payLoad = new ResponsePayload();
            response.setPayLoad(payLoad);
        }
        //根据协议拿到应答消息转换器，在网关调用会用到
        MessageConverter converter = protocol.inMessage();
        BiFunction<Message, Object, Object> function = converter == null ? null : converter.response();
        //构造返回值
        return payLoad.isError() ? new Result(request.getContext(), payLoad.getException(), response) :
                new Result(request.getContext(),
                        function == null ? payLoad.getResponse() :
                                function.apply(request, payLoad.getResponse()), response);
    }

    /**
     * 构建路由器
     *
     * @return
     */
    protected Router<RequestMessage<Invocation>> buildRouter(final URL url, final Class interfaceClass) {
        Router<RequestMessage<Invocation>> router = ROUTER.get(url.getString(Constants.ROUTER_OPTION));
        if (router != null) {
            router.setUrl(url);
            router.setClass(interfaceClass);
            router.setClassName(interfaceName);
            router.setup();
        }
        return router;
    }

    /**
     * 构建路由对象
     *
     * @param url
     * @param loadBalance
     * @return
     */
    protected Route buildRoute(final URL url, final LoadBalance loadBalance) {
        Route<RequestMessage<Invocation>, Result> route = ROUTE.get(url.getString(ROUTE_OPTION));
        if (route != null) {
            route.setUrl(url);
            route.setLoadBalance(loadBalance);
            route.setFunction(this::invokeRemote);
            if (route instanceof RouteFailover) {
                ((RouteFailover<RequestMessage<Invocation>, Result>) route).setRetryFunction(this::getFailoverPolicy);
            }
            route.setup();
        }
        return route;
    }

    @Override
    protected CompletableFuture<Result> doInvoke(final RequestMessage<Invocation> request) {
        //ConsumerInvokerHandler已经设置了class和method对象
        Invocation invocation = request.getPayLoad();
        invocation.setAlias(alias);
        invocation.setObject(config.getStub());

        MethodOption.Option option = options.getOption(invocation.getMethodName());
        request.setFailoverPolicy(option.getFailoverPolicy());
        //避免分组重试重复调用
        if (request.getCreateTime() <= 0) {
            request.setCreateTime(SystemClock.now());
        }
        if (request.getHeader().getTimeout() <= 0) {
            request.setTimeout(option.getTimeout());
            request.getHeader().setTimeout(option.getTimeout());
        }
        //设置实际的类名，而不是泛化类
        invocation.setClassName(interfaceName);
        //接口透传参数
        invocation.addAttachments(interfaceImplicits);
        //方法透传参数
        invocation.addAttachments(option.getImplicits());
        //透传处理
        transmits.forEach(o -> o.inject(request));

        //执行调用链
        return chain.invoke(request);
    }

    /**
     * 分发数据
     *
     * @param request
     * @return
     */
    protected CompletableFuture<Result> distribute(final RequestMessage<Invocation> request) {

        if (inJvm) {
            Exporter exporter = InvokerManager.getFirstExporter(this.exporter);
            if (exporter != null) {
                //本地调用，不需要透传标识
                return exporter.invoke(request);
            }
        }
        return distribution.distribute(request);
    }

    @Override
    protected CompletableFuture<Void> doOpen() {
        CompletableFuture<Void> result = new CompletableFuture<>();
        //注册
        register().whenComplete((v, t) -> {
            if (t == null) {
                logger.info("Success register consumer config " + name);
            }
        });
        //打开集群不需要等到注册成功，因为可以从本地文件恢复。打开之前，已经提前进行了订阅获取全局配置
        cluster.open(o -> {
            if (o.isSuccess()) {
                result.complete(null);
            } else {
                result.completeExceptionally(o.getThrowable());
            }
        });
        return result;
    }

    @Override
    protected CompletableFuture<Void> doClose() {
        //注销节点事件
        cluster.removeHandler(config);
        //从注册中心注销，最多重试1次
        CompletableFuture<Void> future1 = deregister().whenComplete((v, t) -> logger.info("Success deregister consumer config " + name));
        //取消配置订阅
        CompletableFuture<Void> future2 = unsubscribe().whenComplete((v, t) -> logger.info("Success unsubscribe consumer config " + name));
        //关闭集群
        final CompletableFuture<Void> future3 = new CompletableFuture<>();
        cluster.close(r -> {
            logger.info("Success close cluster " + name);
            future3.complete(null);
        });
        //关闭过滤链
        CompletableFuture<Void> future4 = chain.close().whenComplete((v, t) -> logger.info("Success close filter chain " + name));

        return CompletableFuture.allOf(future1, future2, future3, future4).whenComplete((v, t) -> {
            if (closing != null) {
                closing.accept(this, null);
            }
        });
    }

    /**
     * 注册
     *
     * @return
     */
    protected CompletableFuture<Void> register() {
        CompletableFuture<Void> result = new CompletableFuture<>();
        //注册
        if (registerUrl != null) {
            //URL里面注册的类是实际的interfaceClass，不是proxyClass
            registry.register(registerUrl).whenComplete((v, t) -> {
                if (t == null) {
                    result.complete(null);
                } else {
                    result.completeExceptionally(t);
                }
            });
        } else {
            result.complete(null);
        }
        return result;
    }

    /**
     * 从注册中心注销
     *
     * @return
     */
    protected CompletableFuture<Void> deregister() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (registerUrl != null) {
            //URL里面注册的类是实际的interfaceClass，不是proxyClass
            //TODO 要确保各个注册中心实现在服务有问题的情况下，能快速的注销掉
            registry.deregister(registerUrl, 1).whenComplete((r, t) -> future.complete(null));
        } else {
            future.complete(null);
        }
        return future;
    }

    /**
     * 取消配置订阅
     *
     * @return
     */
    protected CompletableFuture<Void> unsubscribe() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        //订阅
        if (subscribeUrl != null) {
            // todo 不能保证执行成功
            configure.unsubscribe(subscribeUrl, configHandler);
        }
        future.complete(null);
        return future;
    }

    @Override
    protected Throwable shutdownException() {
        return new ShutdownExecption("Refer is shutdown.", false);
    }

    public Cluster getCluster() {
        return cluster;
    }

    public ConsumerConfig<?> getConfig() {
        return config;
    }

    public Registry getRegistry() {
        return registry;
    }

}
