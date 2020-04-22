package io.joyrpc.proxy;

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

import io.joyrpc.extension.Extension;

/**
 * 运行时编译器
 */
@Extension("compiler")
public interface JCompiler {

    /**
     * 编译
     *
     * @param className 类名
     * @param context   内容
     * @return 编译好的内
     * @throws ClassNotFoundException 类没有找到
     */
    Class<?> compile(final String className, final CharSequence context) throws ClassNotFoundException;
}
