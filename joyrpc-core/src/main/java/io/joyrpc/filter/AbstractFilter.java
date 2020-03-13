package io.joyrpc.filter;

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

import io.joyrpc.extension.URL;
import io.joyrpc.extension.URLOption;

import static io.joyrpc.constants.Constants.METHOD_KEY_FUNC;

/**
 * 抽象过滤链
 */
public abstract class AbstractFilter implements Filter {
    /**
     * URL
     */
    protected URL url;
    /**
     * 接口类，在泛型调用情况下，clazz和clazzName可能不相同
     */
    protected Class clazz;
    /**
     * 接口类名
     */
    protected String className;

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public void setUrl(final URL url) {
        this.url = url;
    }

    @Override
    public void setClass(final Class clazz) {
        this.clazz = clazz;
    }

    @Override
    public void setClassName(final String className) {
        this.className = className;
    }

    /**
     * 获取参数选项
     *
     * @param method       方法名
     * @param name         参数名称
     * @param defaultValue 默认值
     * @param <T>
     * @return
     */
    protected <T> URLOption<T> getOption(final String method, final String name, final T defaultValue) {
        return new URLOption<>(METHOD_KEY_FUNC.apply(method, name), defaultValue);
    }

}
