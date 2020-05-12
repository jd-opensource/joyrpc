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
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

public class ZoneIdSchema extends AbstractJava8Schema<ZoneId> {

    public static final ZoneIdSchema INSTANCE = new ZoneIdSchema();
    public static final String ID = "id";

    protected static final String[] FIELD_NAMES = new String[]{ID};

    protected static final Map<String, Integer> FIELD_MAP = new HashMap(1);

    protected static Field FIELD_ID;

    static {
        FIELD_MAP.put(ID, 1);
        ZoneId zoneId = ZoneId.systemDefault();
        FIELD_ID = getWriteableField(zoneId.getClass(), ID);
    }

    public ZoneIdSchema() {
        super(ZoneId.class);
    }

    @Override
    public String getFieldName(int number) {
        switch (number) {
            case 1:
                return ID;
            default:
                return null;
        }
    }

    @Override
    public int getFieldNumber(final String name) {
        return FIELD_MAP.get(name);
    }

    @Override
    public ZoneId newMessage() {
        return ZoneId.systemDefault();
    }

    @Override
    public void mergeFrom(final Input input, final ZoneId message) throws IOException {
        while (true) {
            int number = input.readFieldNumber(this);
            switch (number) {
                case 0:
                    return;
                case 1:
                    setValue(FIELD_ID, message, input.readString());
                    break;
                default:
                    input.handleUnknownField(number, this);
            }
        }
    }

    @Override
    public void writeTo(final Output output, final ZoneId message) throws IOException {
        output.writeString(1, message.getId(), false);
    }
}
