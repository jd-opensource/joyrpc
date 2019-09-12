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
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class LocalDateSchema extends AbstractJava8Schema<LocalDate> {

    public static final LocalDateSchema INSTANCE = new LocalDateSchema();
    public static final String YEAR = "year";
    public static final String MONTH = "month";
    public static final String DAY = "day";

    protected static final String[] FIELD_NAMES = new String[]{YEAR, MONTH, DAY};

    protected static final Map<String, Integer> FIELD_MAP = new HashMap(3);

    protected static Field FIELD_YEAR = getWriteableField(LocalDate.class, YEAR);
    protected static Field FIELD_MONTH = getWriteableField(LocalDate.class, MONTH);
    protected static Field FIELD_DAY = getWriteableField(LocalDate.class, DAY);

    static {
        FIELD_MAP.put(YEAR, 1);
        FIELD_MAP.put(MONTH, 2);
        FIELD_MAP.put(DAY, 2);
    }

    public LocalDateSchema() {
        super(LocalDate.class);
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
    public LocalDate newMessage() {
        return LocalDate.now();
    }

    @Override
    public void mergeFrom(final Input input, final LocalDate message) throws IOException {
        while (true) {
            int number = input.readFieldNumber(this);
            switch (number) {
                case 0:
                    return;
                case 1:
                    setValue(FIELD_YEAR, message, input.readInt32());
                    break;
                case 2:
                    setValue(FIELD_MONTH, message, (short) input.readInt32());
                    break;
                case 3:
                    setValue(FIELD_DAY, message, (short) input.readInt32());
                    break;
                default:
                    input.handleUnknownField(number, this);
            }
        }
    }

    @Override
    public void writeTo(final Output output, final LocalDate message) throws IOException {
        output.writeInt32(1, message.getYear(), false);
        output.writeInt32(2, message.getMonthValue(), false);
        output.writeInt32(3, message.getDayOfMonth(), false);
    }
}
