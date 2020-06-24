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

import io.joyrpc.CallbackListener;
import io.joyrpc.Plugin;
import io.joyrpc.Result;
import io.joyrpc.codec.compression.Compression;
import io.joyrpc.config.AbstractInterfaceOption;
import io.joyrpc.config.InterfaceOption;
import io.joyrpc.config.inner.InnerConsumerOption;
import io.joyrpc.constants.Constants;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.exception.MethodOverloadException;
import io.joyrpc.exception.RpcException;
import io.joyrpc.extension.MapParametric;
import io.joyrpc.extension.Parametric;
import io.joyrpc.extension.URL;
import io.joyrpc.extension.WrapperParametric;
import io.joyrpc.protocol.MsgType;
import io.joyrpc.protocol.message.*;
import io.joyrpc.thread.NamedThreadFactory;
import io.joyrpc.thread.ThreadPool;
import io.joyrpc.transport.session.Session;
import io.joyrpc.transport.transport.ChannelTransport;
import io.joyrpc.transport.transport.Transport;
import io.joyrpc.util.GenericMethod;
import io.joyrpc.util.GrpcMethod;
import io.joyrpc.util.network.Ipv4;

import javax.validation.Validator;
import java.io.Closeable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.joyrpc.Plugin.PROXY;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.util.ClassUtils.getPublicMethod;
import static io.joyrpc.util.ClassUtils.isReturnFuture;

/**
 * 回调管理器
 */
public class CallbackManager implements Closeable {

    /**
     * 默认客户端异步返回调用线程池大小
     */
    public final static int DEFAULT_CLIENT_CALLBACK_CORE_THREADS = 20;

    /**
     * 默认客户端异步返回调用线程池大小
     */
    public final static int DEFAULT_CLIENT_CALLBACK_MAX_THREADS = 200;

    /**
     * 默认客户端异步返回调用线程池队列大小
     */
    public final static int DEFAULT_CLIENT_CALLBACK_QUEUE = 256;

    protected static final AtomicLong counter = new AtomicLong(0);

    /**
     * 消费者回调
     */
    protected CallbackContainer consumer = new ConsumerCallbackContainer();

    /**
     * 提供者回调
     */
    protected CallbackContainer producer = new ProducerCallbackContainer();

    /**
     * 回调线程池
     */
    protected ThreadPoolExecutor callbackThreadPool;

    public CallbackContainer getConsumer() {
        return consumer;
    }

    public CallbackContainer getProducer() {
        return producer;
    }

    /**
     * 获取线程池
     *
     * @return 回调线程池
     */
    public ThreadPoolExecutor getThreadPool() {
        if (callbackThreadPool == null) {
            synchronized (this) {
                if (callbackThreadPool == null) {

                    Parametric parametric = new MapParametric(GlobalContext.getGlobalSetting());
                    int coreSize = parametric.getPositive(Constants.SETTING_CALLBACK_POOL_CORE_SIZE, DEFAULT_CLIENT_CALLBACK_CORE_THREADS);
                    int maxSize = parametric.getPositive(Constants.SETTING_CALLBACK_POOL_MAX_SIZE, DEFAULT_CLIENT_CALLBACK_MAX_THREADS);
                    int queueSize = parametric.getPositive(Constants.SETTING_CALLBACK_POOL_QUEUE, DEFAULT_CLIENT_CALLBACK_QUEUE);

                    URL url = new URL("CB", Ipv4.getLocalIp(), 0, new HashMap<String, String>() {{
                        put(Constants.CORE_SIZE_OPTION.getName(), String.valueOf(coreSize));
                        put(Constants.MAX_SIZE_OPTION.getName(), String.valueOf(maxSize));
                    }});

                    callbackThreadPool = Plugin.THREAD_POOL.get().get(url, new NamedThreadFactory("RPC-CB-", true),
                            o -> ThreadPool.QUEUE_FUNCTION.apply(queueSize, false));
                }
            }
        }
        return callbackThreadPool;
    }

    @Override
    public void close() {
        consumer.close();
        producer.close();
    }

    /**
     * 回调容器
     */
    protected static abstract class AbstractCallbackContainer<T extends CallbackInvoker> implements CallbackContainer {

        /**
         * 回调
         */
        protected Map<String, T> callbacks = new ConcurrentHashMap<>();

        /**
         * 回调
         */
        protected Map<Transport, Set<String>> channelIds = new ConcurrentHashMap<>();

        @Override
        public List<CallbackInvoker> removeCallback(final ChannelTransport transport) {
            List<CallbackInvoker> result = new LinkedList<>();
            if (transport != null) {
                Set<String> ids = channelIds.remove(transport);
                if (ids != null && !ids.isEmpty()) {
                    CallbackInvoker invoker;
                    for (String id : ids) {
                        invoker = callbacks.remove(id);
                        if (invoker != null) {
                            result.add(invoker);
                        }
                    }
                }
            }

            return result;
        }

