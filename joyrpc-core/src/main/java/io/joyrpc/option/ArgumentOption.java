package io.joyrpc.option;

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

import io.joyrpc.util.IDLMethod;
import io.joyrpc.util.IDLMethodDesc;

import java.lang.reflect.Method;
import java.util.function.Supplier;

import static io.joyrpc.util.ClassUtils.getCanonicalNames;
import static io.joyrpc.util.ClassUtils.getInitialValue;

/**
 * 方法参数类型
 */
public class ArgumentOption {
    /**
     * 参数类
     */
    protected Class[] classes;
    /**
     * 参数类名
     */
    protected String[] types;
    /**
     * 默认返回值
     */
    protected Object defaultValue;
    /**
     * 方法描述提供者
     */
    protected Supplier<IDLMethodDesc> supplier;

    public ArgumentOption(IDLMethod idlMethod) {
        Method method = idlMethod.getMethod();
        this.classes = method.getParameterTypes();
        this.types = getCanonicalNames(classes);
        this.supplier = idlMethod.getSupplier();
        this.defaultValue = getInitialValue(method.getReturnType());
    }

    public Class[] getClasses() {
        return classes;
    }

    public String[] getTypes() {
        return types;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public IDLMethodDesc getIDLMethodDesc() {
        return supplier.get();
    }
}
