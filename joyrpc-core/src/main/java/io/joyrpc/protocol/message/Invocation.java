package io.joyrpc.protocol.message;

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

import io.joyrpc.constants.Constants;
import io.joyrpc.exception.LafException;
import io.joyrpc.exception.MethodOverloadException;
import io.joyrpc.extension.MapParametric;
import io.joyrpc.extension.Parametric;
import io.joyrpc.extension.URL;
import io.joyrpc.util.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.util.ClassUtils.*;
import static io.joyrpc.util.GenericType.validate;

/**
 * 调用对象
 */
public class Invocation implements Call {

    public static final String CLAZZ_NAME = "clazzName";

    public static final String CLASS_NAME = "className";

    public static final String METHOD_NAME = "methodName";

    public static final String ALIAS = "alias";

    public static final String ARGS_TYPE = "argsType";

    public static final String METHOD_SIGN = "methodSign";

    public static final String ARGS = "args";

    public static final String ATTACHMENTS = "attachments";

    /**
     * 类名
     */
    protected String className;
    /**
     * 分组别名
     */
    protected String alias;
    /**
     * 方法名称
     */
    protected String methodName;
    /**
     * 参数类型字符串
     */
    protected String[] argsType; //考虑优化class？
    /**
     * 参数类型
     */
    protected transient Class[] argClasses;
    /**
     * 参数对象
     */
    protected Object[] args;
    /**
     * 扩展属性，默认为空，很多情况不需要，加快性能
     */
    protected Map<String, Object> attachments;
    /**
     * 方法对象，泛化调用的时候该对象可能为null
     */
    protected transient Method method;
    /**
     * grpc类型信息
     */
    protected transient GrpcType grpcType;
    /**
     * 所在的类，泛化调用的时候该对象可能为null
     */
    protected transient Class clazz;
    /**
     * 调用的目标对象
     */
    protected transient Object object;
    /**
     * 是否泛化调用
     */
    protected transient Boolean generic;
    /**
     * 是否回调
     */
    protected transient boolean callback;
    /**
     * 泛化的方法
     */
    protected transient GenericMethod genericMethod;
    /**
     * 参数泛型信息
     */
    protected transient Type[] genericTypes;

    public Invocation() {
    }

    public Invocation(Map<String, Object> attachments) {
        this.attachments = attachments;
    }

    /**
     * 构造函数
     *
     * @param className  接口名称
     * @param alias      别名
     * @param methodName 方法名称
     */
    public Invocation(String className, String alias, String methodName) {
        this(className, alias, methodName, null);
    }

    /**
     * 构造函数
     *
     * @param className  接口名称
     * @param alias      别名
     * @param methodName 方法名称
     * @param argClasses 参数类型
     */
    public Invocation(String className, String alias, String methodName, Class[] argClasses) {
        this.className = className;
        this.alias = alias;
        this.methodName = methodName;
        if (argClasses != null) {
            //TODO 占用CPU
            setArgsType(argClasses);
        }
    }

    /**
     * 构造函数
     *
     * @param iface  接口
     * @param method 方法
     * @param args   参数
     */
    public Invocation(final Class iface, final Method method, final Object[] args) {
        this(iface, null, method, args, method.getParameterTypes(), null);
    }

    /**
     * 构造函数
     *
     * @param iface  接口
     * @param alias  别名
     * @param method 方法
     * @param args   参数
     */
    public Invocation(final Class iface, final String alias, final Method method, final Object[] args) {
        this(iface, alias, method, args, method.getParameterTypes(), null);
    }

    /**
     * 构造函数
     *
     * @param iface    接口
     * @param alias    别名
     * @param method   方法
     * @param args     参数
     * @param argTypes 类型
     */
    public Invocation(final Class iface, final String alias, final Method method, final Object[] args, final Class[] argTypes) {
        this(iface, alias, method, args, argTypes, null);
    }

    /**
     * 构造函数
     *
     * @param iface   接口
     * @param alias   别名
     * @param method  方法
     * @param args    参数
     * @param generic 泛化
     */
    public Invocation(final Class iface, final String alias, final Method method, final Object[] args, final Boolean generic) {
        this.clazz = iface;
        this.className = iface.getName();
        this.alias = alias;
        this.method = method;
        this.methodName = method.getName();
        this.args = args == null ? new Object[0] : args;
        this.generic = generic;
    }

