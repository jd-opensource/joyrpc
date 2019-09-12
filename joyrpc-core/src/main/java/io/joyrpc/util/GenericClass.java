package io.joyrpc.util;

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

import io.joyrpc.util.GenericType.Variable;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 类型的泛型信息
 */
public class GenericClass {

    protected Class clazz;

    //父类的泛型信息
    protected Map<Class, GenericType> classGeneric = new HashMap<>();
    //字段的泛型信息
    protected Map<Field, GenericType> fieldGeneric = new ConcurrentHashMap<>();
    //参数泛型信息
    protected Map<Executable, GenericExecutable> methodGeneric = new ConcurrentHashMap<>();

    public GenericClass(Class clazz) {
        this.clazz = clazz;

        GenericType childType = new GenericType(clazz);
        //当前类型声明的泛型
        String name;
        TypeVariable<Class>[] variables = clazz.getTypeParameters();
        if (variables.length > 0) {
            for (TypeVariable<Class> variable : variables) {
                childType.addVariable(new Variable(variable.toString()));
            }
            classGeneric.put(clazz, childType);
        }
        //判断是否是接口
        if (!clazz.isInterface()) {
            //不是接口，遍历父类
            Class parentClazz;
            SuperIterator iterator = new SuperIterator(clazz);
            while (iterator.hasNext()) {
                clazz = iterator.next();
                parentClazz = clazz.getSuperclass();
                if (parentClazz != Object.class) {
                    childType = parentType(parentClazz, clazz.getGenericSuperclass(), childType);
                }
            }
        } else {
            //接口
            Set<Class> uniques = new HashSet<>(10);
            Class[] ifaces = clazz.getInterfaces();
            Type[] gfaces = clazz.getGenericInterfaces();
            if (ifaces != null) {
                for (int i = 0; i < ifaces.length; i++) {
                    interfaceType(ifaces[i], gfaces[i], childType, uniques);
                }
            }
        }
    }

    /**
     * 构建父类的泛型对象
     *
     * @param iface     父类
     * @param gface     子类拿到的父类泛型
     * @param childType 子类的泛型类型
     * @param uniques   接口唯一处理
     * @return
     */
    protected void interfaceType(final Class iface, final Type gface, final GenericType childType, final Set<Class> uniques) {
        //父类的泛型对象
        GenericType parentType = parentType(iface, gface, childType);
        Class[] ifaces = iface.getInterfaces();
        Type[] gfaces = iface.getGenericInterfaces();
        if (ifaces != null) {
            for (int i = 0; i < ifaces.length; i++) {
                if (uniques.add(ifaces[i])) {
                    interfaceType(ifaces[i], gfaces[i], parentType, uniques);
                }
            }
        }
    }

    /**
     * 构建父类的泛型类型
     *
     * @param parent     父类
     * @param parentType 子类拿到的父类泛型
     * @param childType  子类的泛型类型
     * @return
     */
    protected GenericType parentType(final Class parent, final Type parentType, final GenericType childType) {
        GenericType result = new GenericType(parent);
        //父类的泛型
        TypeVariable<Class>[] variables = parent.getTypeParameters();
        //通过子类获取父类泛型的具体类型
        if (parentType instanceof ParameterizedType) {
            //得到泛型里的class类型对象
            Type[] arguments = ((ParameterizedType) parentType).getActualTypeArguments();
            Type argument;
            String name;
            GenericType argumentType;//存储父类的泛型信息
            for (int i = 0; i < arguments.length; i++) {
                argument = arguments[i];
                name = variables[i].getName();
                if (argument instanceof Class) {
                    //Class
                    result.addVariable(new Variable(name, argument));
                } else if (argument instanceof TypeVariable) {
                    //从子类获取泛型定义
                    result.addVariable(childType.getOrCreate(name));
                } else {
                    //可以是ParameterizedType和GenericArrayType，判断其内部是否还有泛型变量
                    argumentType = compute(argument, childType);
                    result.addVariable(new Variable(name, argument, argumentType.variable == null ? null : argumentType));
                }
            }
        }
        //父类的泛型信息
        classGeneric.put(parent, result);
        return result;
    }

    public Class getClazz() {
        return clazz;
    }

    /**
     * 获取字段泛型
     *
     * @param field
     * @return
     */
    public GenericType get(final Field field) {
        return field == null ? null : fieldGeneric.computeIfAbsent(field, k -> compute(k.getGenericType(), classGeneric.get(k.getDeclaringClass())));
    }

