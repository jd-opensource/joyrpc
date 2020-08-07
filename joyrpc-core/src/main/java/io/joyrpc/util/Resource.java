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
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.joyrpc.util.ClassUtils.getCurrentClassLoader;

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
        Definition[] definitions = new Definition[resources == null ? 0 : resources.length];
        if (resources != null) {
            for (int i = 0; i < resources.length; i++)
                definitions[i] = new Definition(resources[i]);
        }
        return lines(definitions, includeAll, false, NONE_COMMENT);
    }

    /**
     * 读取资源文件内容，忽略不存在的文件和异常
     *
     * @param definitions 资源
     * @param includeAll  包含所有文件
     * @return
     */
    public static List<String> lines(final Definition[] definitions, final boolean includeAll) {
        return lines(definitions, includeAll, false, NONE_COMMENT);
    }

    /**
     * 读取资源文件内容，忽略不存在的文件和异常，去除掉重复的行
     *
     * @param definitions      资源
     * @param includeAll       包含所有文件
     * @param removeDuplicated 去除重复的行
     * @return
     */
    public static List<String> lines(final Definition[] definitions, final boolean includeAll, final boolean removeDuplicated) {
        return lines(definitions, includeAll, removeDuplicated, NONE_COMMENT);
    }

    /**
     * 读取资源文件内容，忽略不存在的文件和异常，去除掉重复的行
     *
     * @param definitions      资源
     * @param includeAll       包含所有文件
     * @param removeDuplicated 去除重复的行
     * @param predicate        断言
     * @return
     */
    public static List<String> lines(final Definition[] definitions, final boolean includeAll,
                                     final boolean removeDuplicated, final Predicate<String> predicate) {
        if (removeDuplicated) {
            LinkedHashSet<String> result = lines(definitions, includeAll, predicate, new LinkedHashSet<>());
            return result.stream().collect(Collectors.toList());
        } else {
            return lines(definitions, includeAll, predicate, new LinkedList<>());
        }
    }

    /**
     * 读取资源文件内容，忽略不存在的文件和异常，去除掉重复的行
     *
     * @param definitions 资源
     * @param includeAll  包含所有文件
     * @param predicate   断言
     * @param result      集合
     * @return 集合
     */
    protected static <T extends Collection<String>> T lines(final Definition[] definitions, final boolean includeAll,
                                                            final Predicate<String> predicate, final T result) {
        if (definitions != null) {
            ClassLoader loader = getCurrentClassLoader();
            for (Definition definition : definitions) {
                if (definition.isAll()) {
                    //读所有同路径的文件
                    try {
                        Enumeration<URL> urls = loader.getResources(definition.getName());
                        while (urls.hasMoreElements()) {
                            readLines(urls.nextElement(), predicate, result);
                        }
                    } catch (IOException e) {
                    }
                } else {
                    //同路径的文件，只读取一个
                    readLines(loader.getResource(definition.getName()), predicate, result);
                }
                if (!includeAll) {
                    break;
                }
            }
        }
        return result;
    }

    /**
     * 读文件
     *
     * @param url       资源文件
     * @param predicate 断言
     * @param lines     结果
     */
    protected static void readLines(final URL url, final Predicate<String> predicate, final Collection<String> lines) {
        if (url != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (predicate == null || predicate.test(line)) {
                        lines.add(line);
                    }
                }
            } catch (IOException e) {
            }
        }
    }

    /**
     * 读取资源文件内容，忽略不存在的文件和异常
     *
     * @param resource 资源文件
     * @return
     */
    public static List<String> lines(final String resource) {
        return lines(new Definition[]{new Definition(resource)}, false, false, NONE_COMMENT);
    }

    /**
     * 读取资源文件内容，忽略不存在的文件和异常
     *
     * @param resource  资源文件
     * @param predicate 断言
     * @return
     */
    public static List<String> lines(final String resource, final Predicate<String> predicate) {
        return lines(new Definition[]{new Definition(resource)}, false, false, predicate);
    }

    /**
     * 资源定义
     */
    public static class Definition {

        /**
         * 资源名称路径
         */
        protected String name;

        /**
         * 是否包含所有
         */
        protected boolean all;

        public Definition(String name) {
            this.name = name;
        }

        public Definition(String name, boolean all) {
            this.name = name;
            this.all = all;
        }

        public String getName() {
            return name;
        }

        public boolean isAll() {
            return all;
        }
    }
}
