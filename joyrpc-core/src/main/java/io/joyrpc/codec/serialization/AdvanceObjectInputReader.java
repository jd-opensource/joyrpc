package io.joyrpc.codec.serialization;

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

import java.io.IOException;
import java.io.ObjectInput;

/**
 * 高级的Java对象读取器
 */
public class AdvanceObjectInputReader extends ObjectInputReader {

    public AdvanceObjectInputReader(ObjectInput input) {
        super(input);
    }

    @Override
    public String readUTF() throws IOException {
        int len = input.readInt();
        if (len < 0) {
            return null;
        }
        return input.readUTF();
    }

    @Override
    public Object readObject() throws IOException {
        try {
            byte b = input.readByte();
            if (b == 0) {
                return null;
            }
            return input.readObject();
        } catch (ClassNotFoundException e) {
            return new IOException(e.getMessage(), e);
        }
    }
}