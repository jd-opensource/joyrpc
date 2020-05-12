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
import java.util.HashMap;
import java.util.Map;

/**
 * DubboInvocation序列化
 */
public class DubboInvocationSerializer implements AutowiredObjectSerializer<DubboInvocation> {

    protected static final String[] FIELDS = new String[]{
            "targetServiceUniqueName", "methodName", "serviceName", "parameterTypesDesc",
            "compatibleParamSignatures", "arguments", "attachments"
    };

    protected static final Map<String, Integer> FIELD_NUMBERS = new HashMap<>(10);


    static {
        FIELD_NUMBERS.put("targetServiceUniqueName", 1);
        FIELD_NUMBERS.put("methodName", 2);
        FIELD_NUMBERS.put("serviceName", 3);
        FIELD_NUMBERS.put("parameterTypesDesc", 4);
        FIELD_NUMBERS.put("compatibleParamSignatures", 5);
        FIELD_NUMBERS.put("arguments", 6);
        FIELD_NUMBERS.put("attachments", 7);
    }

    @Override
    public Class<DubboInvocation> typeClass() {
        return DubboInvocation.class;
    }

    @Override
    public String getFieldName(int number) {
        return FIELDS[number];
    }

    @Override
    public int getFieldNumber(String name) {
        return FIELD_NUMBERS.get(name);
    }

    @Override
    public boolean isInitialized(DubboInvocation message) {
        return false;
    }

    @Override
    public DubboInvocation newMessage() {
        return new DubboInvocation();
    }

    @Override
    public String messageName() {
        return DubboInvocation.class.getSimpleName();
    }

    @Override
    public String messageFullName() {
        return DubboInvocation.class.getName();
    }

    @Override
    public void mergeFrom(Input input, DubboInvocation message) throws IOException {
    }

    @Override
    public void writeTo(final Output output, final DubboInvocation message) throws IOException {
//        output.writeString(1, message.gett, false);
//        output.writeString(2, message.gett, false);
//        output.writeString(3, message.gett, false);
//        output.writeString(4, message.gett, false);
//        output.writeString(5, message., false);
//        output.writeObject(6, message.getArgs(), false);
//        output.writeObject(7, message.getAttachments(), false);
    }

}
