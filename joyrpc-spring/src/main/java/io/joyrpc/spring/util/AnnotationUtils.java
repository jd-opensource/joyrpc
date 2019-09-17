package io.joyrpc.spring.util;

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

import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static java.lang.String.valueOf;
import static org.springframework.core.annotation.AnnotationUtils.getAnnotationAttributes;
import static org.springframework.core.annotation.AnnotationUtils.getDefaultValue;
import static org.springframework.util.CollectionUtils.arrayToList;
import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.StringUtils.trimWhitespace;

public class AnnotationUtils {


    /**
     * Get the {@link Annotation} attributes
     *
     * @param annotation           specified {@link Annotation}
     * @param ignoreDefaultValue   whether ignore default value or not
     * @param ignoreAttributeNames the attribute names of annotation should be ignored
     * @return non-null
     * @since 2.6.6
     */
    public static Map<String, Object> getAttributes(Annotation annotation, boolean ignoreDefaultValue,
                                                    String... ignoreAttributeNames) {
        return getAttributes(annotation, null, ignoreDefaultValue, ignoreAttributeNames);
    }

    /**
     * Get the {@link Annotation} attributes
     *
     * @param annotation           specified {@link Annotation}
     * @param propertyResolver     {@link PropertyResolver} instance, e.g {@link Environment}
     * @param ignoreDefaultValue   whether ignore default value or not
     * @param ignoreAttributeNames the attribute names of annotation should be ignored
     * @return non-null
     * @since 2.6.6
     */
    public static Map<String, Object> getAttributes(Annotation annotation, PropertyResolver propertyResolver,
                                                    boolean ignoreDefaultValue, String... ignoreAttributeNames) {

        Set<String> ignoreAttributeNamesSet = new HashSet<String>(arrayToList(ignoreAttributeNames));

        Map<String, Object> attributes = getAnnotationAttributes(annotation);

        Map<String, Object> actualAttributes = new LinkedHashMap<String, Object>();

        for (Map.Entry<String, Object> entry : attributes.entrySet()) {

            String attributeName = entry.getKey();
            Object attributeValue = entry.getValue();

            // ignore default attribute value
            if (ignoreDefaultValue && nullSafeEquals(attributeValue, getDefaultValue(annotation, attributeName))) {
                continue;
            }

            // ignore attribute name
            if (ignoreAttributeNamesSet.contains(attributeName)) {
                continue;
            }

            /**
             * @since 2.7.1
             * ignore annotation member
             */
            if (attributeValue.getClass().isAnnotation()) {
                continue;
            }
            if (attributeValue.getClass().isArray() && attributeValue.getClass().getComponentType().isAnnotation()) {
                continue;
            }

            if (attributeValue instanceof String) {
                attributeValue = resolvePlaceholders(valueOf(attributeValue), propertyResolver);
            } else if (attributeValue instanceof String[]) {
                String[] values = (String[]) attributeValue;
                for (int i = 0; i < values.length; i++) {
                    values[i] = resolvePlaceholders(values[i], propertyResolver);
                }
                attributeValue = values;
            }
            actualAttributes.put(attributeName, attributeValue);
        }
        return actualAttributes;
    }

    private static String resolvePlaceholders(String attributeValue, PropertyResolver propertyResolver) {
        String resolvedValue = attributeValue;
        if (propertyResolver != null) {
            resolvedValue = propertyResolver.resolvePlaceholders(resolvedValue);
            resolvedValue = trimWhitespace(resolvedValue);
        }
        return resolvedValue;
    }

}
