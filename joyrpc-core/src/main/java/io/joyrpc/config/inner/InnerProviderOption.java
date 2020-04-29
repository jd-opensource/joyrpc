package io.joyrpc.config.inner;

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

import io.joyrpc.config.AbstractInterfaceOption;
import io.joyrpc.context.IntfConfiguration;
import io.joyrpc.context.auth.IPPermission;
import io.joyrpc.context.limiter.LimiterConfiguration.ClassLimiter;
import io.joyrpc.extension.URL;
import io.joyrpc.extension.WrapperParametric;
import io.joyrpc.invoker.CallbackMethod;
import io.joyrpc.permission.BlackWhiteList;
import io.joyrpc.permission.StringBlackWhiteList;
import io.joyrpc.proxy.JCompiler;
import io.joyrpc.proxy.MethodCaller;
import io.joyrpc.util.ClassUtils;
import io.joyrpc.util.GrpcMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Validator;
import java.lang.reflect.*;
import java.util.Map;
import java.util.function.Supplier;

import static io.joyrpc.Plugin.COMPILER;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.context.auth.IPPermissionConfiguration.IP_PERMISSION;
import static io.joyrpc.context.limiter.LimiterConfiguration.LIMITERS;
import static io.joyrpc.util.ClassUtils.inbox;
import static io.joyrpc.util.ClassUtils.isReturnFuture;

/**
 * 服务提供者接口选项
 */
public class InnerProviderOption extends AbstractInterfaceOption {

    private static final Logger logger = LoggerFactory.getLogger(InnerProviderOption.class);

    /**
     * 方法黑白名单
     */
    protected BlackWhiteList<String> methodBlackWhiteList;
    /**
     * 接口IP限制
     */
    protected IntfConfiguration<String, IPPermission> ipPermissions;
    /**
     * 限流配置
     */
    protected IntfConfiguration<String, ClassLimiter> limiters;
    /**
     * 引用
     */
    protected Object ref;
    /**
     * 预编译
     */
    protected boolean precompilation;
    /**
     * 编译器
     */
    protected JCompiler compiler;

    /**
     * 构造函数
     *
     * @param interfaceClass 接口类
     * @param interfaceName  接口名称
     * @param url            URL
     * @param ref            引用对象
     */
    public InnerProviderOption(final Class<?> interfaceClass, final String interfaceName, final URL url, final Object ref) {
        super(interfaceClass, interfaceName, url);
        this.ref = ref;
        this.generic = false;
        this.compiler = COMPILER.get();
        setup();
        buildOptions();
    }

    @Override
    protected void setup() {
        super.setup();
        String include = url.getString(METHOD_INCLUDE_OPTION.getName());
        String exclude = url.getString(METHOD_EXCLUDE_OPTION.getName());
        this.methodBlackWhiteList = (include == null || include.isEmpty()) && (exclude == null || exclude.isEmpty()) ? null :
                new StringBlackWhiteList(include, exclude);
        this.precompilation = url.getBoolean(METHOD_PRECOMPILATION);
        this.ipPermissions = new IntfConfiguration<>(IP_PERMISSION, interfaceName);
        this.limiters = new IntfConfiguration<>(LIMITERS, interfaceName);
    }

    @Override
    protected void doClose() {
        super.doClose();
        ipPermissions.close();
        limiters.close();
    }

    @Override
    protected InnerMethodOption create(final WrapperParametric parametric) {
        GrpcMethod grpcMethod = getMethod(parametric.getName());
        Method method = grpcMethod == null ? null : grpcMethod.getMethod();
        return new InnerProviderMethodOption(
                grpcMethod,
                getImplicits(parametric.getName()),
                parametric.getPositive(TIMEOUT_OPTION.getName(), timeout),
                new Concurrency(parametric.getInteger(CONCURRENCY_OPTION.getName(), concurrency)),
                getCachePolicy(parametric),
                getValidator(parametric),
                parametric.getString(HIDDEN_KEY_TOKEN, token),
                method != null && isReturnFuture(interfaceClass, method),
                getCallback(method),
                methodBlackWhiteList,
                ipPermissions,
                limiters,
                precompilation ? compile(method) : null);
    }

