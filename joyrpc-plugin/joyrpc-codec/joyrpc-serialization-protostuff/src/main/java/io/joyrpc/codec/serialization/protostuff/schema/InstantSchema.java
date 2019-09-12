package io.joyrpc.codec.serialization.protostuff.schema;

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

import io.protostuff.Input;
import io.protostuff.Output;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class InstantSchema extends AbstractJava8Schema<Instant> {

    public static final InstantSchema INSTANCE = new InstantSchema();
    public static final String SECONDS = "seconds";
    public static final String NANOS = "nanos";

    protected static final String[] FIELD_NAMES = new String[]{SECONDS, NANOS};

    protected static final Map<String, Integer> FIELD_MAP = new HashMap(2);

    protected static Field FIELD_SECONDS = getWriteableField(Instant.class, SECONDS);
    protected static Field FIELD_NANOS = getWriteableField(Instant.class, NANOS);

    static {
        FIELD_MAP.put(SECONDS, 1);
        FIELD_MAP.put(NANOS, 2);
    }


    public InstantSchema() {
        super(Instant.class);
    }

    @Override
    public String getFieldName(int number) {
        return FIELD_NAMES[number];
    }

    @Override
    public int getFieldNumber(final String name) {
        return FIELD_MAP.get(name);
    }

    @Override
    public Instant newMessage() {
        return Instant.now();
    }

    @Override
    public void mergeFrom(final Input input, final Instant message) throws IOException {
        while (true) {
            int number = input.readFieldNumber(this);
            switch (number) {
                case 0:
                    return;
                case 1:
                    setValue(FIELD_SECONDS, message, input.readInt64());
                    break;
                case 2:
                    setValue(FIELD_NANOS, message, input.readInt32());
                    break;
                default:
                    input.handleUnknownField(number, this);
            }
        }
    }

    @Override
    public void writeTo(final Output output, final Instant message) throws IOException {
        output.writeInt64(1, message.getEpochSecond(), false);
        output.writeInt32(2, message.getNano(), false);
    }
}
