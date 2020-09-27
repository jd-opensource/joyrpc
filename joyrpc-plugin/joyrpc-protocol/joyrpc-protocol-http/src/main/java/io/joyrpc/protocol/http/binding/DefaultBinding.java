package io.joyrpc.protocol.http.binding;

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

import io.joyrpc.extension.Extension;
import io.joyrpc.protocol.http.URLBinding;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URLDecoder;

/**
 * 默认参数绑定器
 */
@Extension("default")
public class DefaultBinding implements URLBinding {

    @Override
    public Object convert(final Class<?> clazz, final Method method, final Parameter parameter, final int index,
                          final String value) {
        Class<?> type = parameter.getType();
        if (String.class.equals(type)) {
            try {
                return URLDecoder.decode(value, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                return value;
            }
        } else if (boolean.class.equals(type) || Boolean.class.equals(type)) {
            return Boolean.parseBoolean(value);
        } else if (byte.class.equals(type) || Byte.class.equals(type)) {
            return Byte.decode(value);
        } else if (short.class.equals(type) || Short.class.equals(type)) {
            return Short.decode(value);
        } else if (char.class.equals(type) || Character.class.equals(type)) {
            return value.charAt(0);
        } else if (int.class.equals(type) || Integer.class.equals(type)) {
            return Integer.decode(value);
        } else if (long.class.equals(type) || Long.class.equals(type)) {
            return Long.decode(value);
        } else if (float.class.equals(type) || Float.class.equals(type)) {
            return Float.valueOf(value);
        } else if (double.class.equals(type) || Double.class.equals(type)) {
            return Double.valueOf(value);
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
