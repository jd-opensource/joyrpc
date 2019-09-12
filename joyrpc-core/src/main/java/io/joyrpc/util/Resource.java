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

import io.joyrpc.extension.Predicate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

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
        return lines(resources, includeAll, NONE_COMMENT);
    }

    /**
     * 读取资源文件内容，忽略不存在的文件和异常
     *
     * @param resources  资源文件
     * @param includeAll 包含所有文件
     * @param predicate  断言
     * @return
     */
    public static List<String> lines(final String[] resources, final boolean includeAll, final Predicate<String> predicate) {

        List<String> result = new LinkedList<>();

        if (resources != null) {
            ClassLoader loader = ClassUtils.getCurrentClassLoader();
            for (String resource : resources) {
                InputStream inputStream = loader.getResourceAsStream(resource);
                if (inputStream != null) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (predicate == null || predicate.test(line)) {
                                result.add(line);
                            }
                        }
                        if (!includeAll) {
                            break;
                        }
                    } catch (IOException e) {
                    }
                }

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
        return lines(new String[]{resource}, false, NONE_COMMENT);
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
