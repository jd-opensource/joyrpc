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
import io.protostuff.Schema;

import java.io.IOException;
import java.util.Date;

public abstract class AbstractSqlDateSchema<T extends Date> implements Schema<T> {

    protected static final String TIME = "time";
    protected Class<T> clazz;


    public AbstractSqlDateSchema(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public boolean isInitialized(final T message) {
        return true;
    }

    @Override
    public String messageName() {
        return clazz.getSimpleName();
    }

    @Override
    public String messageFullName() {
        return clazz.getName();
    }

    @Override
    public Class<? super T> typeClass() {
        return clazz;
    }

    @Override
    public String getFieldName(int number) {
        switch (number) {
            case 1:
                return TIME;
            default:
                return null;
        }
    }

    @Override
    public int getFieldNumber(final String name) {
        switch (name) {
            case TIME:
                return 1;
            default:
                return 0;
        }
    }

    @Override
    public void mergeFrom(final Input input, final T message) throws IOException {
        while (true) {
            int number = input.readFieldNumber(this);
            switch (number) {
                case 0:
                    return;
                case 1:
                    message.setTime(input.readInt64());
                    break;
                default:
                    input.handleUnknownField(number, this);
            }
        }
    }

    @Override
    public void writeTo(final Output output, final T message) throws IOException {
        output.writeInt64(1, message.getTime(), false);
    }

}
