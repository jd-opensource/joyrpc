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
import java.time.Period;
import java.util.HashMap;
import java.util.Map;

public class PeriodSchema extends AbstractJava8Schema<Period> {

    public static final PeriodSchema INSTANCE = new PeriodSchema();
    public static final String YEARS = "years";
    public static final String MONTHS = "months";
    public static final String DAYS = "days";

    protected static final String[] FIELD_NAMES = new String[]{YEARS, MONTHS, DAYS};

    protected static final Map<String, Integer> FIELD_MAP = new HashMap(3);

    protected static Field FIELD_YEARS = getWriteableField(Period.class, YEARS);
    protected static Field FIELD_MONTHS = getWriteableField(Period.class, MONTHS);
    protected static Field FIELD_DAYS = getWriteableField(Period.class, DAYS);

    static {
        FIELD_MAP.put(YEARS, 1);
        FIELD_MAP.put(MONTHS, 2);
        FIELD_MAP.put(DAYS, 3);
    }


    public PeriodSchema() {
        super(Period.class);
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
    public Period newMessage() {
        return Period.of(0, 0, 0);
    }

    @Override
    public void mergeFrom(final Input input, final Period message) throws IOException {
        while (true) {
            int number = input.readFieldNumber(this);
            switch (number) {
                case 0:
                    return;
                case 1:
                    setValue(FIELD_YEARS, message, input.readInt32());
                    break;
                case 2:
                    setValue(FIELD_MONTHS, message, input.readInt32());
                    break;
                case 3:
                    setValue(FIELD_DAYS, message, input.readInt32());
                    break;
                default:
                    input.handleUnknownField(number, this);
            }
        }
    }

    @Override
    public void writeTo(final Output output, final Period message) throws IOException {
        output.writeInt32(1, message.getYears(), false);
        output.writeInt32(2, message.getMonths(), false);
        output.writeInt32(3, message.getDays(), false);
    }
}
