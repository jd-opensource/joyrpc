package io.joyrpc.codec.serialization.java;

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

import io.joyrpc.exception.SerializerException;
import io.joyrpc.permission.BlackList;
import io.joyrpc.permission.BlackWhiteList;

import java.io.*;

/**
 * Java输入流
 */
public class JavaInputStream extends ObjectInputStream implements ObjectInput, ObjectStreamConstants {

    protected BlackList<String> blackList;

    public JavaInputStream(final InputStream in, final BlackList<String> blackList) throws IOException {
        super(in);
        this.blackList = blackList;
    }

    public JavaInputStream(final BlackWhiteList blackList) throws IOException, SecurityException {
        this.blackList = blackList;
    }

    @Override
    protected Class<?> resolveClass(final ObjectStreamClass desc) throws IOException, ClassNotFoundException, SerializerException {
        if (blackList != null && blackList.isBlack(desc.getName())) {
            throw new SerializerException("Failed to decode class " + desc.getName() + " by java serialization, it is in blacklist");
        }
        return super.resolveClass(desc);
    }
}
