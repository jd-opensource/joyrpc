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
import sun.util.locale.BaseLocale;
import sun.util.locale.LocaleExtensions;
import sun.util.locale.LocaleUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LocaleSchema extends AbstractJava8Schema<Locale> {

    public static final LocaleSchema INSTANCE = new LocaleSchema();
    public static final String LOCALE = "locale";

    protected static final Map<String, Integer> FIELD_MAP = new HashMap(3);

    protected static Field FIELD_BASE_LOCALE = getWriteableField(Locale.class, "baseLocale");
    protected static Field FIELD_LOCALE_EXTENSIONS = getWriteableField(Locale.class, "localeExtensions");

    static {
        FIELD_MAP.put(LOCALE, 1);
    }

    public LocaleSchema() {
        super(Locale.class);
    }

    @Override
    public String getFieldName(int number) {
        switch (number) {
            case 1:
                return LOCALE;
            default:
                return null;
        }
    }

    @Override
    public int getFieldNumber(final String name) {
        return FIELD_MAP.get(name);
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
                    setValue(FIELD_BASE_LOCALE, message, BaseLocale.getInstance(convertOldISOCodes(language), "", country, var));
                    setValue(FIELD_LOCALE_EXTENSIONS, message, getCompatibilityExtensions(language, "", country, var));

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

    protected static String convertOldISOCodes(String language) {
        // we accept both the old and the new ISO codes for the languages whose ISO
        // codes have changed, but we always store the OLD code, for backward compatibility
        language = LocaleUtils.toLowerString(language).intern();
        if (language == "he") {
            return "iw";
        } else if (language == "yi") {
            return "ji";
        } else if (language == "id") {
            return "in";
        } else {
            return language;
        }
    }

    protected static LocaleExtensions getCompatibilityExtensions(String language,
                                                                 String script,
                                                                 String country,
                                                                 String variant) {
        LocaleExtensions extensions = null;
        // Special cases for backward compatibility support
        if (LocaleUtils.caseIgnoreMatch(language, "ja")
                && script.length() == 0
                && LocaleUtils.caseIgnoreMatch(country, "jp")
                && "JP".equals(variant)) {
            // ja_JP_JP -> u-ca-japanese (calendar = japanese)
            extensions = LocaleExtensions.CALENDAR_JAPANESE;
        } else if (LocaleUtils.caseIgnoreMatch(language, "th")
                && script.length() == 0
                && LocaleUtils.caseIgnoreMatch(country, "th")
                && "TH".equals(variant)) {
            // th_TH_TH -> u-nu-thai (numbersystem = thai)
            extensions = LocaleExtensions.NUMBER_THAI;
        }
        return extensions;
    }
}