    /**
     * 获取构造函数泛型
     *
     * @param method
     * @return
     */
    public GenericConstructor get(final Constructor method) {
        return method == null ? null : (GenericConstructor) methodGeneric.computeIfAbsent(method, key -> compute(method));
    }

    /**
     * 获取方法泛型
     *
     * @param method
     * @return
     */
    public GenericMethod get(final Method method) {
        return method == null ? null : (GenericMethod) methodGeneric.computeIfAbsent(method, key -> compute(method));
    }

    /**
     * 计算
     *
     * @param executable
     * @param declaringType
     * @param consumer
     */
    protected GenericType[] computeParameters(final Executable executable, final GenericType declaringType,
                                              final Consumer<Map<String, Integer>> consumer) {
        Parameter[] parameters = executable.getParameters();
        GenericType[] parameterTypes = new GenericType[parameters.length];

        //计算参数泛型信息
        Map<String, Integer> variableTypes = new HashMap<>(2);
        Parameter parameter;
        GenericType parameterType;
        for (int i = 0; i < parameters.length; i++) {
            parameter = parameters[i];
            parameterType = compute(parameter.getParameterizedType(), declaringType);
            parameterTypes[i] = parameterType;
            //判断该参数是否是泛型的类型
            if (parameter.getType() == Class.class && parameterType.variable != null) {
                variableTypes.put(parameterType.variable.name, i);
            }
        }
        if (!variableTypes.isEmpty()) {
            if (consumer != null) {
                consumer.accept(variableTypes);
            }
            for (GenericType type : parameterTypes) {
                type.compute(variableTypes);
            }
        }

        return parameterTypes;
    }

    /**
     * 计算
     *
     * @param executable
     * @param declaringType
     */
    protected GenericType[] computeExceptions(final Executable executable, final GenericType declaringType) {
        Type[] exceptionTypes = executable.getGenericExceptionTypes();
        GenericType[] parameterTypes = new GenericType[exceptionTypes.length];

        GenericType parameterType;
        for (int i = 0; i < exceptionTypes.length; i++) {
            parameterType = compute(exceptionTypes[i], declaringType);
            parameterTypes[i] = parameterType;
        }

        return parameterTypes;
    }

    /**
     * 计算方法的泛型信息
     *
     * @param method
     * @return
     */
    protected GenericMethod compute(final Method method) {
        GenericType declaringType = classGeneric.get(method.getDeclaringClass());
        GenericType returnType = compute(method.getGenericReturnType(), declaringType);
        GenericType[] exceptionsTypes = computeExceptions(method, declaringType);
        GenericType[] parameterTypes = computeParameters(method, declaringType, o -> {
            returnType.compute(o);
            for (GenericType exceptionsType : exceptionsTypes) {
                exceptionsType.compute(o);
            }
        });

        return new GenericMethod(method, parameterTypes, exceptionsTypes, returnType);
    }

    /**
     * 获取泛型
     *
     * @param constructor
     * @return
     */
    protected GenericConstructor compute(final Constructor constructor) {
        GenericType declaringType = classGeneric.get(constructor.getDeclaringClass());
        GenericType[] parameterTypes = computeParameters(constructor, declaringType, null);
        GenericType[] exceptionsTypes = computeExceptions(constructor, declaringType);
        return new GenericConstructor(constructor, parameterTypes, exceptionsTypes);
    }

    /**
     * 计算泛型
     *
     * @param type
     * @param declaringType
     * @return
     */
    protected GenericType compute(final Type type, final GenericType declaringType) {
        return compute(new GenericType(type), type, declaringType);
    }

    /**
     * 计算泛型
     *
     * @param genericType
     * @param type
     * @param declaringType
     * @return
     */
    protected GenericType compute(final GenericType genericType, final Type type, final GenericType declaringType) {
        String name;
        if (type instanceof Class) {
            //没有泛型信息
        } else if (type instanceof TypeVariable) {
            //变量
            name = type.toString();
            GenericDeclaration gd = ((TypeVariable) type).getGenericDeclaration();
            if (gd instanceof Class) {
                genericType.addVariable(declaringType == null ? null : declaringType.getOrCreate(name));
            } else if (gd instanceof Method) {
                genericType.addVariable(new Variable(name));
            }
        } else if (type instanceof ParameterizedType) {
            //得到泛型里的class类型对象
            Type[] arguments = ((ParameterizedType) type).getActualTypeArguments();
            for (Type argument : arguments) {
                compute(genericType, argument, declaringType);
            }
        } else if (type instanceof GenericArrayType) {
            //泛型数组
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            compute(genericType, componentType, declaringType);
        }
        return genericType;
    }
}
