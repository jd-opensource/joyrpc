package io.joyrpc.proxy.javassist;

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

import io.joyrpc.exception.ProxyException;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.condition.ConditionalOnClass;
import io.joyrpc.proxy.ProxyFactory;
import io.joyrpc.util.ClassUtils;
import javassist.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The type Javassist proxy factory.
 *
 * @date: 1 /22/2019
 */
@Extension("javassist")
@ConditionalOnClass("javassist.ClassPool")
public class JavassistProxyFactory implements ProxyFactory {

    protected static final AtomicInteger COUNTER = new AtomicInteger();

    /**
     * 原始类和代理类的映射
     */
    protected static final Map<Class<?>, Class<?>> PROXIES = new ConcurrentHashMap<>();

    /**
     * ClassLoader 缓存
     */
    protected static final Map<ClassLoader, LoaderClassPath> LOADERS = new ConcurrentHashMap<>();


    /**
     * 取得代理类(javassist方式)
     */

    @Override
    public <T> T getProxy(final Class<T> clz, final InvocationHandler invoker, final ClassLoader classLoader) throws ProxyException {
        try {
            Class<?> clazz = PROXIES.get(clz);
            if (clazz == null) {
                ClassPool mPool = ClassPool.getDefault();
                //添加类加载器，防止重复添加
                addClassPath(mPool, classLoader);
                //同步创建代理类
                synchronized (clz) {
                    clazz = PROXIES.get(clz);
                    if (clazz == null) {
                        String interfaceName = ClassUtils.getName(clz);
                        String className = interfaceName + "_proxy_" + COUNTER.getAndIncrement();
                        CodeGenerator generator = new CodeGenerator(className, interfaceName, clz.getMethods());
                        clazz = generator.build(mPool);

                        PROXIES.put(clz, clazz);
                    }
                }
            }

            Object instance = clazz.newInstance();
            clazz.getMethod("setInvocationHandler", InvocationHandler.class).invoke(instance, invoker);
            clazz.getField("methods").set(instance, clz.getMethods());

            return (T) instance;
        } catch (Exception e) {
            throw new ProxyException("Error occurred while creating javassist proxy of " + clz.getName(), e);
        }
    }

    protected void addClassPath(final ClassPool mPool, final ClassLoader classLoader) {
        LOADERS.computeIfAbsent(classLoader, o -> {
            LoaderClassPath path = new LoaderClassPath(classLoader);
            mPool.appendClassPath(path);
            return path;
        });
    }

    /**
     * The type Code generator.
     */
    protected static class CodeGenerator {
        //类名
        protected String className;
        //接口名称
        protected String interfaceName;
        //方法
        protected Method[] methods;

        public CodeGenerator(String className, String interfaceName, Method[] methods) {
            this.className = className;
            this.interfaceName = interfaceName;
            this.methods = methods;
        }

        /**
         * 构建类
         *
         * @param classPool 类池
         * @return 类
         * @throws CannotCompileException
         * @throws NotFoundException
         */
        public Class<?> build(final ClassPool classPool) throws CannotCompileException, NotFoundException {

            CtClass mCtc = classPool.makeClass(className);
            mCtc.addInterface(classPool.get(interfaceName));

            mCtc.addField(CtField.make(InvocationHandler.class.getCanonicalName() + " invocationHandler = null;", mCtc));
            mCtc.addField(CtField.make("public static java.lang.reflect.Method[] methods;", mCtc));

            StringBuilder builder = new StringBuilder(1000);
            for (int i = 0; i < methods.length; i++) {
                if (!Modifier.isStatic(methods[i].getModifiers())) {
                    source(mCtc, methods[i], i, builder);
                    mCtc.addMethod(CtMethod.make(builder.toString(), mCtc));
                    builder.setLength(0);
                }
            }
            mCtc.addMethod(CtMethod.make("public void setInvocationHandler(" + InvocationHandler.class.getName() + " h){ invocationHandler=$1; }", mCtc));
            return mCtc.toClass();
        }

