package io.joyrpc.extension;

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
import java.util.*;

/**
 * 禁用管理器
 */
public class Disable {

    protected static Set<String> PLUGIN_DISABLE_CLASS = new HashSet<String>();
    protected static Map<String, Map<String, String>> PLUGIN_DISABLE = new HashMap<String, Map<String, String>>();

    static {
        parseDisable(loadDisable());
    }

    /**
     * 是否禁用
     *
     * @param meta
     * @return
     */
    public static boolean isDisable(final ExtensionMeta meta) {
        Name name = meta.getExtension();
        Name extensible = meta.getExtensible();
        if (PLUGIN_DISABLE_CLASS.contains(name.getClazz().getName())) {
            return true;
        }
        Map<String, String> extensions = PLUGIN_DISABLE.get(extensible.getName());
        if (extensions == null) {
            return false;
        }
        String provider = extensions.get(name.getName());
        if (provider == null) {
            return false;
        }
        return provider.isEmpty() || provider.equals(meta.getProvider());
    }

    /**
     * 解析禁用
     *
     * @param disables
     */
    protected static void parseDisable(final List<String> disables) {
        for (String disable : disables) {
            disable = disable.trim();
            if (!disable.isEmpty()) {
                parseDisable(disable);
            }
        }
    }

    /**
     * 解析禁用
     *
     * @param disable
     */
    protected static void parseDisable(final String disable) {
        String[] parts;
        int pos1;
        String extensible;
        String provider;
        int pos2;
        String extension;
        Map<String, String> extensions;
        parts = disable.split("[;,]");
        for (String part : parts) {
            if (!part.isEmpty()) {
                pos1 = part.indexOf(':');
                if (pos1 < 0) {
                    //插件全路径类名
                    PLUGIN_DISABLE_CLASS.add(part);
                } else {
                    extensible = part.substring(0, pos1);
                    provider = null;
                    pos2 = part.indexOf('@', pos1 + 1);
                    if (pos2 < 0) {
                        extension = part.substring(pos1 + 1);
                    } else {
                        extension = part.substring(pos1 + 1, pos2);
                        provider = part.substring(pos2 + 1);
                    }
                    if (!extensible.isEmpty() && !extension.isEmpty()) {
                        extensions = PLUGIN_DISABLE.get(extensible);
                        if (extensions == null) {
                            extensions = new HashMap<String, String>();
                            PLUGIN_DISABLE.put(extensible, extensions);
                        }
                        extensions.put(extension, provider == null ? "" : provider);
                    }
                }
            }
        }
    }

    /**
     * 读取禁用配置
     *
     * @return
     */
    protected static List<String> loadDisable() {
        List<String> disables = new LinkedList<String>();
        //加载禁用配置
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        classLoader = classLoader == null ? ExtensionManager.class.getClassLoader() : classLoader;
        InputStream is = classLoader.getResourceAsStream("plugin.disable");
        String line;
        if (is != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            try {
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    disables.add(line);
                }
            } catch (IOException e) {
            } finally {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
        line = System.getenv("PLUGIN_DISABLE");
        if (line != null) {
            disables.add(line);
        }
        line = System.getProperty("plugin.disable");
        if (line != null) {
            disables.add(line);
        }
        return disables;
    }
}
