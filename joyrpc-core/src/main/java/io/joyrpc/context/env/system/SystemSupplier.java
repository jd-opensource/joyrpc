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
import io.joyrpc.util.Resource;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static io.joyrpc.context.Environment.*;
import static io.joyrpc.context.EnvironmentSupplier.SYSTEM_ORDER;

/**
 * 系统环境变量
 */
@Extension(value = "system", order = SYSTEM_ORDER)
public class SystemSupplier implements EnvironmentSupplier {

    @Override
    public Map<String, String> environment() {
        List<String> names = Resource.lines(new String[]{"system_env", "META-INF/system_env"}, false);
        HashSet<String> whites = new HashSet<>();
        HashSet<String> blacks = new HashSet<>();
        names.forEach(o -> {
            switch (o.charAt(0)) {
                case '-':
                    blacks.add(o.substring(1));
                    break;
                case '+':
                    whites.add(o.substring(1));
                    break;
                default:
                    whites.add(o);
            }
        });

        BlackWhiteList<String> bwl = new StringBlackWhiteList(whites, blacks);
        //从系统环境获取
        Map<String, String> env = System.getenv();
        Map<String, String> result = new HashMap<>(env.size());
        for (Map.Entry<String, String> entry : env.entrySet()) {
            if (bwl.isValid(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        //从系统运行时获取
        result.put(MEMORY, String.valueOf(getMemory()));
        result.put(CPU_CORES, String.valueOf(getCpuCores()));
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
