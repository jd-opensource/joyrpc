package io.joyrpc.protocol.telnet.util;

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

import io.joyrpc.util.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

/**
 * @date: 2019/1/22
 */
public class TelnetUtils {

    private static Logger logger = LoggerFactory.getLogger(TelnetUtils.class);

    //Telnet取得参数信息
    public static void scanParameter(Class<?> parameter, Set<Class<?>> checkSet, StringBuilder builder) {
        if (checkSet.contains(parameter) || parameter.isPrimitive() || parameter.isEnum()
                || parameter.getCanonicalName().startsWith("java.")) {
            return;
        }
        //Bean
        checkSet.add(parameter);
        List<Field> fieldList = ClassUtils.getFields(parameter);
        if (builder.lastIndexOf(",") != builder.length() - 1) {
            builder.append(",");
        }
        builder.append("\"").append(parameter.getSimpleName()).append("\":{");
        boolean isAdd = false;
        for (Field field : fieldList) {
            Class<?> fieldClass = field.getType();
            Type fType = field.getGenericType();
            if (isAdd && builder.lastIndexOf(",") != builder.length() - 1) {
                builder.append(",");
            }
            if (!isAdd) {
                isAdd = true;
            }
            if (fType instanceof ParameterizedType) {
                ParameterizedType pfType = (ParameterizedType) fType;
                String typeStr = pfType.toString();
                builder.append("\"" + field.getName() + "\":").append("\"" + typeStr + "\",");
                getGenericField(typeStr, builder, checkSet);
            } else {
                if (fieldClass.isPrimitive() || fieldClass.isEnum() || fieldClass.getCanonicalName().startsWith("java.")) {
                    builder.append("\"" + field.getName() + "\":").append("\"" + fieldClass.getCanonicalName() + "\",");
                    continue;
                }
                //自包类忽略
                if (fieldClass.equals(parameter)) {
                    builder.append("\"" + field.getName() + "\":").append("\"" + fieldClass.getCanonicalName() + "\",");
                    continue;
                }
                if (fieldClass.isArray()) {
                    builder.append("\"" + field.getName() + "\":").append("\"" + fieldClass.getCanonicalName() + "\",");
                    Class comClass = fieldClass.getComponentType();
                    while (comClass.isArray()) {
                        comClass = comClass.getComponentType();
                    }
                    scanParameter(comClass, checkSet, builder);
                    continue;
                }
                scanParameter(fieldClass, checkSet, builder);
            }
        }
        if (builder.lastIndexOf(",") == builder.length() - 1) {
            builder.deleteCharAt(builder.length() - 1);
        }
        builder.append("}");
    }

    //	//取得泛型信息
    public static void getGenericField(String typeStr, StringBuilder builder, Set<Class<?>> checkSet) {
        if (typeStr.indexOf("<") > 0) {
            String types[] = typeStr.split("<|,|>");
            for (String type : types) {
                try {
                    type = type.trim();
                    if (type.indexOf("<") > 0) {
                        type = type.substring(0, type.indexOf("<"));
                    }
                    if (void.class.getCanonicalName().equals(type) || type.startsWith("java.") || type.length() < 3) {
                        continue;
                    }
                    Class genericClass = Class.forName(type);
                    scanParameter(genericClass, checkSet, builder);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        } else {
            try {
                if (void.class.getCanonicalName().equals(typeStr) || typeStr.startsWith("java.")) {
                    return;
                }
                Class genericClass = Class.forName(typeStr);
                scanParameter(genericClass, checkSet, builder);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

}
