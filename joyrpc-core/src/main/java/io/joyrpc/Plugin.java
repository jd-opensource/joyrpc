package io.joyrpc;

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

import io.joyrpc.cache.CacheFactory;
import io.joyrpc.cache.CacheKeyGenerator;
import io.joyrpc.cluster.MetricHandler;
import io.joyrpc.cluster.candidate.Candidature;
import io.joyrpc.cluster.discovery.config.Configure;
import io.joyrpc.cluster.discovery.naming.Registar;
import io.joyrpc.cluster.discovery.registry.RegistryFactory;
import io.joyrpc.cluster.distribution.*;
import io.joyrpc.cluster.distribution.loadbalance.adaptive.Arbiter;
import io.joyrpc.cluster.distribution.loadbalance.adaptive.Election;
import io.joyrpc.cluster.distribution.loadbalance.adaptive.Judge;
import io.joyrpc.codec.CodecType;
import io.joyrpc.codec.checksum.Checksum;
import io.joyrpc.codec.compression.Compression;
import io.joyrpc.codec.crypto.Decryptor;
import io.joyrpc.codec.crypto.Encryptor;
import io.joyrpc.codec.crypto.Signature;
import io.joyrpc.codec.digester.Digester;
import io.joyrpc.codec.serialization.*;
import io.joyrpc.config.InterfaceOptionFactory;
import io.joyrpc.config.validator.InterfaceValidator;
import io.joyrpc.context.ConfigEventHandler;
import io.joyrpc.context.Configurator;
import io.joyrpc.context.ContextSupplier;
import io.joyrpc.context.Environment;
import io.joyrpc.context.injection.NodeReqInjection;
import io.joyrpc.context.injection.RespInjection;
import io.joyrpc.context.injection.Transmit;
import io.joyrpc.event.EventBus;
import io.joyrpc.expression.ExpressionProvider;
import io.joyrpc.extension.*;
import io.joyrpc.filter.ConsumerFilter;
import io.joyrpc.filter.ProviderFilter;
import io.joyrpc.health.Doctor;
import io.joyrpc.invoker.ExceptionHandler;
import io.joyrpc.invoker.FilterChainFactory;
import io.joyrpc.invoker.GroupInvoker;
import io.joyrpc.metric.DashboardFactory;
import io.joyrpc.permission.Authentication;
import io.joyrpc.permission.Authorization;
import io.joyrpc.permission.Identification;
import io.joyrpc.protocol.ClientProtocol;
import io.joyrpc.protocol.MessageHandler;
import io.joyrpc.protocol.Protocol.ProtocolVersion;
import io.joyrpc.protocol.ServerProtocol;
import io.joyrpc.proxy.GrpcFactory;
import io.joyrpc.proxy.JCompiler;
import io.joyrpc.proxy.ProxyFactory;
import io.joyrpc.thread.ThreadPool;
import io.joyrpc.transport.EndpointFactory;
import io.joyrpc.transport.channel.ChannelManagerFactory;
import io.joyrpc.transport.http.HttpClient;
import io.joyrpc.transport.telnet.TelnetHandler;
import io.joyrpc.transport.transport.TransportFactory;

/**
 * @date: 23/1/2019
 */
public interface Plugin {

    /**
     * 接口验证插件
     */
    ExtensionPoint<InterfaceValidator, String> INTERFACE_VALIDATOR = new ExtensionPointLazy<>(InterfaceValidator.class);
    /**
     * 消息处理器插件
     */
    ExtensionPoint<MessageHandler, Integer> MESSAGE_HANDLER = new ExtensionPointLazy<>(MessageHandler.class);

    /**
     * 插件选择器
     */
    ExtensionSelector<MessageHandler, Integer, Integer, MessageHandler> MESSAGE_HANDLER_SELECTOR = new MessageHandlerSelector(MESSAGE_HANDLER);

    /**
     * 过滤链构建器
     */
    ExtensionPoint<FilterChainFactory, String> FILTER_CHAIN_FACTORY = new ExtensionPointLazy<>(FilterChainFactory.class);

    /**
     * 消费者过滤器插件
     */
    ExtensionPoint<ConsumerFilter, String> CONSUMER_FILTER = new ExtensionPointLazy<>(ConsumerFilter.class);
    /**
     * 服务提供者过滤器插件
     */
    ExtensionPoint<ProviderFilter, String> PROVIDER_FILTER = new ExtensionPointLazy<>(ProviderFilter.class);
    /**
     * 缓存生成器插件
     */
    ExtensionPoint<CacheKeyGenerator, String> CACHE_KEY_GENERATOR = new ExtensionPointLazy<>(CacheKeyGenerator.class);

