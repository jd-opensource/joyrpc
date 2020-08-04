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
import io.joyrpc.permission.BlackWhiteList;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Java输入流
 */
public class JavaInputStream extends ObjectInputStream implements ObjectInput, ObjectStreamConstants {

    protected BlackWhiteList<Class<?>> blackWhiteList;

    private final static Set<String> javaWhiteList = new HashSet<String>() {{
        add("java.time.Ser");
    }};

    public JavaInputStream(final InputStream in, final BlackWhiteList<Class<?>> blackWhiteList) throws IOException {
        super(in);
        this.blackWhiteList = blackWhiteList;
    }

    public JavaInputStream(final BlackWhiteList blackWhiteList) throws IOException, SecurityException {
        this.blackWhiteList = blackWhiteList;
    }

    @Override
    protected Class<?> resolveClass(final ObjectStreamClass desc) throws IOException, ClassNotFoundException, SerializerException {
        Class<?> result = super.resolveClass(desc);
        if ((blackWhiteList != null && !blackWhiteList.isValid(result))
                && !javaWhiteList.contains(desc.getName())) {
            throw new SerializerException("Failed to decode class " + result.getName() + " by java serialization, it is not passed through blackWhiteList.");
        }
        return result;
    }


}
