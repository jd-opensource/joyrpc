package io.joyrpc.util;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;

/**
 * 资源工具类，读取资源文件
 */
public class Resource {

    public static Predicate<String> NONE_COMMENT = o -> !o.isEmpty() && o.charAt(0) != '#';

    /**
     * 读取资源文件内容，忽略不存在的文件和异常
     *
     * @param resources  资源文件
     * @param includeAll 包含所有文件
     * @return
     */
    public static List<String> lines(final String[] resources, final boolean includeAll) {
        return lines(resources, includeAll, false, NONE_COMMENT);
    }

    /**
     * 读取资源文件内容，忽略不存在的文件和异常
     *
     * @param resources         资源文件
     * @param includeAll        包含所有文件
     * @param readDuplicateFile 读重复路径的文件
     * @return
     */
    public static List<String> lines(final String[] resources, final boolean includeAll, final boolean readDuplicateFile) {
        return lines(resources, includeAll, readDuplicateFile, NONE_COMMENT);
    }

    /**
     * 读取资源文件内容，忽略不存在的文件和异常
     *
     * @param resources         资源文件
     * @param includeAll        包含所有文件
     * @param readDuplicateFile 读重复路径的文件
     * @param predicate         断言
     * @return
     */
    public static List<String> lines(final String[] resources, final boolean includeAll,
                                     final boolean readDuplicateFile, final Predicate<String> predicate) {
        List<String> result = new LinkedList<>();
        if (resources != null) {
            ClassLoader loader = ClassUtils.getCurrentClassLoader();
            for (String resource : resources) {
                if (readDuplicateFile) {
                    //读所有同路径的文件，合并重复行
                    Set<String> rs = new LinkedHashSet<>();
                    try {
                        Enumeration<URL> duplicateResources = loader.getResources(resource);
                        while ((duplicateResources.hasMoreElements())) {
                            InputStream inputStream = duplicateResources.nextElement().openStream();
                            rs.addAll(readLines(inputStream, predicate));
                        }
                        result.addAll(rs);
                    } catch (IOException e) {
                    }
                } else {
                    //同路径的文件，只读取一个
                    InputStream inputStream = loader.getResourceAsStream(resource);
                    result.addAll(readLines(inputStream, predicate));
                }
                if (!includeAll) {
                    break;
                }
            }
        }
        return result;
    }

    protected static List<String> readLines(InputStream inputStream, final Predicate<String> predicate) {
        List<String> result = new LinkedList<>();
        if (inputStream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (predicate == null || predicate.test(line)) {
                        result.add(line);
                    }
                }

            } catch (IOException e) {
            }
        }
        return result;
    }

    /**
     * 读取资源文件内容，忽略不存在的文件和异常
     *
     * @param resource 资源文件
     * @return
     */
    public static List<String> lines(final String resource) {
        return lines(new String[]{resource}, false, false, NONE_COMMENT);
    }

    /**
     * 读取资源文件内容，忽略不存在的文件和异常
     *
     * @param resource  资源文件
     * @param predicate 断言
     * @return
     */
    public static List<String> lines(final String resource, final Predicate<String> predicate) {
        return lines(new String[]{resource}, false);
    }
}