    /**
     * 构造函数
     *
     * @param iface    接口
     * @param alias    别名
     * @param method   方法
     * @param args     参数
     * @param argTypes 类型
     * @param generic  泛化
     */
    public Invocation(final Class iface, final String alias, final Method method, final Object[] args, final Class[] argTypes, final Boolean generic) {
        this.clazz = iface;
        this.className = iface.getName();
        this.alias = alias;
        this.method = method;
        this.methodName = method.getName();
        this.args = args == null ? new Object[0] : args;
        this.generic = generic;
        setArgsType(argTypes);
    }

    /**
     * 为本地服务端创建调用对象
     *
     * @return 调用对象
     */
    public Invocation create() {
        Invocation result = new Invocation();
        result.className = className;
        result.alias = alias;
        result.methodName = methodName;
        result.args = args;
        result.argsType = argsType;
        result.attachments = attachments == null ? null : new HashMap<>(attachments);
        result.generic = generic;
        return result;
    }

    @Override
    public String[] getArgsType() {
        return argsType;
    }

    /**
     * 设置参数类型
     *
     * @param argsType 参数类名
     */
    public void setArgsType(String[] argsType) {
        this.argsType = argsType;
        // 清空缓存
        this.argClasses = null;
    }

    /**
     * 设置参数类型，只在回调函数和解析请求的时候主动设置
     *
     * @param argsType 参数类型
     */
    public void setArgsType(Class[] argsType) {
        if (argsType == null) {
            this.argsType = new String[0];
            this.argClasses = new Class[0];
        } else {
            this.argClasses = argsType;
            //采用canonicalName是为了和泛化调用保持一致，可读性和可写行更好
            this.argsType = getCanonicalNames(argsType);
        }
    }

    /**
     * 设置参数类型
     *
     * @param argClasses 参数类
     * @param argTypes   参数类名
     */
    public void setArgsType(Class[] argClasses, String[] argTypes) {
        this.argClasses = argClasses;
        this.argsType = argTypes;
    }

