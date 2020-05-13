package io.joyrpc.protocol.dubbo.serialization.java;

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

import io.joyrpc.codec.serialization.ObjectInputReader;

import java.io.IOException;
import java.io.ObjectInput;

/**
 * Dubbo对象读取器
 */
public class DubboObjectInputReader extends ObjectInputReader {

    public DubboObjectInputReader(ObjectInput input) {
        super(input);
    }

    @Override
    public <T> T readObject(Class<T> clazz) throws IOException {
        return (T) readObject();
    }

    @Override
    public Object readObject() throws IOException {
        byte b = input.readByte();
        if (b == 0) {
            return null;
        }
        return readObject();
    }

    @Override
    public String readUTF() throws IOException {
        int len = input.readInt();
        if (len < 0) {
            return null;
        }
        return input.readUTF();
    }

}
