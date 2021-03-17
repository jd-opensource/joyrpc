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
import io.joyrpc.constants.Constants;
import io.joyrpc.context.RequestContext;
import io.joyrpc.invoker.injection.Transmit;
import io.joyrpc.invoker.injection.Transmits;
import io.joyrpc.extension.URL;
import io.joyrpc.invoker.option.ArgumentOption;
import io.joyrpc.invoker.option.MethodOption;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.util.SystemClock;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

import static io.joyrpc.GenericService.GENERIC;
import static io.joyrpc.Plugin.TRANSMIT;
import static io.joyrpc.util.ClassUtils.getInitialValue;
import static io.joyrpc.util.ClassUtils.isReturnFuture;

/**
 * 调用代理
 */
public class InvokerCaller implements InvocationHandler {
    /**
     * The Invoker.
     */
    protected Invoker invoker;
    /**
     * 接口名称
     */
    protected Class<?> interfaceClass;
    /**
     * 是否为异步
     */
    protected boolean async;
    /**
     * 是否是泛化调用
     */
    protected boolean generic;
    /**
     * 默认方法构造器
     */
    protected volatile Constructor<MethodHandles.Lookup> constructor;
    /**
     * 默认方法处理器
     */
    protected Map<String, Optional<MethodHandle>> handles = new ConcurrentHashMap<>();
    /**
     * 透传
     */
    protected Transmit transmit = new Transmits(TRANSMIT.reverse());

