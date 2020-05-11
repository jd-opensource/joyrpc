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

import io.joyrpc.codec.serialization.hessian2.Hessian2Reader;
import io.joyrpc.com.caucho.hessian.io.AbstractHessianInput;
import io.joyrpc.com.caucho.hessian.io.AutowiredObjectDeserializer;
import io.joyrpc.com.caucho.hessian.io.Hessian2Input;
import io.joyrpc.protocol.dubbo.message.DubboInvocation;
import io.joyrpc.protocol.dubbo.serialization.DubboInvocationReader;

import java.io.IOException;

/**
 * DubboInvocation反序列化
 */
public class DubboInvocationDeserializer implements AutowiredObjectDeserializer {

    @Override
    public Class<?> getType() {
        return DubboInvocation.class;
    }

    @Override
    public Object readObject(AbstractHessianInput in) throws IOException {
        return new DubboInvocationReader(new Hessian2Reader((Hessian2Input) in)).read();
    }
}