    /**
     * 业务线程插件.
     */
    ExtensionPoint<ThreadPool, String> THREAD_POOL = new ExtensionPointLazy<>(ThreadPool.class);

    /**
     * 上下文传递扩展
     */
    ExtensionPoint<Transmit, String> TRANSMIT = new ExtensionPointLazy<>(Transmit.class);

    /**
     * 节点请求注入
     */
    ExtensionPoint<NodeReqInjection, String> NODE_REQUEST_INJECTION = new ExtensionPointLazy<>(NodeReqInjection.class);

    /**
     * 应答注入
     */
    ExtensionPoint<RespInjection, String> RESPONSE_INJECTION = new ExtensionPointLazy<>(RespInjection.class);

    /**
     * 配置
     */
    ExtensionPoint<Configurator, String> CONFIGURATOR = new ExtensionPointLazy<>(Configurator.class);

    /**
     * 分组路由插件
     */
    ExtensionPoint<GroupInvoker, String> GROUP_ROUTE = new ExtensionPointLazy<>(GroupInvoker.class);

    /**
     * 路由插件
     */
    ExtensionPoint<NodeSelector, String> NODE_SELECTOR = new ExtensionPointLazy<>(NodeSelector.class);

    /**
     * 注册中心全局配置变更事件通知插件
     */
    ExtensionPoint<ConfigEventHandler, String> CONFIG_EVENT_HANDLER = new ExtensionPointLazy<>(ConfigEventHandler.class);

    /**
     * 面板工程类
     */
    ExtensionPoint<DashboardFactory, String> DASHBOARD_FACTORY = new ExtensionPointLazy<>(DashboardFactory.class);

    /**
     * 分发异常处理
     */
    ExtensionPoint<ExceptionHandler, String> EXCEPTION_HANDLER = new ExtensionPointLazy<>(ExceptionHandler.class);

    /**
     * 泛化参数序列化插件
     */
    ExtensionPoint<GenericSerializer, String> GENERIC_SERIALIZER = new ExtensionPointLazy<>(GenericSerializer.class);

    /**
     * 表达式插件
     */
    ExtensionPoint<ExpressionProvider, String> EXPRESSION_PROVIDER = new ExtensionPointLazy<>(ExpressionProvider.class);

    /**
     * 接口选项工厂类
     */
    ExtensionPoint<InterfaceOptionFactory, String> INTERFACE_OPTION_FACTORY = new ExtensionPointLazy<>(InterfaceOptionFactory.class);

    /**
     * 编解码插件选择器
     */
    class MessageHandlerSelector extends ExtensionSelector<MessageHandler, Integer, Integer, MessageHandler> {
        /**
         * ID数组
         */
        protected volatile MessageHandler[] handlers;

        /**
         * 构造函数
         *
         * @param extensionPoint
         */
        public MessageHandlerSelector(ExtensionPoint<MessageHandler, Integer> extensionPoint) {
            super(extensionPoint, null);
        }

        @Override
        public MessageHandler select(final Integer condition) {
            if (handlers == null) {
                synchronized (this) {
                    if (handlers == null) {
                        final MessageHandler[] handlers = new MessageHandler[127];
                        extensionPoint.metas().forEach(o -> {
                            MessageHandler handler = o.getTarget();
                            handlers[handler.type()] = handler;
                        });
                        this.handlers = handlers;
                    }
                }
                ;
            }
            return handlers[condition];
        }
    }

    /**
     * 环境插件
     */
    ExtensionPoint<Environment, String> ENVIRONMENT = new ExtensionPointLazy<>(Environment.class);

    /**
     * 全局变量提供者插件
     */
    ExtensionPoint<ContextSupplier, String> CONTEXT_SUPPLIER = new ExtensionPointLazy<>(ContextSupplier.class);


    /**
     * 事件总线
     */
    ExtensionPoint<EventBus, String> EVENT_BUS = new ExtensionPointLazy<>(EventBus.class);

    /**
     * 缓存插件
     */
    ExtensionPoint<CacheFactory, String> CACHE = new ExtensionPointLazy<>(CacheFactory.class);

    /**
     * 序列化类型提供者
     */
    ExtensionPoint<Serialization, String> SERIALIZATION = new ExtensionPointLazy<>(Serialization.class);

