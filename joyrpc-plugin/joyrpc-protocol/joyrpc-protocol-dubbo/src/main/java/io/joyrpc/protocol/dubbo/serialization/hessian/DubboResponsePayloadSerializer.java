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
import io.joyrpc.protocol.dubbo.message.DubboResponsePayload;

import java.io.IOException;

/**
 * DubboResponsePayload序列化
 */
public class DubboResponsePayloadSerializer implements AutowiredObjectSerializer {

    @Override
    public Class<?> getType() {
        return DubboResponsePayload.class;
    }

    @Override
    public void writeObject(Object obj, AbstractHessianOutput out) throws IOException {
        ((DubboResponsePayload) obj).write(new Hessian2Writer(out));
    }
}
