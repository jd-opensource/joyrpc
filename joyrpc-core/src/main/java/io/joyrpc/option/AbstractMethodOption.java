package io.joyrpc.option;

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

import io.joyrpc.invoker.CallbackMethod;
import io.joyrpc.permission.SerializerWhiteList;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.transaction.TransactionOption;
import io.joyrpc.util.GenericMethod;
import io.joyrpc.util.IDLMethod;

import javax.validation.Validator;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import static io.joyrpc.permission.SerializerWhiteList.getGlobalWhitelist;
import static io.joyrpc.util.ClassUtils.getDesc;

/**
 * 抽象方法选项
 */
public abstract class AbstractMethodOption implements MethodOption {
    /**
     * 方法
     */
    protected Method method;
    /**
     * 泛型方法
     */
    protected GenericMethod genericMethod;
    /**
     * 参数类型
     */
    protected ArgumentOption argType;
    /**
     * 方法级别隐式传参，合并了接口的隐藏参数，只读
     */
    protected Map<String, ?> implicits;
    /**
     * 超时时间
     */
    protected int timeout;
    /**
     * 并发数配置
     */
    protected Concurrency concurrency;
    /**
     * 缓存策略
     */
    protected CacheOption cachePolicy;
    /**
     * 方法参数验证器
     */
    protected Validator validator;
    /**
     * 事务选项
     */
    protected TransactionOption transactionOption;
    /**
     * 令牌
     */
    protected String token;
    /**
     * 是否异步调用
     */
    protected boolean async;
    /**
     * 是否开启跟踪
     */
    protected transient boolean trace;
    /**
     * 跟踪的span名称
     */
    protected String traceSpanId;
    /**
     * 回调方法
     */
    protected CallbackMethod callback;
    /**
     * 参数描述
     */
    protected String description;
    /**
     * 序列化白名单
     */
    protected SerializerWhiteList whiteList;

    /**
     * 构造函数
     *
     * @param idlMethod     GRPC方法
     * @param genericMethod 泛型方法
     * @param implicits     隐式传参
     * @param timeout       超时时间
     * @param concurrency   并发数配置
     * @param cachePolicy   缓存策略
     * @param validator     方法参数验证器
     * @param token         令牌
     * @param async         判断方法是否是异步调用
     * @param callback      回调方法
     */
    public AbstractMethodOption(final IDLMethod idlMethod,
                                final GenericMethod genericMethod,
                                final Map<String, ?> implicits, int timeout,
                                final Concurrency concurrency,
                                final CacheOption cachePolicy,
                                final Validator validator,
                                final TransactionOption transactionOption,
                                final String token,
                                final boolean async,
                                final boolean trace,
                                final CallbackMethod callback) {
        //只有泛化调用的时候没有设置grpMethod
        this.method = idlMethod == null ? null : idlMethod.getMethod();
        this.genericMethod = genericMethod;
        //采用canonicalName是为了和泛化调用保持一致，可读性和可写行更好
        this.argType = method == null ? null : new ArgumentOption(idlMethod);
        this.description = getDesc(argType == null ? null : argType.getClasses());
        this.implicits = implicits == null ? null : Collections.unmodifiableMap(implicits);
        this.timeout = timeout;
        this.concurrency = concurrency;
        this.cachePolicy = cachePolicy;
        this.validator = validator;
        this.transactionOption = transactionOption;
        this.token = token;
        this.async = async;
        this.trace = trace;
        this.traceSpanId = method == null ? null : getTraceSpanId(idlMethod.getClazz().getName(), method.getName());
        this.callback = callback;
        this.whiteList = getGlobalWhitelist();
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public GenericMethod getGenericMethod() {
        return genericMethod;
    }

    @Override
    public ArgumentOption getArgType() {
        return argType;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Map<String, ?> getImplicits() {
        return implicits;
    }

    @Override
    public int getTimeout() {
        return timeout;
    }

    @Override
    public Concurrency getConcurrency() {
        return concurrency;
    }

    @Override
    public CacheOption getCachePolicy() {
        return cachePolicy;
    }

    @Override
    public Validator getValidator() {
        return validator;
    }

    @Override
    public TransactionOption getTransactionOption() {
        return transactionOption;
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public boolean isAsync() {
        return async;
    }

    @Override
    public boolean isTrace() {
        return trace;
    }

    public void setTrace(boolean trace) {
        this.trace = trace;
    }

    /**
     * 获取跟踪ID
     *
     * @param className  类
     * @param methodName 方法
     * @return 跟踪ID
     */
    protected String getTraceSpanId(final String className, final String methodName) {
        return className + "/" + methodName;
    }

    @Override
    public String getTraceSpanId(final Invocation invocation) {
        if (traceSpanId != null) {
            return traceSpanId;
        }
        //泛化调用
        return getTraceSpanId(invocation.getClassName(), invocation.getArgs()[0].toString());
    }

    @Override
    public CallbackMethod getCallback() {
        return callback;
    }

}