    /**
     * 身份认证插件
     */
    ExtensionPoint<Authentication, String> AUTHENTICATOR = new ExtensionPointLazy<>(Authentication.class);

    /**
     * 身份插件
     */
    ExtensionPoint<Identification, String> IDENTIFICATION = new ExtensionPointLazy<>(Identification.class);
    /**
     * 权限认证
     */
    ExtensionPoint<Authorization, String> AUTHORIZATION = new ExtensionPointLazy<>(Authorization.class);

    /**
     * JSON提供者
     */
    ExtensionPoint<Json, String> JSON = new ExtensionPointLazy<>(Json.class);

    /**
     * JSON提供者
     */
    ExtensionPoint<Xml, String> XML = new ExtensionPointLazy<>(Xml.class);

    /**
     * 加密算法
     */
    ExtensionPoint<Encryptor, String> ENCRYPTOR = new ExtensionPointLazy<>(Encryptor.class);

    /**
     * 解密算法
     */
    ExtensionPoint<Decryptor, String> DECRYPTOR = new ExtensionPointLazy<>(Decryptor.class);

    /**
     * 签名算法
     */
    ExtensionPoint<Signature, String> SIGNATURE = new ExtensionPointLazy<>(Signature.class);

    /**
     * 摘要算法
     */
    ExtensionPoint<Digester, String> DIGESTER = new ExtensionPointLazy<>(Digester.class);

    /**
     * 压缩插件
     */
    ExtensionPoint<Compression, String> COMPRESSION = new ExtensionPointLazy<>(Compression.class);

    /**
     * 校验和插件
     */
    ExtensionPoint<Checksum, String> CHECKSUM = new ExtensionPointLazy<>(Checksum.class);

    /**
     * Proxy插件
     */
    ExtensionPoint<ProxyFactory, String> PROXY = new ExtensionPointLazy<>(ProxyFactory.class);

    /**
     * 编译器
     */
    ExtensionPoint<JCompiler, String> COMPILER = new ExtensionPointLazy<>(JCompiler.class);

    /**
     * GRPC工厂插件
     */
    ExtensionPoint<GrpcFactory, String> GRPC_FACTORY = new ExtensionPointLazy<>(GrpcFactory.class);

    /**
     * 医生插件
     */
    ExtensionPoint<Doctor, String> DOCTOR = new ExtensionPointLazy<>(Doctor.class);

    /**
     * 指标处理器插件
     */
    ExtensionPoint<MetricHandler, String> METRIC_HANDLER = new ExtensionPointLazy<>(MetricHandler.class);

    /**
     * 客户端协议
     */
    ExtensionPoint<ClientProtocol, String> CLIENT_PROTOCOL = new ExtensionPointLazy<>(ClientProtocol.class);

    /**
     * 客户端协议选择器，匹配最优的协议
     */
    ExtensionSelector<ClientProtocol, String, ProtocolVersion, ClientProtocol> CLIENT_PROTOCOL_SELECTOR = new ExtensionSelector<>(CLIENT_PROTOCOL,
            new Selector.CacheSelector<>((extensions, protocolVersion) -> {
                String name = protocolVersion.getName();
                String version = protocolVersion.getVersion();
                //协议版本为空，直接根据协议名称获取
                if (version == null || version.isEmpty()) {
                    return extensions.get(name);
                }
                //根据版本获取
                ClientProtocol protocol = extensions.get(version);
                if (protocol == null && name != null && !name.isEmpty()) {
                    String n;
                    //版本没有找到，则按照名称取优先级最高的版本
                    for (ExtensionMeta<ClientProtocol, String> meta : extensions.metas()) {
                        //插件名称
                        n = meta.getExtension().getName();
                        //以指定名称开头，如joyrpc2以joyrpc开头
                        if (n.startsWith(name)) {
                            try {
                                //如果以数字结尾
                                Integer.valueOf(n.substring(name.length()));
                                protocol = meta.getTarget();
                                break;
                            } catch (NumberFormatException e) {
                                if (n.equals(name) && protocol == null) {
                                    //还没有找到高版本的协议，但找到了与name名称相同的协议，暂时先赋值
                                    protocol = meta.getTarget();
                                }
                            }
                        }
                    }

                }
                return protocol;
            }));

    /**
     * 服务端协议
     */
    ExtensionPoint<ServerProtocol, String> SERVER_PROTOCOL = new ExtensionPointLazy<>(ServerProtocol.class);

    /**
     * 自定义类型的编解码器
     */
    ExtensionPoint<CustomCodec, Class> CUSTOM_CODEC = new ExtensionPointLazy<>(CustomCodec.class);