    /**
     * 动态编译方法
     *
     * @param method 方法
     * @return 动态编译
     */
    protected MethodCaller compile(final Method method) {
        if (method == null || compiler == null) {
            return null;
        }
        String name = method.getName();
        name = Character.toUpperCase(name.charAt(0)) + name.substring(1) + "Caller";
        String simpleName = interfaceClass.getSimpleName() + "$" + name;
        String fullName = interfaceClass.getName() + "$" + name;
        boolean isVoid = method.getReturnType() == void.class;
        StringBuilder builder = new StringBuilder(300).
                append("package ").append(interfaceClass.getPackage().getName()).append(";\n").
                append("public class ").append(simpleName).append(" implements ").append(MethodCaller.class.getCanonicalName()).append("{\n").
                append("\t").append("protected ").append(interfaceClass.getCanonicalName()).append(" ref").append(";\n").
                append("\t").append("public ").append(simpleName).append("(").append(interfaceClass.getCanonicalName()).append(" ref").append(')').append("{\n").
                append("\t\t").append("this.ref=ref;").append("\n").
                append("\t}\n").
                append("\t").append("public Object invoke(Object[] args) throws java.lang.reflect.InvocationTargetException").append("{\n").
                append("\t\ttry{\n").
                append("\t\t\t").append(!isVoid ? "return " : "").
                append(Modifier.isStatic(method.getModifiers()) ? interfaceClass.getCanonicalName() : "ref").append('.').
                append(method.getName()).append("(");
        //参数
        int index = 0;
        Class<?> type;
        for (Parameter parameter : method.getParameters()) {
            //强制类型转换
            type = parameter.getType();
            builder.append(index > 0 ? "," : "").append("(").append(inbox(type).getCanonicalName()).append(")").append("args[").append(index++).append("]");
        }
        builder.append(");").append("\n").
                append(isVoid ? "\t\t\treturn null;\n" : "").
                append("\t\t}catch(Throwable e){\n").
                append("\t\t\tthrow new java.lang.reflect.InvocationTargetException(e);\n").
                append("\t\t}\n").
                append("\t}\n").append("}");
        try {
            Class<?> clazz = ClassUtils.forName(fullName, (n) -> {
                try {
                    return compiler.compile(n, builder);
                } catch (Throwable e) {
                    logger.error(e.getMessage() + " java:\n" + builder.toString());
                    return null;
                }
            });
            if (clazz == null) {
                return null;
            }
            Constructor[] constructors = clazz.getConstructors();
            return (MethodCaller) constructors[0].newInstance(ref);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            return null;
        }
    }

    /**
     * 方法选项
     */
    protected static class InnerProviderMethodOption extends InnerMethodOption implements ProviderMethodOption {
        /**
         * 方法的黑白名单
         */
        protected BlackWhiteList<String> methodBlackWhiteList;
        /**
         * IP限制
         */
        protected Supplier<IPPermission> iPPermission;
        /**
         * 限流
         */
        protected Supplier<ClassLimiter> limiter;
        /**
         * 动态生成的方法调用
         */
        protected MethodCaller caller;

        public InnerProviderMethodOption(final GrpcMethod method,
                                         final Map<String, ?> implicits, final int timeout,
                                         final Concurrency concurrency, final CachePolicy cachePolicy, final Validator validator,
                                         final String token, final boolean async, final CallbackMethod callback,
                                         final BlackWhiteList<String> methodBlackWhiteList,
                                         final Supplier<IPPermission> iPPermission,
                                         final Supplier<ClassLimiter> limiter,
                                         final MethodCaller caller) {
            super(method, implicits, timeout, concurrency, cachePolicy, validator, token, async, callback);
            this.methodBlackWhiteList = methodBlackWhiteList;
            this.iPPermission = iPPermission;
            this.limiter = limiter;
            this.caller = caller;
        }

        @Override
        public BlackWhiteList<String> getMethodBlackWhiteList() {
            return methodBlackWhiteList;
        }

        @Override
        public IPPermission getIPPermission() {
            return iPPermission.get();
        }

        @Override
        public ClassLimiter getLimiter() {
            return limiter.get();
        }

        @Override
        public MethodCaller getCaller() {
            return caller;
        }
    }

}
