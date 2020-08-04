package io.joyrpc.permission;

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

import io.joyrpc.util.GenericChecker;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;

import static io.joyrpc.util.ClassUtils.getGenericClass;
import static io.joyrpc.util.ClassUtils.isJavaClass;
import static io.joyrpc.util.GenericChecker.NONE_STATIC_TRANSIENT_FIELD;
import static io.joyrpc.util.GenericChecker.NONE_STATIC_METHOD;

/**
 * 序列化类型扫描器
 */
public class SerializerTypeScanner {

    protected Class<?> clazz;
    protected GenericChecker checker;
    protected Set<Class<?>> uniques = new HashSet<>();

    public SerializerTypeScanner(Class<?> clazz) {
        this.clazz = clazz;
        this.checker = new GenericChecker();
    }

    /**
     * 扫描
     *
     * @return 序列化白名单
     */
    public Set<Class<?>> scan() {
        checker.checkMethods(clazz, NONE_STATIC_METHOD, new BiConsumer<Class, GenericChecker.Scope>() {
            @Override
            public void accept(final Class clazz, final GenericChecker.Scope scope) {
                if (Void.class == clazz || void.class == clazz || CompletionStage.class.isAssignableFrom(clazz)) {
                    //不能序列化的
                } else if (clazz.isEnum()) {
                    uniques.add(clazz);
                } else if (Throwable.class.isAssignableFrom(clazz)) {
                    //异常
                    uniques.add(clazz);
                } else if (isJavaClass(clazz)) {
                    uniques.add(clazz);
                } else if (clazz.isInterface()) {
                    //自定义接口
                    checker.checkMethods(clazz, NONE_STATIC_METHOD, this);
                } else {
                    uniques.add(clazz);
                    checker.checkFields(getGenericClass(clazz), NONE_STATIC_TRANSIENT_FIELD, this);
                }
            }
        });
        return uniques;
    }
}
