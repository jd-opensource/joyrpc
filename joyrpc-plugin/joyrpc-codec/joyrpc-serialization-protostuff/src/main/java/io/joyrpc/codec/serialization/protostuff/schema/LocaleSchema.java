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
import java.util.Locale;

public class LocaleSchema extends AbstractJava8Schema<Locale> {

    public static final LocaleSchema INSTANCE = new LocaleSchema();
    public static final String LOCALE = "locale";
    protected static Field FIELD_BASE_LOCALE = getWriteableField(Locale.class, "baseLocale");
    protected static Field FIELD_LOCALE_EXTENSIONS = getWriteableField(Locale.class, "localeExtensions");

    public LocaleSchema() {
        super(Locale.class);
    }

    @Override
    public String getFieldName(int number) {
        return switch (number) {
            case 1 -> LOCALE;
            default -> null;
        };
    }

    @Override
    public int getFieldNumber(final String name) {
        return switch (name) {
            case LOCALE -> 1;
            default -> 0;
        };
    }

    @Override
    public Locale newMessage() {
        return new Locale("", "", "");
    }

    @Override
    public void mergeFrom(final Input input, final Locale message) throws IOException {
        while (true) {
            int number = input.readFieldNumber(this);
            switch (number) {
                case 0:
                    return;
                case 1:
                    String s = input.readString();
                    int len = s.length();
                    char ch = ' ';

                    int i = 0;
                    for (; i < len && ('a' <= (ch = s.charAt(i)) && ch <= 'z' || 'A' <= ch && ch <= 'Z' || '0' <= ch && ch <= '9'); i++) {
                    }
                    String language = s.substring(0, i);
                    String country = null;
                    String var = null;
                    if (ch == '-' || ch == '_') {
                        int head = ++i;
                        for (; i < len && ('a' <= (ch = s.charAt(i)) && ch <= 'z' || 'A' <= ch && ch <= 'Z' || '0' <= ch && ch <= '9'); i++) {
                        }
                        country = s.substring(head, i);
                    }
                    if (ch == '-' || ch == '_') {
                        int head = ++i;
                        for (; i < len && ('a' <= (ch = s.charAt(i)) && ch <= 'z' || 'A' <= ch && ch <= 'Z' || '0' <= ch && ch <= '9'); i++) {
                        }
                        var = s.substring(head, i);
                    }
                    var = var == null ? "" : var;
                    country = country == null ? "" : country;
                    Locale locale = new Locale(language, country, var);
                    setValue(FIELD_BASE_LOCALE, message, getValue(FIELD_BASE_LOCALE, locale));
                    setValue(FIELD_LOCALE_EXTENSIONS, message, getValue(FIELD_LOCALE_EXTENSIONS, locale));
                    break;
                default:
                    input.handleUnknownField(number, this);
            }
        }
    }

    @Override
    public void writeTo(final Output output, final Locale message) throws IOException {
        output.writeString(1, message.toString(), false);
    }
}
