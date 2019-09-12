package io.joyrpc.extension.condition;

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

import java.lang.annotation.Annotation;

/**
 * 条件匹配
 */
public interface Condition {

    /**
     * 匹配
     *
     * @param classLoader 类加载器
     * @param clazz       插件实现类
     * @param annotation  注解
     * @return 是否满足
     */
    boolean match(ClassLoader classLoader, Class clazz, Annotation annotation);

}
