package io.joyrpc.protocol.dubbo.serialization.protostuff;

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

import io.joyrpc.protocol.dubbo.message.DubboInvocation;
import io.protostuff.AutowiredObjectSerializer;
import io.protostuff.Input;
import io.protostuff.Output;

import java.io.IOException;

/**
 * DubboInvocation序列化
 */
public class DubboInvocationSerializer implements AutowiredObjectSerializer {

    @Override
    public Class<?> typeClass() {
        return DubboInvocation.class;
    }

    @Override
    public String getFieldName(int i) {
        return null;
    }

    @Override
    public int getFieldNumber(String s) {
        return 0;
    }

    @Override
    public boolean isInitialized(Object o) {
        return false;
    }

    @Override
    public Object newMessage() {
        return null;
    }

    @Override
    public String messageName() {
        return null;
    }

    @Override
    public String messageFullName() {
        return null;
    }

    @Override
    public void mergeFrom(Input input, Object o) throws IOException {
    }

    @Override
    public void writeTo(Output output, Object o) throws IOException {

    }

}
