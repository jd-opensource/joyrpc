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

import io.joyrpc.Callback;
import io.joyrpc.Plugin;
import io.joyrpc.Result;
import io.joyrpc.codec.compression.Compression;
import io.joyrpc.constants.Constants;
import io.joyrpc.constants.ExceptionCode;
import io.joyrpc.constants.HeadKey;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.exception.InitializationException;
import io.joyrpc.exception.RpcException;
import io.joyrpc.extension.Parametric;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.MsgType;
import io.joyrpc.protocol.message.*;
import io.joyrpc.thread.ThreadPool;
import io.joyrpc.transport.session.Session;
import io.joyrpc.transport.transport.ChannelTransport;
import io.joyrpc.transport.transport.Transport;
import io.joyrpc.thread.NamedThreadFactory;
import io.joyrpc.util.network.Ipv4;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static io.joyrpc.Plugin.PROXY;
import static io.joyrpc.util.ClassUtils.getPublicMethod;

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
     * 回调元数据
     */
    protected Map<Class, Map<String, CallbackMeta>> metas = new ConcurrentHashMap<>();

    /**
     * 消费者回调
     */
    protected CallbackContainer consumer = new ConsumerCallbackContainer(this::getCallbackMeta);

    /**
     * 提供者回调
     */
    protected CallbackContainer producer = new ProducerCallbackContainer(this::getCallbackMeta);

    /**
     * 回调线程池
     */
    protected ThreadPoolExecutor callbackThreadPool;

    /**
     * 注册接口
     *
     * @param iface
     */
    public boolean register(final Class iface) {
        if (iface == null) {
            return false;
        }
        //获取该类型有回调参数的方法信息
        List<CallbackMeta> callbacks = new LinkedList<>();
        List<Method> methods = getPublicMethod(iface);
        for (Method method : methods) {
            CallbackMeta meta = compute(method);
            if (meta != null) {
                // 需要解析出Callback<T>里的T的实际类型
                Type type = meta.parameter.getParameterizedType();
                if (type instanceof ParameterizedType) {
                    Type[] actualTypes = ((ParameterizedType) type).getActualTypeArguments();
                    if (actualTypes.length == 2) {
                        meta.parameterType = getRealClass(actualTypes[0]);
                        meta.returnType = getRealClass(actualTypes[1]);
                    }
                }
                if (meta.parameterType == null) {
                    // 抛出转换异常 表示为"?"泛化类型， java.lang.ClassCastException:
                    throw new InitializationException(String.format("Method Must set actual type of Callback, %s", method), ExceptionCode.COMMON_CALL_BACK_ERROR);
                }
                callbacks.add(meta);
            }
        }
        //如果有回调参数的方法
        if (!callbacks.isEmpty()) {
            Map<String, CallbackMeta> methodMetas = metas.computeIfAbsent(iface, o -> new ConcurrentHashMap<>(callbacks.size()));
            callbacks.forEach(o -> methodMetas.put(o.method.getName(), o));

            return true;
        }
        return false;
    }

    public CallbackContainer getConsumer() {
        return consumer;
    }

    public CallbackContainer getProducer() {
        return producer;
    }

    /**
     * 获取线程池
     *
     * @return
     */
    public ThreadPoolExecutor getThreadPool() {
        if (callbackThreadPool == null) {
            synchronized (this) {
                if (callbackThreadPool == null) {

                    Parametric parametric = GlobalContext.asParametric(Constants.GLOBAL_SETTING);
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
        metas.clear();
    }

    /**
     * 回去回调元数据
     *
     * @param clazz
     * @param methodName
     * @return
     */
    protected CallbackMeta getCallbackMeta(final Class clazz, final String methodName) {
        if (clazz == null || methodName == null) {
            return null;
        }
        Map<String, CallbackMeta> methodMetas = metas.get(clazz);
        return methodMetas == null ? null : methodMetas.get(methodName);
    }

    /**
     * 生成回调元数据
     *
     * @param method
     * @return
     */
    protected CallbackMeta compute(final Method method) {
        Parameter result = null;
        int index = 0;
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (isCallbackInterface(parameters[i].getType())) {
                if (result != null) {
                    throw new InitializationException("Illegal callback parameter at method " + method.getName()
                            + ",just allow one callback parameter", ExceptionCode.COMMON_CALL_BACK_ERROR);
                }
                result = parameters[i];
                index = i;
            }
        }
        return result == null ? null : new CallbackMeta(method, index, result);
    }

    /**
     * 是否是回调接口
     *
     * @param clazz
     * @return
     */
    public static boolean isCallbackInterface(final Class clazz) {
        //约定规范只能是Callback类，不能是子类或实现类
        if (Callback.class.isAssignableFrom(clazz)) {
            return true;
        }
        return false;
    }

    /**
     * 获取真实类型
     *
     * @param actualType
     * @return
     */
    protected Class getRealClass(final Type actualType) {
        if (actualType instanceof ParameterizedType) {
            // 例如 Callback<List<String>>
            Type rawType = ((ParameterizedType) actualType).getRawType();
            if (rawType instanceof Class) {
                return (Class) rawType;
            }
        } else if (actualType instanceof Class) {
            // 普通的 Callback<String>
            return (Class) actualType;
        }
        return null;
    }


    /**
     * 回调元数据
     */
    protected static class CallbackMeta {
        /**
         * 所属方法
         */
        protected Method method;
        /**
         * 索引
         */
        protected int index;
        /**
         * 参数索引
         */
        protected Parameter parameter;
        /**
         * 参数真实类型
         */
        protected Class parameterType;
        /**
         * 返回值真实类型
         */
        protected Class returnType;

        public CallbackMeta(Method method, int index, Parameter parameter) {
            this.method = method;
            this.index = index;
            this.parameter = parameter;
        }

        public int getIndex() {
            return index;
        }

        public Parameter getParameter() {
            return parameter;
        }

        public Class getParameterType() {
            return parameterType;
        }

        public Class getReturnType() {
            return returnType;
        }

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

        protected BiFunction<Class, String, CallbackMeta> function;

        public AbstractCallbackContainer(BiFunction<Class, String, CallbackMeta> function) {
            this.function = function;
        }

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
         * @param callbackId
         * @param transport
         * @param function
         */
        protected T add(String callbackId, ChannelTransport transport, BiFunction<String, ChannelTransport, T> function) {
            channelIds.computeIfAbsent(transport, c -> new CopyOnWriteArraySet()).add(callbackId);
            T callback = function.apply(callbackId, transport);
            callbacks.put(callbackId, callback);
            return callback;
        }
    }

    /**
     * 消费者回调容器
     */
    protected static class ConsumerCallbackContainer extends AbstractCallbackContainer<ConsumerCallbackInvoker> {

        public ConsumerCallbackContainer(BiFunction<Class, String, CallbackMeta> function) {
            super(function);
        }

        @Override
        public void addCallback(final RequestMessage<Invocation> request, final ChannelTransport transport) {
            Invocation invocation = request.getPayLoad();
            CallbackMeta meta = function.apply(invocation.getClazz(), invocation.getMethodName());
            if (meta == null) {
                return;
            }
            int port = transport.getLocalAddress().getPort();
            Object[] args = invocation.getArgs();
            Object callbackArg = args[meta.index];
            if (callbackArg == null || !(callbackArg instanceof Callback)) {
                throw new RpcException(String.format("Callback parameter be null!,%s", invocation.getMethod()));
            }
            Callback callback = (Callback) callbackArg;
            String ip = Ipv4.getLocalIp();
            int pid = GlobalContext.getPid();
            String callbackId = new StringBuilder(100).append(ip).append("_")
                    .append(port).append("_")
                    .append(pid).append("_")
                    .append(callbackArg.getClass().getCanonicalName()).append("_")
                    .append(counter.incrementAndGet()).toString();
            //注入回调ID，便于外部感知，再需要的时候进行删除
            callback.setCallbackId(callbackId);
            //header设置callbackId
            request.getHeader().addAttribute(HeadKey.callbackInsId, callbackId);
            //callback参数置空
            args[meta.index] = null;
            add(callbackId, transport, (c, t) -> new ConsumerCallbackInvoker(callback, t));
        }
    }

    /**
     * 服务提供者回调容器
     */
    protected static class ProducerCallbackContainer extends AbstractCallbackContainer<ProducerCallbackInvoker> {

        public ProducerCallbackContainer(BiFunction<Class, String, CallbackMeta> function) {
            super(function);
        }

        @Override
        public void addCallback(final RequestMessage<Invocation> request, final ChannelTransport transport) {
            Invocation invocation = request.getPayLoad();
            MessageHeader header = request.getHeader();
            CallbackMeta meta = function.apply(invocation.getClazz(), invocation.getMethodName());
            if (meta == null) {
                return;
            }
            String callbackId = header.getAttribute(HeadKey.callbackInsId).toString();
            if (callbackId == null || callbackId.isEmpty()) {
                throw new RuntimeException(" Server side handle RequestMessage callbackId can not be null! ");
            }
            Class<? extends Callback> callbackClass = invocation.getArgClasses()[meta.index];
            ProducerCallbackInvoker handler = add(callbackId, transport,
                    (c, t) -> new ProducerCallbackInvoker(c, invocation.getClazz(),
                            meta.getParameterType(), callbackClass, header, t, k -> callbacks.remove(k)));

            invocation.getArgs()[meta.index] = handler.callback;
        }
    }

    /**
     * 消费者注册用于处理服务端回调消息的处理器
     */
    protected static class ConsumerCallbackInvoker implements CallbackInvoker {
        /**
         * 回调
         */
        protected Callback callback;
        //通道
        protected ChannelTransport transport;

        public ConsumerCallbackInvoker(Callback callback, ChannelTransport transport) {
            this.callback = callback;
            this.transport = transport;
        }

        @Override
        public CompletableFuture<Result> invoke(RequestMessage<Invocation> request) {
            try {
                return CompletableFuture.completedFuture(new Result(request.getContext(), callback.notify(request.getPayLoad().getArgs()[0])));
            } catch (Exception e) {
                return CompletableFuture.completedFuture(new Result(request.getContext(), e));
            }
        }

        @Override
        public ChannelTransport getTransport() {
            return transport;
        }

        @Override
        public Callback getCallback() {
            return callback;
        }
    }

    /**
     * 回调
     */
    protected static class ProducerCallbackInvoker<Q, S> implements CallbackInvoker {
        /**
         * ID
         */
        protected String id;
        /**
         * 接口楼
         */
        protected Class interfaceClass;
        /**
         * 参数类型
         */
        protected Class<S> parameterType;
        /**
         * 请求头
         */
        protected MessageHeader header;
        /**
         * 连接
         */
        protected ChannelTransport transport;
        /**
         * 执行完毕消费者
         */
        protected Consumer<String> closing;
        /**
         * 回调
         */
        protected Callback<Q, S> callback;


        public ProducerCallbackInvoker(final String id,
                                       final Class interfaceClass,
                                       final Class<S> parameterType,
                                       final Class<? extends Callback> callbackClass,
                                       final MessageHeader header,
                                       final ChannelTransport transport,
                                       final Consumer<String> closing) {
            this.id = id;
            this.interfaceClass = interfaceClass;
            this.parameterType = parameterType;
            this.header = header;
            this.transport = transport;
            this.closing = closing;
            this.callback = PROXY.get().getProxy(callbackClass, this::doInvoke);
        }

        @Override
        public CompletableFuture<Result> invoke(final RequestMessage<Invocation> request) {
            Object response = request.getPayLoad().getArgs()[0];
            try {
                return CompletableFuture.completedFuture(new Result(request.getContext(), callback.notify((Q) response)));
            } catch (Exception e) {
                return CompletableFuture.completedFuture(new Result(request.getContext(), e));
            }
        }

        @Override
        public Callback<Q, S> getCallback() {
            return callback;
        }

        @Override
        public ChannelTransport getTransport() {
            return transport;
        }

        /**
         * 调用
         *
         * @param proxy
         * @param method
         * @param param
         * @return
         * @throws Throwable
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
            }
            Session session = transport.session();
            //回调不需要别名,需要设置真实的参数类型
            RequestMessage<Invocation> request = RequestMessage.build(
                    new Invocation(interfaceClass, null, method, param, new Class[]{parameterType}));
            MessageHeader rh = request.getHeader();
            //TODO 应答的压缩格式，老版协议没有会话
            rh.setCompression(session == null ? Compression.NONE : session.getCompressionType());
            rh.setProtocolType(header.getProtocolType());
            rh.setSerialization(header.getSerialization());
            rh.setMsgType(MsgType.CallbackReq.getType());
            rh.addAttribute(HeadKey.callbackInsId, id);

            //为了兼容，目前只支持同步调用
            ResponseMessage<ResponsePayload> message = (ResponseMessage) transport.sync(request, 3000);
            ResponsePayload responsePayload = message.getPayLoad();
            if (responsePayload.isError()) {
                throw responsePayload.getException();
            }
            return responsePayload.getResponse();
        }

    }

}
