package io.joyrpc.protocol.dubbo.serialization.hessian;

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

import io.joyrpc.codec.serialization.hessian2.Hessian2Writer;
import io.joyrpc.com.caucho.hessian.io.AbstractHessianOutput;
import io.joyrpc.com.caucho.hessian.io.AutowiredObjectSerializer;
import io.joyrpc.protocol.dubbo.message.DubboInvocation;

import java.io.IOException;

/**
 * DubboInvocation序列化
 */
public class DubboInvocationSerializer implements AutowiredObjectSerializer {

    @Override
    public Class<?> getType() {
        return DubboInvocation.class;
    }

    @Override
    public void writeObject(final Object obj, final AbstractHessianOutput out) throws IOException {
        ((DubboInvocation) obj).write(new Hessian2Writer(out));
    }
}
