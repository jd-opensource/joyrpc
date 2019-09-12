package io.joyrpc.context;

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

/**
 * 操作系统类型
 */
public enum OsType {
    LINUX,
    WINDOWS,
    SOLARIS,
    MAC,
    FREEBSD,
    OTHER;

    public static OsType detect(final String type) {
        if (type == null || type.isEmpty()) {
            return OTHER;
        } else if (type.startsWith("Linux")) {
            return LINUX;
        } else if (type.startsWith("Windows")) {
            return WINDOWS;
        } else if (type.contains("SunOS") || type.contains("Solaris")) {
            return SOLARIS;
        } else if (type.contains("Mac")) {
            return MAC;
        } else if (type.contains("FreeBSD")) {
            return FREEBSD;
        } else {
            return OTHER;
        }
    }
}
