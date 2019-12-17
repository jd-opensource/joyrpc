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

import java.util.function.Function;

import static io.joyrpc.GenericService.GENERIC;

/**
 * 处理泛型的方法选项
 *
 * @param <T>
 */
public class GenericMethodOption<T> extends MethodOption.NameKeyOption<T> {

    /**
     * 构造函数
     *
     * @param clazz
     * @param className
     * @param nameFunction
     */
    public GenericMethodOption(final Class clazz, final String className, final Function<String, T> nameFunction) {
        super(GENERIC.test(clazz) ? null : clazz, className, nameFunction);
    }
}