        @Override
        public CallbackInvoker removeCallback(final String callbackId) {
            if (callbackId == null) {
                return null;
            }
            CallbackInvoker invoker = callbacks.remove(callbackId);
            if (invoker != null) {
                Set<String> set = channelIds.get(invoker.getTransport());
                if (set != null) {
                    set.remove(callbackId);
                }
            }
            return invoker;
        }

        @Override
        public CallbackInvoker getInvoker(final String callbackId) {
            return callbackId == null ? null : callbacks.get(callbackId);
        }

        @Override
        public void close() {
            callbacks.clear();
            channelIds.clear();
        }

        /**
         * 添加调用器
         *
         * @param callbackId 回调ID
         * @param transport  通道
         * @param function   函数
         */
        protected T add(String callbackId, ChannelTransport transport, BiFunction<String, ChannelTransport, T> function) {
            T callback = function.apply(callbackId, transport);
            //回调很少，可以使用CopyOnWriteArraySet
            channelIds.computeIfAbsent(transport, c -> new CopyOnWriteArraySet<>()).add(callbackId);
            callbacks.put(callbackId, callback);
            return callback;
        }
    }

    /**
     * 消费者回调容器
     */
    protected static class ConsumerCallbackContainer extends AbstractCallbackContainer<ConsumerCallbackInvoker> {

        @Override
        public void addCallback(final RequestMessage<Invocation> request, final ChannelTransport transport) {
            CallbackMethod meta = request.getOption().getCallback();
            if (meta == null) {
                return;
            }
            Invocation invocation = request.getPayLoad();
            Object[] args = invocation.getArgs();
            Object callbackArg = args[meta.index];
            //和callback对象绑定
            String callbackId = String.valueOf(System.identityHashCode(callbackArg));
            //注入回调ID，便于外部感知，再需要的时候进行删除
            if (callbackArg instanceof CallbackListener) {
                ((CallbackListener) callbackArg).setCallbackId(callbackId);
            }
            //header设置callbackId
            request.getHeader().addAttribute(HEAD_CALLBACK_INSID, callbackId);
            //callback参数置空
            args[meta.index] = null;
            //保存回调对象
            add(callbackId, transport, (c, t) -> new ConsumerCallbackInvoker(meta.getParameter().getType(), callbackArg, t));
        }
    }

    /**
     * 服务提供者回调容器
     */
    protected static class ProducerCallbackContainer extends AbstractCallbackContainer<ProducerCallbackInvoker> {

        @Override
        public void addCallback(final RequestMessage<Invocation> request, final ChannelTransport transport) {
            CallbackMethod meta = request.getOption().getCallback();
            if (meta == null) {
                return;
            }
            Invocation invocation = request.getPayLoad();
            MessageHeader header = request.getHeader();
            String callbackId = (String) header.getAttribute(HEAD_CALLBACK_INSID);
            if (callbackId == null || callbackId.isEmpty()) {
                throw new RpcException("callbackId can not be empty! ");
            }
            ProducerCallbackInvoker handler = add(callbackId, transport,
                    (c, t) -> new ProducerCallbackInvoker(
                            c, meta.getParameter().getType(),
                            m -> meta.getParameterTypes(m),
                            header,
                            t,
                            k -> callbacks.remove(k)));

            invocation.getArgs()[meta.index] = handler.callback;
        }

    }

    /**
     * 消费者注册用于处理服务端回调消息的处理器
     */
    protected static abstract class AbstractCallbackInvoker implements CallbackInvoker {

        /**
         * 回调接口
         */
        protected Class<?> callbackClass;
        /**
         * 回调
         */
        protected Object callback;
        /**
         * 连接
         */
        protected ChannelTransport transport;

        @Override
        public CompletableFuture<Result> invoke(final RequestMessage<Invocation> request) {
            // 回调的场景少，直接采用反射
            CompletableFuture<Result> result = new CompletableFuture<>();
            try {
                Invocation payLoad = request.getPayLoad();
                Method method = getPublicMethod(callbackClass, payLoad.getMethodName());
                Object value = method.invoke(callback, payLoad.getArgs());
                //异步回调
                CompletableFuture<?> future = isReturnFuture(callbackClass, method) ?
                        (CompletableFuture<?>) value : CompletableFuture.completedFuture(value);
                future.whenComplete((v, t) -> {
                    if (t != null) {
                        result.complete(new Result(request.getContext(), t));
                    } else {
                        result.complete(new Result(request.getContext(), v));
                    }
                });
            } catch (Throwable e) {
                result.completedFuture(new Result(request.getContext(), e));
            }
            return result;
        }

        @Override
        public ChannelTransport getTransport() {
            return transport;
        }

        @Override
        public void recallback() {

        }
    }

    /**
     * 消费者注册用于处理服务端回调消息的处理器
     */
    protected static class ConsumerCallbackInvoker extends AbstractCallbackInvoker {

        public ConsumerCallbackInvoker(final Class<?> callbackClass, final Object callback, final ChannelTransport transport) {
            this.callbackClass = callbackClass;
            this.callback = callback;
            this.transport = transport;
        }

