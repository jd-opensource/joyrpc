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

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 类型的泛型信息
 */
public class GenericClass {
    /**
     * 类
     */
    protected Class clazz;
    /**
     * 父类的泛型信息
     */
    protected Map<Class, GenericType> classGeneric = new HashMap<>();
    /**
     * 字段的泛型信息
     */
    protected Map<Field, GenericType> fieldGeneric = new ConcurrentHashMap<>();
    /**
     * 参数泛型信息
     */
    protected Map<Executable, GenericExecutable> methodGeneric = new ConcurrentHashMap<>();

    public GenericClass(Class clazz) {
        this(clazz, null);
    }

    public GenericClass(Class clazz, Map<String, ? extends Type> parent) {
        this.clazz = clazz;

        GenericType childType = new GenericType(clazz);
        //当前类型声明的泛型
        TypeVariable<Class>[] variables = clazz.getTypeParameters();
        if (variables.length > 0) {
            for (TypeVariable<Class> variable : variables) {
                if (parent == null) {
                    childType.addVariable(new Variable(variable.getName(), variable.getBounds()[0]));
                } else {
                    childType.addVariable(new Variable(variable.getName(), parent.get(variable.getName())));
                }
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
                    //递归遍历父接口
                    interfaceType(ifaces[i], gfaces[i], childType, uniques);
                }
            }
        }
    }

    /**
     * 构建父类的泛型对象
     *
     * @param iface     当前接口类
     * @param gface     当前接口类的子类所拿到的父类泛型
     * @param childType 当前接口类的子类的泛型类型
     * @param uniques   接口唯一处理
     */
    protected void interfaceType(final Class iface, final Type gface, final GenericType childType, final Set<Class> uniques) {
        //父类的泛型对象
        GenericType parentType = parentType(iface, gface, childType);
        //递归当前接口的父类
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
                    result.addVariable(new Variable(name, childType.getVariable(((TypeVariable) argument).getName())));
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
     * @param field 字段
     * @return 泛型
     */
    public GenericType get(final Field field) {
        return field == null ? null : fieldGeneric.computeIfAbsent(field, key -> compute(field));
    }

    /**
     * 获取构造函数泛型
     *
     * @param constructor 构造函数
     * @return 泛型
     */
    public GenericConstructor get(final Constructor constructor) {
        return constructor == null ? null : (GenericConstructor) methodGeneric.computeIfAbsent(constructor, key -> compute(constructor));
    }

    /**
     * 获取方法泛型
     *
     * @param method 方法
     * @return 泛型
     */
    public GenericMethod get(final Method method) {
        return method == null ? null : (GenericMethod) methodGeneric.computeIfAbsent(method, key -> compute(method));
    }

    /**
     * 计算
     *
     * @param executable    执行器
     * @param declaringType 声明该执行器的类的泛型
     * @param consumer      参数消费者
     * @return 参数泛型数组
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
     * @param executable    执行器
     * @param declaringType 声明该执行器的类的泛型
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
     * @param method 方法
     * @return 泛型
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
     * @param field 字段
     * @return 泛型
     */
    protected GenericType compute(final Field field) {
        return compute(field.getGenericType(), classGeneric.get(field.getDeclaringClass()));
    }

    /**
     * 获取泛型
     *
     * @param constructor 构造函数
     * @return 泛型
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
     * @param type          待解析类型
     * @param declaringType 声明的泛型
     * @return 泛型
     */
    protected GenericType compute(final Type type, final GenericType declaringType) {
        GenericType genericType = new GenericType(type);
        genericType.setType(compute(genericType, type, declaringType));
        return genericType;
    }

    /**
     * 计算泛型，返回解析好的类型
     *
     * @param genericType   目标泛型
     * @param type          待解析类型
     * @param declaringType 子类声明的泛型
     * @return 解析好的类型
     */
    protected Type compute(final GenericType genericType, final Type type, final GenericType declaringType) {
        String name;
        if (type instanceof Class) {
            //没有泛型信息
        } else if (type instanceof TypeVariable) {
            //变量
            TypeVariable typeVariable = (TypeVariable) type;
            name = typeVariable.getName();
            //泛型声明的地方
            GenericDeclaration gd = typeVariable.getGenericDeclaration();
            if (gd instanceof Class) {
                //类变量
                if (declaringType != null) {
                    Variable variable = declaringType.getVariable(name);
                    genericType.addVariable(variable);
                    if (variable.getType() != type) {
                        //把解析好的变量，重新包装生成Type
                        return variable.getType();
                    }
                }
            } else if (gd instanceof Executable) {
                //执行器变量（方法&构造函数）
                Type[] oldBounds = typeVariable.getBounds();
                //并计算变量的限定类
                Type[] newBounds = compute(genericType, oldBounds, declaringType);
                typeVariable = oldBounds == newBounds ? typeVariable : new TypeVariableImpl<>(typeVariable, newBounds);
                genericType.addVariable(new Variable(name, typeVariable));
                if (typeVariable != type) {
                    return typeVariable;
                }
            }
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Type[] oldTypes = pType.getActualTypeArguments();
            Type[] newTypes = compute(genericType, oldTypes, declaringType);
            if (newTypes != oldTypes) {
                //把解析好的变量，重新包装生成Type
                return new ParameterizedTypeImpl(newTypes, pType.getOwnerType(), pType.getRawType());
            }
        } else if (type instanceof GenericArrayType) {
            //泛型数组
            Type oldComponentType = ((GenericArrayType) type).getGenericComponentType();
            Type newComponentType = compute(genericType, oldComponentType, declaringType);
            if (newComponentType != oldComponentType) {
                //把解析好的变量，重新包装生成Type
                return new GenericArrayTypeImpl(newComponentType);
            }
        } else if (type instanceof WildcardType) {
            //通配符
            WildcardType wildcardType = (WildcardType) type;
            Type[] oldUpperBounds = wildcardType.getUpperBounds();
            Type[] oldLowerBounds = wildcardType.getLowerBounds();
            Type[] newUpperBounds = compute(genericType, oldUpperBounds, declaringType);
            Type[] newLowerBounds = compute(genericType, oldLowerBounds, declaringType);
            if (oldUpperBounds != newUpperBounds || oldLowerBounds != newLowerBounds) {
                return new WildcardTypeImpl(newUpperBounds, newLowerBounds);
            }
        }
        return type;
    }

    /**
     * 计算
     *
     * @param genericType   泛型类型
     * @param types         待解析类型数组
     * @param declaringType 声明所属的泛型类型
     * @return 解析好的类型数组
     */
    protected Type[] compute(final GenericType genericType, final Type[] types, final GenericType declaringType) {
        Type[] newTypes = new Type[types.length];
        boolean flag = false;
        for (int i = 0; i < types.length; i++) {
            //解析每个泛型参数
            newTypes[i] = compute(genericType, types[i], declaringType);
            if (newTypes[i] != types[i]) {
                flag = true;
            }
        }
        return flag ? newTypes : types;
    }

    /**
     * 泛型数组实现
     */
    protected static class GenericArrayTypeImpl implements GenericArrayType {
        protected final Type genericComponentType;

        public GenericArrayTypeImpl(Type genericComponentType) {
            assert genericComponentType != null;
            this.genericComponentType = genericComponentType;
        }

        @Override
        public Type getGenericComponentType() {
            return genericComponentType;
        }

        @Override
        public String toString() {
            Type genericComponentType = getGenericComponentType();
            StringBuilder builder = new StringBuilder();
            if (genericComponentType instanceof Class) {
                builder.append(((Class) genericComponentType).getName());
            } else {
                builder.append(genericComponentType.toString());
            }
            builder.append("[]");
            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof GenericArrayType) {
                GenericArrayType that = (GenericArrayType) obj;
                return this.genericComponentType.equals(that.getGenericComponentType());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.genericComponentType.hashCode();
        }
    }

    /**
     * 参数化泛型实现
     */
    protected static class ParameterizedTypeImpl implements ParameterizedType {

        protected final Type[] actualTypeArguments;
        protected final Type ownerType;
        protected final Type rawType;

        public ParameterizedTypeImpl(Type[] actualTypeArguments, Type ownerType, Type rawType) {
            this.actualTypeArguments = actualTypeArguments;
            this.ownerType = ownerType;
            this.rawType = rawType;
        }

        public Type[] getActualTypeArguments() {
            return actualTypeArguments;
        }

        public Type getOwnerType() {
            return ownerType;
        }

        public Type getRawType() {
            return rawType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ParameterizedTypeImpl that = (ParameterizedTypeImpl) o;

            // Probably incorrect - comparing Object[] arrays with Arrays.equals
            if (!Arrays.equals(actualTypeArguments, that.actualTypeArguments)) {
                return false;
            }
            if (ownerType != null ? !ownerType.equals(that.ownerType) : that.ownerType != null) {
                return false;
            }
            return rawType != null ? rawType.equals(that.rawType) : that.rawType == null;

        }

        @Override
        public int hashCode() {
            int result = actualTypeArguments != null ? Arrays.hashCode(actualTypeArguments) : 0;
            result = 31 * result + (ownerType != null ? ownerType.hashCode() : 0);
            result = 31 * result + (rawType != null ? rawType.hashCode() : 0);
            return result;
        }
    }

    /**
     * 泛型变量
     *
     * @param <D>
     */
    protected static class TypeVariableImpl<D extends GenericDeclaration> implements TypeVariable<D> {
        protected final TypeVariable<D> source;
        protected final Type[] bounds;

        public TypeVariableImpl(TypeVariable<D> source, Type[] bounds) {
            this.source = source;
            this.bounds = bounds;
        }

        @Override
        public Type[] getBounds() {
            return bounds;
        }

        @Override
        public D getGenericDeclaration() {
            return source.getGenericDeclaration();
        }

        @Override
        public String getName() {
            return source.getName();
        }

        @Override
        public AnnotatedType[] getAnnotatedBounds() {
            return source.getAnnotatedBounds();
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            return source.getAnnotation(annotationClass);
        }

        @Override
        public Annotation[] getAnnotations() {
            return source.getAnnotations();
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return source.getDeclaredAnnotations();
        }

        @Override
        public String toString() {
            return getName();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            TypeVariableImpl<?> that = (TypeVariableImpl<?>) o;

            return source.equals(that.source);
        }

        @Override
        public int hashCode() {
            return source.hashCode();
        }
    }

    /**
     * 参数化泛型实现
     */
    protected static class WildcardTypeImpl implements WildcardType {

        protected final Type[] upperBounds;
        protected final Type[] lowerBounds;

        public WildcardTypeImpl(Type[] upperBounds, Type[] lowerBounds) {
            this.upperBounds = upperBounds;
            this.lowerBounds = lowerBounds;
        }

        @Override
        public Type[] getUpperBounds() {
            return upperBounds;
        }

        @Override
        public Type[] getLowerBounds() {
            return lowerBounds;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            WildcardTypeImpl that = (WildcardTypeImpl) o;

            // Probably incorrect - comparing Object[] arrays with Arrays.equals
            if (!Arrays.equals(upperBounds, that.upperBounds)) {
                return false;
            }
            // Probably incorrect - comparing Object[] arrays with Arrays.equals
            return Arrays.equals(lowerBounds, that.lowerBounds);
        }

        @Override
        public int hashCode() {
            int result = Arrays.hashCode(upperBounds);
            result = 31 * result + Arrays.hashCode(lowerBounds);
            return result;
        }

        @Override
        public String toString() {
            Type[] types;
            StringBuilder builder = new StringBuilder();
            if (lowerBounds.length > 0) {
                types = lowerBounds;
                builder.append("? super ");
            } else {
                if (upperBounds.length <= 0 || upperBounds[0].equals(Object.class)) {
                    return "?";
                }
                types = upperBounds;
                builder.append("? extends ");
            }
            for (int i = 0; i < types.length; i++) {
                if (i > 0) {
                    builder.append(" & ");
                }
                builder.append(types[i].getTypeName());
            }
            return builder.toString();
        }
    }
}
