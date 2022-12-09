package io.joyrpc.codec.serialization.fastjson2.java8;

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

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import io.joyrpc.codec.serialization.fastjson2.AbstractSerialization;

import java.lang.reflect.Type;
import java.time.Instant;

/**
 * Instant序列化
 */
public class InstantSerialization extends AbstractSerialization<Instant> {

    public static final InstantSerialization INSTANCE = new InstantSerialization();

    @Override
    protected void doWrite(final JSONWriter jsonWriter, final Instant object, final Object fieldName, final Type fieldType, final long features) {
        jsonWriter.writeString(object.toString());
    }

    @Override
    protected Instant doRead(final JSONReader jsonReader, final Type fieldType, final Object fieldName, final long features) {
        return Instant.parse(jsonReader.readString());
    }

}
