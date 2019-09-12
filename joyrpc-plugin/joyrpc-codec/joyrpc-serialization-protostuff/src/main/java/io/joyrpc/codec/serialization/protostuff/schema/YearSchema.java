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
import java.time.Year;
import java.util.HashMap;
import java.util.Map;

public class YearSchema extends AbstractJava8Schema<Year> {

    public static final YearSchema INSTANCE = new YearSchema();
    public static final String YEAR = "year";

    protected static final String[] FIELD_NAMES = new String[]{YEAR};

    protected static final Map<String, Integer> FIELD_MAP = new HashMap(1);

    protected static Field FIELD_YEAR = getWriteableField(Year.class, YEAR);

    static {
        FIELD_MAP.put(YEAR, 1);
    }


    public YearSchema() {
        super(Year.class);
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
    public Year newMessage() {
        return Year.of(2000);
    }

    @Override
    public void mergeFrom(final Input input, final Year message) throws IOException {
        while (true) {
            int number = input.readFieldNumber(this);
            switch (number) {
                case 0:
                    return;
                case 1:
                    setValue(FIELD_YEAR, message, input.readInt32());
                    break;
                default:
                    input.handleUnknownField(number, this);
            }
        }
    }

    @Override
    public void writeTo(final Output output, final Year message) throws IOException {
        output.writeInt32(1, message.getValue(), false);
    }
}
