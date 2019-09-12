package io.joyrpc.extension.condition;

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

import io.joyrpc.extension.exception.PluginException;

import java.lang.annotation.Annotation;

/**
 * Java版本判断
 */
public class OnJavaCondition implements Condition {
    @Override
    public boolean match(final ClassLoader classLoader, final Class clazz, final Annotation annotation) {
        String version = System.getProperty("java.version");
        ConditionalOnJava onJava = (ConditionalOnJava) annotation;
        int javaVersion = getVersion(clazz, version);
        int targetVersion = getVersion(clazz, onJava.value());
        switch (onJava.range()) {
            case OLDER_THAN:
                return javaVersion < targetVersion;
            case EQUAL_OR_NEWER:
                return javaVersion >= targetVersion;
            default:
                return false;
        }
    }

    /**
     * 获取版本
     *
     * @param clazz
     * @param version
     * @return
     */
    protected int getVersion(final Class clazz, final String version) {
        try {
            String[] parts = version.trim().split("\\.");
            if (parts.length > 1) {
                return Integer.parseInt(parts[0]) * 1000 + Integer.parseInt(parts[1]);
            } else {
                return Integer.parseInt(parts[0]) * 1000;
            }
        } catch (Exception e) {
            throw new PluginException(clazz.getName() + ": Error parse java version: " + version);
        }
    }
}