    @Override
    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    @Override
    public Class[] getArgClasses() {
        if (argClasses == null) {
            //TODO 要以argsType优先，因为方法上的参数类型可能为接口
            if (method != null) {
                argClasses = method.getParameterTypes();
            } else if (argsType != null) {
                try {
                    argClasses = ClassUtils.getClasses(argsType);
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage());
                }
            }
        }
        return argClasses;
    }

    @Override
    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public GrpcType getGrpcType() {
        return grpcType;
    }

    public void setGrpcType(GrpcType grpcType) {
        this.grpcType = grpcType;
    }

    @Override
    public Class getClazz() {
        return clazz;
    }

    public void setClazz(Class clazz) {
        this.clazz = clazz;
    }

    @Override
    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public GenericMethod getGenericMethod() {
        if (genericMethod == null) {
            GenericClass genericClass = getGenericClass(clazz);
            genericMethod = genericClass == null ? null : genericClass.get(method);
        }
        return genericMethod;
    }

    public void setGenericMethod(GenericMethod genericMethod) {
        this.genericMethod = genericMethod;
    }

    @Override
    public Type[] computeTypes() throws ClassNotFoundException, NoSuchMethodException, MethodOverloadException {
        if (genericTypes != null) {
            return genericTypes;
        }
        Class[] resolvedClasses;
        Type[] resolvedTypes;
        if (clazz == null) {
            clazz = forName(className);
        }
        if (method == null) {
            method = getPublicMethod(clazz, methodName);
        }
        Parameter[] parameters = method.getParameters();
        if (parameters.length > 0) {
            if (genericMethod == null) {
                genericMethod = getGenericClass(clazz).get(method);
            }
            //获取用户设置的参数类型
            String[] argTypes = argsType;
            if (isGeneric()) {
                //泛化调用，从参数里面获取参数类型
                if (args == null || args.length != 3) {
                    throw new NoSuchMethodException("The number of parameter is wrong.");
                }
                argTypes = (String[]) args[1];
            }
            if (argTypes == null || argTypes.length == 0) {
                resolvedTypes = genericMethod.getGenericTypes();
                resolvedClasses = genericMethod.getTypes();
            } else if (argTypes.length != parameters.length) {
                throw new NoSuchMethodException(String.format("There is no method %s with %d parameters", methodName, parameters.length));
            } else {
                GenericType[] genericTypes = genericMethod.getParameters();
                resolvedTypes = new Type[parameters.length];
                resolvedClasses = new Class<?>[parameters.length];
                Class<?> clazz;
                for (int i = 0; i < resolvedClasses.length; i++) {
                    resolvedTypes[i] = genericTypes[i].getGenericType();
                    resolvedClasses[i] = genericTypes[i].getType();
                    clazz = ClassUtils.getClass(argTypes[i]);
                    if (validate(resolvedTypes[i], clazz, true)) {
                        resolvedClasses[i] = clazz;
                        resolvedTypes[i] = clazz;
                        continue;
                    } else {
                        throw new NoSuchMethodException(String.format("There is no method %s with parameter type %s at %d", methodName, argTypes[i], i));
                    }
                }
            }
        } else {
            resolvedClasses = new Class[0];
            resolvedTypes = new Type[0];
        }
        argClasses = resolvedClasses;
        if (argsType == null) {
            argsType = getCanonicalNames(argClasses);
        }
        genericTypes = resolvedTypes;
        return resolvedTypes;
    }

    public void setGenericTypes(Type[] genericTypes) {
        this.genericTypes = genericTypes;
    }

    /**
     * 如果参数类型不存在，则进行计算
     *
     * @return
     */
    public String[] computeArgsType() {
        if (argsType == null) {
            //采用canonicalName是为了和泛化调用保持一致，可读性和可写行更好
            if (argClasses != null) {
                argsType = getCanonicalNames(argClasses);
            } else if (method != null) {
                argsType = getCanonicalNames(method.getParameterTypes());
            }
        }
        return argsType;
    }

    /**
     * 添加扩展信息
     *
     * @param key
     * @param value
     * @return
     */
    public Invocation addAttachment(final String key, final Object value) {
        if (key != null && value != null) {
            if (attachments == null) {
                attachments = new HashMap<>();
            }
            attachments.put(key, value);
        }
        return this;
    }

    /**
     * 添加扩展属性
     *
     * @param map 参数
     */
    public void addAttachments(final Map<String, ?> map) {
        if (map != null) {
            if (attachments == null) {
                attachments = new HashMap<>();
            }
            attachments.putAll(map);
        }
    }

    /**
     * 获取扩展属性
     *
     * @param key
     * @param function
     * @param <T>
     * @return
     */
    public <T> T computeIfAbsent(final String key, final Function<String, T> function) {
        if (key == null) {
            return null;
        }
        if (attachments != null) {
            attachments = new HashMap<>();
        }
        return (T) (function == null ? attachments.get(key) : attachments.computeIfAbsent(key, function));
    }

    /**
     * 移除扩展信息
     *
     * @param key 键
     * @return
     */
    public Object removeAttachment(final String key) {
        return key == null || attachments == null ? null : attachments.remove(key);
    }

    @Override
    public Map<String, Object> getAttachments() {
        return attachments;
    }

    /**
     * 获取扩展属性
     *
     * @param key 键
     * @return 扩展属性
     */
    public <T> T getAttachment(final String key) {
        return attachments == null ? null : (T) attachments.get(key);
    }

    /**
     * 获取扩展属性
     *
     * @param key          键
     * @param defaultValue 默认值
     * @return 扩展属性
     */
    public <T> T getAttachment(final String key, final T defaultValue) {
        if (attachments == null) {
            return defaultValue;
        } else {
            T result = (T) attachments.get(key);
            return result == null ? defaultValue : result;
        }
    }

    public Parametric asParametric() {
        return new MapParametric(attachments);
    }

    @Override
    public boolean isGeneric() {
        if (generic == null) {
            Object attachment = attachments == null ? null : attachments.get(GENERIC_KEY);
            generic = attachment == null ? Boolean.FALSE : Boolean.TRUE.equals(attachment);
        }
        return generic;
    }

    public boolean isCallback() {
        return callback;
    }

    public void setCallback(boolean callback) {
        this.callback = callback;
    }

    /**
     * 方法调用
     *
     * @param target
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public Object invoke(final Object target) throws InvocationTargetException, IllegalAccessException {
        return method.invoke(target, args);
    }

    /**
     * 构建调用对象
     *
     * @param url        url
     * @param parametric 参数
     * @param supplier   异常提供者
     * @return 调用对象
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws MethodOverloadException
     */
    public static Invocation build(final URL url, final Parametric parametric, final Supplier<LafException> supplier)
            throws ClassNotFoundException, NoSuchMethodException, MethodOverloadException {
        return build(url, parametric, null, supplier);
    }

    /**
     * 构建调用对象
     *
     * @param url        url
     * @param parametric 参数
     * @param function   GrpcType函数
     * @param supplier   异常提供者
     * @return 调用对象
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws MethodOverloadException
     */
    public static Invocation build(final URL url, final Parametric parametric,
                                   final BiFunction<Class<?>, Method, GrpcType> function,
                                   final Supplier<LafException> supplier)
            throws ClassNotFoundException, NoSuchMethodException, MethodOverloadException {
        String path = url.getPath();
        String[] parts = path == null ? new String[0] : StringUtils.split(path, '/');
        String className;
        String alias;
        String methodName;
        if (parts.length > 2) {
            className = parts[0];
            alias = parts[1];
            methodName = parts[2];
        } else if (parts.length == 2) {
            className = parts[0];
            methodName = parts[1];
            alias = parametric.getString(ALIAS_OPTION);
        } else {
            throw supplier.get();
        }
        Class ifaceClass = forName(className);
        //获取方法信息
        Method method;
        GrpcType grpcType = null;
        if (function == null) {
            method = getPublicMethod(ifaceClass, methodName);
        } else {
            //需要GrpcType信息
            GrpcMethod grpcMethod = getPublicMethod(ifaceClass, methodName, function);
            method = grpcMethod.getMethod();
            grpcType = grpcMethod.getType();
        }
        GenericClass genericClass = ClassUtils.getGenericClass(ifaceClass);
        GenericMethod genericMethod = genericClass.get(method);

        Invocation invocation = new Invocation(className, alias == null ? "" : alias, methodName, genericMethod.getTypes()).
                addAttachment(Constants.HIDDEN_KEY_TOKEN, parametric.getString(KEY_TOKEN)).
                addAttachment(HIDDEN_KEY_APPID, parametric.getString(KEY_APPID)).
                addAttachment(HIDDEN_KEY_APPNAME, parametric.getString(KEY_APPNAME)).
                addAttachment(HIDDEN_KEY_APPINSID, parametric.getString(KEY_APPINSID));
        invocation.setClazz(ifaceClass);
        invocation.setMethod(method);
        invocation.setGenericMethod(genericMethod);
        invocation.setGenericTypes(genericMethod.getGenericTypes());
        invocation.setGrpcType(grpcType);
        //隐式传参
        parametric.foreach((key, value) -> {
            if (!key.isEmpty() && key.charAt(0) == Constants.HIDE_KEY_PREFIX) {
                invocation.addAttachment(key, value);
            }
        });
        return invocation;
    }

    /**
     * 构建调用对象
     *
     * @param url      url
     * @param headers  http头
     * @param supplier 异常提供者
     * @return
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws MethodOverloadException
     */
    public static Invocation build(final URL url, final Map<CharSequence, Object> headers, final Supplier<LafException> supplier)
            throws ClassNotFoundException, NoSuchMethodException, MethodOverloadException {
        return build(url, new MapParametric(headers), null, supplier);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Invocation that = (Invocation) o;

        if (className != null ? !className.equals(that.className) : that.className != null) {
            return false;
        }
        if (alias != null ? !alias.equals(that.alias) : that.alias != null) {
            return false;
        }
        if (methodName != null ? !methodName.equals(that.methodName) : that.methodName != null) {
            return false;
        }
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(argsType, that.argsType)) {
            return false;
        }
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(args, that.args)) {
            return false;
        }
        return attachments != null ? attachments.equals(that.attachments) : that.attachments == null;

    }

    @Override
    public int hashCode() {
        int result = className != null ? className.hashCode() : 0;
        result = 31 * result + (alias != null ? alias.hashCode() : 0);
        result = 31 * result + (methodName != null ? methodName.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(argsType);
        result = 31 * result + Arrays.hashCode(args);
        result = 31 * result + (attachments != null ? attachments.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Invocation{");
        sb.append("className='").append(className).append('\'');
        sb.append(", alias='").append(alias).append('\'');
        sb.append(", methodName='").append(methodName).append('\'');
        sb.append(", argsType=").append(Arrays.toString(argsType));
        sb.append(", args=").append(Arrays.toString(args));
        sb.append(", attachments=").append(attachments);
        sb.append('}');
        return sb.toString();
    }
}
