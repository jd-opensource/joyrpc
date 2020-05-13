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

import io.joyrpc.codec.serialization.ObjectOutputWriter;

import java.io.IOException;
import java.io.ObjectOutput;

/**
 * Dubbo对象写入器
 */
public class DubboObjectOutputWriter extends ObjectOutputWriter {

    public DubboObjectOutputWriter(ObjectOutput output) {
        super(output);
    }

    @Override
    public void writeObject(Object v) throws IOException {
        if (v == null) {
            output.writeByte(0);
        } else {
            output.writeByte(1);
            output.writeObject(v);
        }
    }

    @Override
    public void writeUTF(String s) throws IOException {
        if (s == null) {
            output.writeInt(-1);
        } else {
            output.writeInt(s.length());
            output.writeUTF(s);
        }
    }
}
