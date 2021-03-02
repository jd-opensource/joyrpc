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

/**
 * 接口描述语言对应的类型转换器
 */
public class IDLConverter {
    /**
     * 参数转换成包装对象函数
     */
    protected final Function<Object[], Object> toWrapper;
    /**
     * 包装对象转换成参数函数
     */
    protected final Function<Object, Object[]> toParameter;

    public IDLConverter(final Function<Object[], Object> toWrapper,
                        final Function<Object, Object[]> toParameter) {
        this.toWrapper = toWrapper;
        this.toParameter = toParameter;
    }

    public Function<Object[], Object> getToWrapper() {
        return toWrapper;
    }

    public Function<Object, Object[]> getToParameter() {
        return toParameter;
    }
}