    /**
     * ChannelManager插件
     */
    ExtensionPoint<ChannelManagerFactory, String> CHANNEL_MANAGER_FACTORY = new ExtensionPointLazy<>(ChannelManagerFactory.class);

    /**
     * TransportFactory插件
     */
    ExtensionPoint<TransportFactory, String> TRANSPORT_FACTORY = new ExtensionPointLazy<>(TransportFactory.class);

    ExtensionPoint<EndpointFactory, String> ENDPOINT_FACTORY = new ExtensionPointLazy<>(EndpointFactory.class);

    /**
     * 候选者算法插件
     */
    ExtensionPoint<Candidature, String> CANDIDATURE = new ExtensionPointLazy<>(Candidature.class);
    /**
     * 负载均衡插件
     */
    ExtensionPoint<LoadBalance, String> LOADBALANCE = new ExtensionPointLazy<>(LoadBalance.class);

    /**
     * 各维度LB服务评分链
     */
    ExtensionPoint<Judge, String> JUDGE = new ExtensionPointLazy<Judge, String>(Judge.class);

    /**
     * 综合评分
     */
    ExtensionPoint<Arbiter, String> ARBITER = new ExtensionPointLazy<Arbiter, String>(Arbiter.class);

    /**
     * 根据评分结果进行选择
     */
    ExtensionPoint<Election, String> ELECTION = new ExtensionPointLazy(Election.class);

    /**
     * 服务端限流器
     */
    ExtensionPoint<RateLimiter, String> LIMITER = new ExtensionPointLazy<>(RateLimiter.class);

    /**
     * 路由策略
     */
    ExtensionPoint<Router, String> ROUTER = new ExtensionPointLazy<>(Router.class);

    /**
     * 重试目标节点选择器
     */
    ExtensionPoint<FailoverSelector, String> FAILOVER_SELECTOR = new ExtensionPointLazy<>(FailoverSelector.class);

    /**
     * 异常重试判断
     */
    ExtensionPoint<ExceptionPredication, String> EXCEPTION_PREDICATION = new ExtensionPointLazy<>(ExceptionPredication.class);

    /**
     * 目录服务插件
     */
    ExtensionPoint<Registar, String> REGISTAR = new ExtensionPointLazy<>(Registar.class);

    /**
     * 配置服务插件
     */
    ExtensionPoint<Configure, String> CONFIGURE = new ExtensionPointLazy<>(Configure.class);

    /**
     * Telnet命令处理器
     */
    ExtensionPoint<TelnetHandler, String> TELNET_HANDLER = new ExtensionPointLazy<>(TelnetHandler.class);

    /**
     * HTTP客户端
     */
    ExtensionPoint<HttpClient, String> HTTP_CLIENT = new ExtensionPointLazy<>(HttpClient.class);

    /**
     * 注册中心插件
     */
    ExtensionPoint<RegistryFactory, String> REGISTRY = new ExtensionPointLazy<>(RegistryFactory.class);

    /**
     * 序列化选择器
     */
    ExtensionSelector<Serialization, String, Byte, Serialization> SERIALIZATION_SELECTOR = new Plugin.CodecSelector<>(SERIALIZATION);
    /**
     * 压缩选择器
     */
    ExtensionSelector<Compression, String, Byte, Compression> COMPRESSION_SELECTOR = new Plugin.CodecSelector<>(COMPRESSION);

    /**
     * 校验和选择器
     */
    ExtensionSelector<Checksum, String, Byte, Checksum> CHECKSUM_SELECTOR = new Plugin.CodecSelector<>(CHECKSUM);

    /**
     * 编解码插件选择器
     *
     * @param <T>
     */
    class CodecSelector<T extends CodecType> extends ExtensionSelector<T, String, Byte, T> {
        /**
         * ID数组
         */
        protected volatile CodecType[] codecs;

        /**
         * 构造函数
         *
         * @param extensionPoint
         */
        public CodecSelector(ExtensionPoint<T, String> extensionPoint) {
            super(extensionPoint, null);
        }

        @Override
        public T select(final Byte condition) {
            if (codecs == null) {
                synchronized (this) {
                    if (codecs == null) {
                        CodecType[] codecs = new CodecType[127];
                        extensionPoint.metas().forEach(o -> {
                            T target = o.getTarget();
                            codecs[target.getTypeId()] = target;
                        });
                        this.codecs = codecs;
                    }
                }
            }
            return (T) codecs[condition];
        }
    }
}
