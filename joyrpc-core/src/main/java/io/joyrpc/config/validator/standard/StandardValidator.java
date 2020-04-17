package io.joyrpc.config.validator.standard;

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

import io.joyrpc.config.validator.InterfaceValidator;
import io.joyrpc.extension.Extension;
import io.joyrpc.util.GenericChecker;
import io.joyrpc.util.GenericChecker.Scope;
import io.joyrpc.util.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ValidationException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static io.joyrpc.util.ClassUtils.*;
import static io.joyrpc.util.GenericChecker.NONE_STATIC_METHOD;

/**
 * @date 25/6/2019
 */
@Extension(value = "standard", order = 100)
public class StandardValidator implements InterfaceValidator {
    private static final Logger logger = LoggerFactory.getLogger(StandardValidator.class);

    /**
     * 标准推荐类
     */
    protected Set<Class> standards = new HashSet<>();
    /**
     * 不建议的类
     */
    protected Set<Class> noRecommendations = new CopyOnWriteArraySet<>();

    /**
     * 构造函数
     */
    public StandardValidator() {
        List<String> names = Resource.lines(new String[]{"META-INF/system_standard_type", "user_standard_type"}, true);
        for (String name : names) {
            name = name.trim();
            if (!name.isEmpty()) {
                try {
                    standards.add(forName(name));
                } catch (ClassNotFoundException e) {
                    logger.error("class in standards file is not found ." + name);
                }
            }
        }
    }

    @Override
    public void validate(final Class clazz) throws ValidationException {
        Set<String> overloads = new HashSet<>();
        GenericChecker checker = new GenericChecker();
        checker.checkMethods(clazz, NONE_STATIC_METHOD.and(method -> {
            if (!overloads.add(method.getName())) {
                onOverloadMethod(clazz, method);
                return false;
            } else {
                return true;
            }
        }), new MyConsumer(checker, c -> standards.contains(c), noRecommendations));
    }


    /**
     * 重载方法
     *
     * @param clazz
     * @param method
     */
    protected void onOverloadMethod(final Class clazz, final Method method) {
        throw new ValidationException(String.format("Overloaded methods are not supported. %s.%s",
                clazz.getName(), method.getName()));
    }


    /**
     * 检查出的类消费者
     */
    protected static class MyConsumer implements BiConsumer<Class, Scope> {
        /**
         * 检查
         */
        protected GenericChecker checker;
        /**
         * 标准类检查
         */
        protected Function<Class, Boolean> standard;
        /**
         * 不建议的类
         */
        protected Set<Class> noRecommendations;

        /**
         * 构造函数
         *
         * @param checker
         * @param standard
         * @param noRecommendations
         */
        public MyConsumer(final GenericChecker checker, Function<Class, Boolean> standard, Set<Class> noRecommendations) {
            this.checker = checker;
            this.standard = standard;
            this.noRecommendations = noRecommendations;
        }

        @Override
        public void accept(final Class clazz, final Scope scope) {
            if (clazz.isEnum()) {
                onEnum(clazz, scope);
            } else if (Throwable.class.isAssignableFrom(clazz)) {
                //异常
                onThrowable(clazz, scope);
            } else if (isJavaClass(clazz)) {
                onJava(clazz, scope);
            } else if (clazz.isInterface()) {
                //自定义接口
                onCustomInterface(clazz, scope);
            } else {
                if (Modifier.isAbstract(clazz.getModifiers())) {
                    //抽象类
                    onCustomAbstract(clazz, scope);
                } else if (getDefaultConstructor(clazz) == null) {
                    //检查是否有默认函数
                    onNoDefaultConstructor(clazz);
                }
                if (!(clazz instanceof Serializable)) {
                    //没有序列化
                    onNotSerializable(clazz);
                }
                checker.checkFields(getGenericClass(clazz), GenericChecker.NONE_STATIC_FINAL_TRANSIENT_FIELD, this);
            }
        }

        /**
         * Java内置类型
         *
         * @param clazz
         * @param scope
         */
        protected void onJava(final Class clazz, final Scope scope) {
            if (!isStandard(clazz)) {
                //不是标准类型
                onNotStandard(clazz);
            }
        }

        /**
         * 检查异常
         *
         * @param clazz
         * @param scope
         */
        protected void onThrowable(final Class clazz, final Scope scope) {

        }

        /**
         * 检查是否是标准推进的类型
         *
         * @param clazz
         * @return
         */
        protected boolean isStandard(final Class clazz) {
            return standard.apply(clazz);
        }

        /**
         * 枚举类型
         *
         * @param clazz
         * @param scope
         */
        protected void onEnum(final Class clazz, final Scope scope) {
        }

        /**
         * 没有默认序列化
         *
         * @param clazz
         */
        protected void onNoDefaultConstructor(final Class clazz) {
            throw new ValidationException(String.format("This type does not have a default constructor. %s", clazz.getName()));
        }

        /**
         * 不能序列化
         *
         * @param clazz
         */
        protected void onNotSerializable(final Class clazz) {
            throw new ValidationException(String.format("The type is not implement serializable. %s", clazz));
        }

        /**
         * 参数、返回值或者字段上有接口
         *
         * @param clazz
         */
        protected void onCustomInterface(final Class clazz, final Scope scope) {
            //参数允许是Callback
            if (scope == Scope.PARAMETER) {
                return;
            }
            throw new ValidationException(String.format("The interface is not allowed at %s. %s, it may cause serialization problems.", scope.getName(), clazz.getName()));
        }

        /**
         * 抽象类
         *
         * @param clazz
         */
        protected void onCustomAbstract(final Class clazz, final Scope scope) {
            throw new ValidationException(String.format("The type is abstract at %s. %s, it may cause serialization problems.", scope.getName(), clazz.getName()));
        }

        /**
         * 非推荐的标准类型
         *
         * @param clazz
         */
        protected void onNotStandard(final Class clazz) {
            if (noRecommendations.add(clazz)) {
                //防止多次输出日志
                logger.warn(String.format("This type is not recommended. %s", clazz));
            }
        }
    }

}
