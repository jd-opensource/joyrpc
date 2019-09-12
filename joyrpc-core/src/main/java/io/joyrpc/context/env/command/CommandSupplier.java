package io.joyrpc.context.env.command;

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

import io.joyrpc.context.EnvironmentSupplier;
import io.joyrpc.extension.Extension;
import io.joyrpc.permission.BlackWhiteList;
import io.joyrpc.permission.StringBlackWhiteList;
import io.joyrpc.util.Resource;

import java.lang.management.ManagementFactory;
import java.util.*;

/**
 * 命令行参数变量
 */
@Extension(value = "command", order = EnvironmentSupplier.COMMAND_ORDER)
public class CommandSupplier implements EnvironmentSupplier {

    public static final String SYSTEM_PROPERTIES_INCLUDE_FILE = "system.properties.include";

    @Override
    public Map<String, String> environment() {

        BlackWhiteList bwl = new StringBlackWhiteList(new HashSet<>(Resource.lines(SYSTEM_PROPERTIES_INCLUDE_FILE)), null);
        //从系统属性和jvm参数获取
        Properties properties = System.getProperties();
        //从系统环境获取
        Map<String, String> result = new HashMap(properties.size());
        properties.forEach((k, v) -> {
            String key = k.toString();
            if (bwl.isValid(key)) {
                result.put(key, v.toString());
            }
        });

        //从命令行获取
        List<String> args = ManagementFactory.getRuntimeMXBean().getInputArguments();
        for (String arg : args) {
            if (arg.startsWith("--") && arg.length() > 2) {
                String optionText = arg.substring(2);
                int pos = optionText.indexOf('=');
                if (pos > 0) {
                    String optionName = optionText.substring(0, pos);
                    String optionValue = optionText.substring(pos + 1);
                    if (optionName != null && !optionName.isEmpty() && optionValue != null) {
                        result.put(optionName, optionValue);
                    }
                }
            }
        }

        return result;
    }

}