        /**
         * 生成方法源代码
         *
         * @param mCtc
         * @param method
         * @param index
         * @param builder
         */
        protected void source(final CtClass mCtc, final Method method, final int index, final StringBuilder builder) {

            //返回值
            Class<?> returnType = method.getReturnType();
            //参数
            Class<?>[] parameterType = method.getParameterTypes();
            //修饰符
            modifier(method.getModifiers(), builder).append(' ');
            //返回值
            getName(returnType, builder).append(' ');
            //方法名称
            builder.append(method.getName());
            //参数
            builder.append('(');
            for (int i = 0; i < parameterType.length; i++) {
                if (i > 0) {
                    builder.append(',');
                }
                getName(parameterType[i], builder).append(" arg").append(i);
            }
            builder.append(')');
            //异常
            Class<?>[] et = method.getExceptionTypes();
            if (et.length > 0) {
                builder.append(" throws ");
                for (int i = 0; i < et.length; i++) {
                    if (i > 0) {
                        builder.append(',');
                    }
                    getName(et[i], builder);
                }
            }

            //方法体
            builder.append('{');

            builder.append("Object[] args = new Object[").append(parameterType.length).append("];");
            for (int j = 0; j < parameterType.length; j++) {
                builder.append(" args[").append(j).append("] = ($w)$").append(j + 1).append(";");
            }
            builder.append(" Object result = invocationHandler.invoke(this, methods[").append(index).append("], args);");
            if (!Void.TYPE.equals(returnType)) {
                builder.append(" return ");
                asArgument(returnType, "result", builder).append(';');
            }
            builder.append('}');
        }

        /**
         * 添加修饰符
         *
         * @param mod     修饰符变量
         * @param builder 字符串构建器
         * @return 字符串构建器
         */
        protected StringBuilder modifier(final int mod, final StringBuilder builder) {
            if (Modifier.isPublic(mod)) {
                builder.append("public");
            } else if (Modifier.isProtected(mod)) {
                builder.append("protected");
            } else if (Modifier.isPrivate(mod)) {
                builder.append("private");
            }
            if (Modifier.isStatic(mod)) {
                builder.append(" static");
            }
            if (Modifier.isVolatile(mod)) {
                builder.append(" volatile");
            }

            return builder;
        }

        /**
         * 添加参数
         *
         * @param cl
         * @param name
         * @param builder
         * @return
         */
        protected StringBuilder asArgument(final Class<?> cl, final String name, final StringBuilder builder) {
            if (cl.isPrimitive()) {
                if (Boolean.TYPE == cl) {
                    builder.append(name).append("==null?false:((Boolean)").append(name).append(").booleanValue()");
                } else if (Byte.TYPE == cl) {
                    builder.append(name).append("==null?(byte)0:((Byte)").append(name).append(").byteValue()");
                } else if (Character.TYPE == cl) {
                    builder.append(name).append("==null?(char)0:((Character)").append(name).append(").charValue()");
                } else if (Double.TYPE == cl) {
                    builder.append(name).append("==null?(double)0:((Double)").append(name).append(").doubleValue()");
                } else if (Float.TYPE == cl) {
                    builder.append(name).append("==null?(float)0:((Float)").append(name).append(").floatValue()");
                } else if (Integer.TYPE == cl) {
                    builder.append(name).append("==null?(int)0:((Integer)").append(name).append(").intValue()");
                } else if (Long.TYPE == cl) {
                    builder.append(name).append("==null?(long)0:((Long)").append(name).append(").longValue()");
                } else if (Short.TYPE == cl) {
                    builder.append(name).append("==null?(short)0:((Short)").append(name).append(").shortValue()");
                } else {
                    throw new RuntimeException(name + " is unknown primitive type.");
                }
            } else {
                getName(cl, builder.append('(')).append(')').append(name);
            }
            return builder;
        }

        /**
         * get name.
         * java.lang.Object[][].class => "java.lang.Object[][]"
         *
         * @param c       class.
         * @param builder
         * @return name.
         */
        protected StringBuilder getName(final Class<?> c, final StringBuilder builder) {
            if (c.isArray()) {
                int dimension = 0;
                Class<?> ct = c;
                while (ct.isArray()) {
                    ct = ct.getComponentType();
                    dimension++;
                }
                builder.append(ct.getName());
                for (int i = 0; i < dimension; i++) {
                    builder.append("[]");
                }
            } else {
                builder.append(c.getName());
            }
            return builder;
        }
    }
}
