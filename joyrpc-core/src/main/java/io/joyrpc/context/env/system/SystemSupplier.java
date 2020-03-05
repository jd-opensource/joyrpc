package io.joyrpc.context.env.system;

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

import com.sun.management.OperatingSystemMXBean;
import io.joyrpc.context.EnvironmentSupplier;
import io.joyrpc.context.OsType;
import io.joyrpc.extension.Extension;
import io.joyrpc.permission.BlackWhiteList;
import io.joyrpc.permission.StringBlackWhiteList;
import io.joyrpc.util.Pair;
import io.joyrpc.util.Resource;

import java.lang.management.ManagementFactory;
import java.util.*;

import static io.joyrpc.context.Environment.*;
import static io.joyrpc.context.EnvironmentSupplier.SYSTEM_ORDER;
import static io.joyrpc.util.StringUtils.SEMICOLON_COMMA_WHITESPACE;
import static io.joyrpc.util.StringUtils.split;

/**
 * 系统环境变量提供者，从环境变量和系统属性获取，支持黑白名单和变量重命名。<br/>
 * 从类路径"system_env"和"META-INF/system_env"按照顺序找到第一个资源文件进行加载
 * 黑白名单规则:以'-'开头的是黑名单,'*'代表所有<br/>
 * 重命名规则:key=key1,key2,key3,按照顺序找到第一个值
 */
@Extension(value = "system", order = SYSTEM_ORDER)
public class SystemSupplier implements EnvironmentSupplier {

    @Override
    public Map<String, String> environment() {
        List<String> names = Resource.lines(new String[]{"system_env", "META-INF/system_env"}, false);

        //黑白名单
        HashSet<String> whites = new HashSet<>();
        HashSet<String> blacks = new HashSet<>();
        HashSet<String> renames = new HashSet<>();
        //未重命名的环境变量名称
        HashSet<String> unrenames = new HashSet<>();
        //重命名规则
        List<Pair<String, String[]>> renameRules = new LinkedList<>();
        for (String name : names) {
            if (name.charAt(0) == '-') {
                //黑名单
                blacks.add(name.substring(1));
            } else {
                //判断重命名
                String source = name;
                String alias = null;
                int pos = name.indexOf('=');
                if (pos >= 0) {
                    alias = name.substring(0, pos);
                    source = name.substring(pos + 1);
                }
                if (alias == null) {
                    unrenames.add(source);
                    whites.add(source);
                } else if (!alias.isEmpty()) {
                    String[] parts = split(source, SEMICOLON_COMMA_WHITESPACE);
                    renameRules.add(Pair.of(alias, parts));
                    for (String part : parts) {
                        whites.add(part);
                        renames.add(part);
                    }
                }
            }
        }

        //从系统环境获取
        Map<String, String> env = System.getenv();
        Map<String, String> result = new HashMap<>(env.size());
        //保存重命名的变量
        Map<String, String> candidates = new HashMap<>(env.size());
        BlackWhiteList<String> bwl = new StringBlackWhiteList(whites, blacks);
        for (Map.Entry<String, String> entry : env.entrySet()) {
            if (bwl.isValid(entry.getKey())) {
                if (renames.contains(entry.getKey())) {
                    if (unrenames.contains(entry.getKey())) {
                        result.putIfAbsent(entry.getKey(), entry.getValue());
                    }
                    candidates.put(entry.getKey(), entry.getValue());
                } else {
                    result.putIfAbsent(entry.getKey(), entry.getValue());
                }
            }
        }
        //从系统属性和jvm参数获取
        Properties properties = System.getProperties();
        properties.forEach((k, v) -> {
            String key = k.toString();
            if (bwl.isValid(key)) {
                String value = v.toString();
                if (renames.contains(key)) {
                    if (unrenames.contains(key)) {
                        result.putIfAbsent(key, value);
                    }
                    candidates.put(key, value);
                } else {
                    result.putIfAbsent(key, value);
                }
            }
        });

        //遍历重命名
        String value;
        for (Pair<String, String[]> rule : renameRules) {
            for (String part : rule.getValue()) {
                value = candidates.get(part);
                if (value != null) {
                    if (result.putIfAbsent(rule.getKey(), value) != null) {
                        break;
                    }
                }
            }
        }
        //从系统运行时获取
        result.putIfAbsent(MEMORY, String.valueOf(getMemory()));
        result.putIfAbsent(CPU_CORES, String.valueOf(getCpuCores()));
        result.put(PID, String.valueOf(getPid()));
        result.put(START_TIME, String.valueOf(getStartTime()));
        result.put(OS_TYPE, getOsType().toString());

        return result;
    }

    /**
     * 获取进程ID
     *
     * @return 进程ID
     */
    protected long getPid() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        int indexOf = name.indexOf('@');
        if (indexOf > 0) {
            name = name.substring(0, indexOf);
        }
        return Long.parseLong(name);
    }

    /**
     * 获取进程启动时间
     *
     * @return 进程启动时间
     */
    protected long getStartTime() {
        return ManagementFactory.getRuntimeMXBean().getStartTime();
    }

    /**
     * 获取内存大小
     *
     * @return 内存大小
     */
    protected long getMemory() {
        OperatingSystemMXBean osmxb = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        return osmxb.getTotalPhysicalMemorySize();
    }

    /**
     * 获取操作系统版本
     *
     * @return
     */
    protected OsType getOsType() {
        return OsType.detect(System.getProperty(OS_NAME));
    }

    /**
     * 获取CPU核数
     *
     * @return
     */
    protected int getCpuCores() {
        return Runtime.getRuntime().availableProcessors();
    }
}