        @Override
        public void recallback() {
            if (callback instanceof CallbackListener) {
                ((CallbackListener) callback).recallback();
            }
        }
    }

    /**
     * 回调
     */
    protected static class ProducerCallbackInvoker extends AbstractCallbackInvoker {
        /**
         * ID
         */
        protected String id;
        /**
         * 参数类型
         */
        protected Function<Method, Class[]> typeFunc;
        /**
         * 请求头
         */
        protected MessageHeader header;
        /**
         * 接口选项
         */
        protected InterfaceOption option;
        /**
         * 执行完毕消费者
         */
        protected Consumer<String> closing;

        public ProducerCallbackInvoker(final String id,
                                       final Class<?> callbackClass,
                                       final Function<Method, Class[]> typeFunc,
                                       final MessageHeader header,
                                       final ChannelTransport transport,
                                       final Consumer<String> closing) {
            this.id = id;
            this.callbackClass = callbackClass;
            this.typeFunc = typeFunc;
            this.header = header;
            this.transport = transport;
            this.closing = closing;
            this.option = new CallbackRequestOption(callbackClass, callbackClass.getName());
            this.callback = PROXY.get().getProxy(callbackClass, this::doInvoke);
        }

        /**
         * 调用
         *
         * @param proxy  对象
         * @param method 方法
         * @param param  参数
         * @return 结果
         * @throws Throwable 异常
         */
        protected Object doInvoke(final Object proxy, final Method method, final Object[] param) throws Throwable {
            String methodName = method.getName();
            Class[] paramTypes = method.getParameterTypes();
            if ("toString".equals(methodName) && paramTypes.length == 0) {
                return this.toString();
            } else if ("hashCode".equals(methodName) && paramTypes.length == 0) {
                return this.hashCode();
            } else if ("equals".equals(methodName) && paramTypes.length == 1) {
                return this.equals(param[0]);
            } else if (Modifier.isStatic(method.getModifiers())) {
                //静态方法
                return method.invoke(proxy, param);
            }
            Session session = transport.session();
            //回调不需要别名,需要设置真实的参数类型
            Invocation invocation = new Invocation(callbackClass, null, method, param, typeFunc.apply(method));
            invocation.setCallback(true);
            //已经设置了创建时间
            RequestMessage<Invocation> request = RequestMessage.build(invocation);
            request.setOption(option.getOption(methodName));
            request.setUrl(transport.getUrl());
            MessageHeader rh = request.getHeader();
            //回调请求
            rh.setMsgType(MsgType.CallbackReq.getType());
            rh.setProtocolType(header.getProtocolType());
            rh.setSerialization(header.getSerialization());
            rh.setCompression(session == null ? Compression.NONE : session.getCompressionType());
            rh.addAttribute(HEAD_CALLBACK_INSID, id);
            //TODO 缺乏参数注入
            //超时时间放在后面，Invocation已经注入了请求上下文参数，隐藏参数等等
            if (request.getHeader().getTimeout() <= 0) {
                Parametric parametric = new MapParametric(invocation.getAttachments());
                int timeout = parametric.getPositive(HIDDEN_KEY_TIME_OUT, Constants.DEFAULT_TIMEOUT);
                //超时时间
                request.setTimeout(timeout);
                rh.setTimeout(timeout);
            }

            //为了兼容，目前只支持同步调用
            ResponseMessage<ResponsePayload> message = (ResponseMessage<ResponsePayload>) transport.sync(request, request.getTimeout());
            ResponsePayload payLoad = message.getPayLoad();
            if (payLoad.isError()) {
                throw payLoad.getException();
            }
            return payLoad.getResponse();
        }

    }

    /**
     * 回调请求选项
     */
    protected static class CallbackRequestOption extends AbstractInterfaceOption {

        public CallbackRequestOption(Class<?> interfaceClass, String interfaceName) {
            super(interfaceClass, interfaceName, URL.valueOf("callback://localhost"));
            buildOptions();
        }

        @Override
        protected InnerMethodOption create(WrapperParametric parametric) {
            GrpcMethod grpcMethod = getMethod(parametric.getName());
            return new CallbackInnerMethodOption(grpcMethod);
        }

        /**
         * 获取回调方法
         *
         * @param methodName 方法名称
         * @return 回调方法对象
         */
        protected GrpcMethod getMethod(final String methodName) {
            if (generic) {
                return null;
            }
            try {
                return getPublicMethod(interfaceClass, methodName, GRPC_TYPE_FUNCTION);
            } catch (NoSuchMethodException | MethodOverloadException e) {
                return null;
            }
        }

        /**
         * 方法选项
         */
        protected static class CallbackInnerMethodOption extends InnerMethodOption {

            /**
             * 构造函数
             *
             * @param grpcMethod GRPC方法
             */
            public CallbackInnerMethodOption(GrpcMethod grpcMethod) {
                super(grpcMethod, null, null, 0, null, null,
                        null, null, false, false, null);
            }
        }
    }


}
