package io.joyrpc.com.caucho.hessian.io.java8;

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

import io.joyrpc.com.caucho.hessian.io.AbstractHessianOutput;
import io.joyrpc.com.caucho.hessian.io.AbstractSerializer;

import java.io.IOException;

/**
 * Java8时间序列化器
 */
public class Java8TimeSerializer<T extends Java8TimeWrapper> extends AbstractSerializer {

    //handle 具体类型
    protected Class<T> handleType;

    protected Java8TimeSerializer(Class<T> handleType) {
        this.handleType = handleType;
    }

    public static <T extends Java8TimeWrapper> Java8TimeSerializer of(Class<T> handleType) {
        return new Java8TimeSerializer(handleType);
    }

    @Override
    public void writeObject(final Object obj, final AbstractHessianOutput out) throws IOException {
        if (obj == null) {
            out.writeNull();
            return;
        }
        T handle = null;
        try {
            handle = handleType.newInstance();
        } catch (Exception e) {
            throw new IOException(String.format("failed to instance class %s ", handleType.getName()), e);
        }
        handle.wrap(obj);
        out.writeObject(handle);
    }
}