    /**
     * 构造函数
     *
     * @param invoker        调用器
     * @param interfaceClass 接口类
     * @param url            url
     */
    public InvokerCaller(final Invoker invoker, final Class<?> interfaceClass, final URL url) {
        this.invoker = invoker;
        this.interfaceClass = interfaceClass;
        this.async = url.getBoolean(Constants.ASYNC_OPTION);
        this.generic = GENERIC.test(interfaceClass);
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] param) throws Throwable {
        Class<?> declaringClass = method.getDeclaringClass();
        int modifiers = method.getModifiers();
        if (generic && ((modifiers & (Modifier.ABSTRACT | Modifier.PUBLIC | Modifier.STATIC)) ==
                Modifier.PUBLIC) && declaringClass.isInterface()) {
            //Java8允许在接口上定义静态方法和默认方法（仅用与GenericService接口类及其子接口类）
            return doDefault(proxy, method, param);
        } else if (Modifier.isStatic(modifiers)) {
            //Java8允许在接口上定义静态方法
            return method.invoke(proxy, param);
        } else if (declaringClass == Object.class) {
            //处理toString，equals，hashcode等方法
            return method.invoke(invoker, param);
        } else {
            boolean isReturnFuture = isReturnFuture(interfaceClass, method);
            //请求上下文
            RequestContext context = RequestContext.getContext();
            //调用之前链路是否为异步
            boolean isAsyncBefore = context.isAsync();
            //上下文的异步必须设置成completeFuture
            context.setAsync(isReturnFuture);
            try {
                RequestMessage<Invocation> request = createRequest(method, param, context);
                //调用
                return isReturnFuture ? doAsync(request) : (async ? doContextAsync(request) : doSync(request));
            } finally {
                //重置异步标识，防止影响同一context下的provider业务逻辑以及其他consumer
                context.setAsync(isAsyncBefore);
            }
        }
    }

    /**
     * 构建请求
     *
     * @param method  方法
     * @param param   参数
     * @param context 请求上下文
     * @return 请求消息
     */
    protected RequestMessage<Invocation> createRequest(final Method method, final Object[] param, final RequestContext context) {
        //构造请求消息，参数类型放在Refer里面设置，使用缓存避免每次计算加快性能
        Invocation invocation = new Invocation(interfaceClass, null, method, param, generic);
        RequestMessage<Invocation> request = RequestMessage.build(invocation);
        //分组Failover调用，需要在这里设置创建时间和超时时间，不能再Refer里面。否则会重置。
        request.setCreateTime(SystemClock.now());
        //超时时间为0，Refer会自动修正，便于分组重试
        request.getHeader().setTimeout(0);
        //当前线程
        request.setThread(Thread.currentThread());
        //当前线程上下文
        request.setContext(context);
        //消费端
        request.setConsumer(true);
        //实际的方法名称
        if (generic) {
            request.setMethodName(param[0] == null ? null : param[0].toString());
            if (request.getMethodName() == null || request.getMethodName().isEmpty()) {
                //泛化调用没有传递方法名称
                throw new IllegalArgumentException(String.format("the method argument of GenericService.%s can not be empty.", method.getName()));
            }
        } else {
            request.setMethodName(method.getName());
        }
        //初始化请求，绑定方法选项
        invoker.setup(request);
        return request;
    }

    /**
     * 调用默认方法
     *
     * @param proxy  代理
     * @param method 方法
     * @param param  参数
     * @return 结果
     * @throws Throwable 异常
     */
    protected Object doDefault(final Object proxy, final Method method, final Object[] param) throws Throwable {
        if (constructor == null) {
            synchronized (this) {
                if (constructor == null) {
                    constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
                    constructor.setAccessible(true);
                }
            }
        }
        if (constructor != null) {
            Optional<MethodHandle> optional = handles.computeIfAbsent(method.getName(), o -> {
                Class<?> declaringClass = method.getDeclaringClass();
                try {
                    return Optional.of(constructor.
                            newInstance(declaringClass, MethodHandles.Lookup.PRIVATE).
                            unreflectSpecial(method, declaringClass).
                            bindTo(proxy));
                } catch (Throwable e) {
                    return Optional.empty();
                }
            });
            if (optional.isPresent()) {
                return optional.get().invokeWithArguments(param);
            }
        }
        throw new UnsupportedOperationException();
    }

    /**
     * 同步调用
     *
     * @param request 请求
     * @return 调用结果
     * @throws Throwable
     */
    protected Object doSync(final RequestMessage<Invocation> request) throws Throwable {
        try {
            CompletableFuture<Result> future = invoker.invoke(request);
            //正常同步返回，处理Java8的future.get内部先自循环造成的性能问题
            Result result = future.get(Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
            if (result.isException()) {
                throw result.getException();
            }
            return result.getValue();
        } catch (CompletionException | ExecutionException e) {
            throw e.getCause() != null ? e.getCause() : e;
        } finally {
            //调用结束，使用新的请求上下文，保留会话、调用者和跟踪的上下文
            transmit.onReturn(request);
        }
    }

    /**
     * 异步调用
     *
     * @param request 请求
     * @return 调用结果
     * @throws Throwable
     */
    protected CompletableFuture<Object> doAsync(final RequestMessage<Invocation> request) throws Throwable {
        //异步调用，业务逻辑执行完毕，不清理IO线程的上下文
        CompletableFuture<Object> response = new CompletableFuture<>();
        try {
            CompletableFuture<Result> future = invoker.invoke(request);
            future.whenComplete((res, err) -> {
                //目前是让用户自己保留上下文
                Throwable throwable = err == null ? res.getException() : err;
                if (throwable != null) {
                    transmit.onComplete(request, new Result(request.getContext(), throwable));
                    response.completeExceptionally(throwable);
                } else {
                    transmit.onComplete(request, res);
                    response.complete(res.getValue());
                }
            });
        } catch (CompletionException e) {
            //调用出错，线程没有切换，保留原有上下文
            transmit.onComplete(request, new Result(request.getContext(), e));
            response.completeExceptionally(e.getCause() != null ? e.getCause() : e);
        } catch (Throwable e) {
            //调用出错，线程没有切换，保留原有上下文
            transmit.onComplete(request, new Result(request.getContext(), e));
            response.completeExceptionally(e);
        } finally {
            //调用结束，使用新的请求上下文，保留会话、调用者和跟踪的上下文
            transmit.onReturn(request);
        }
        return response;
    }

    /**
     * 上下文异步调用
     *
     * @param request 请求
     * @return 调用结果
     * @throws Throwable
     */
    protected Object doContextAsync(final RequestMessage<Invocation> request) throws Throwable {
        RequestContext context = request.getContext();
        //设置CompletableFuture到上下文
        context.setFuture(doAsync(request));
        //返回默认值
        MethodOption option = request.getOption();
        if (option != null) {
            ArgumentOption argumentOption = option.getArgumentOption();
            if (argumentOption != null) {
                return option.getArgumentOption();
            }
        }
        return getInitialValue(request.getPayLoad().getMethod().getReturnType());
    }
}
