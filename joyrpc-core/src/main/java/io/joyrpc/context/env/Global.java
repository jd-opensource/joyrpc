package io.joyrpc.context.env;

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

import io.joyrpc.context.Environment;
import io.joyrpc.context.EnvironmentSupplier;
import io.joyrpc.context.Property;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.ExtensionPoint;
import io.joyrpc.extension.ExtensionPointLazy;
import io.joyrpc.util.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.joyrpc.util.StringUtils.SEMICOLON_COMMA_WHITESPACE;
import static io.joyrpc.util.StringUtils.split;

/**
 * 全局环境配置器，负责加载EnvironmentSupplier来获取配置项
 */
@Extension("global")
public class Global implements Environment {
    private static final Logger logger = LoggerFactory.getLogger(Global.class);
    /**
     * 环境插件
     */
    protected final static ExtensionPoint<EnvironmentSupplier, String> SYSTEM = new ExtensionPointLazy<>(EnvironmentSupplier.class);

    /**
     * 上下文
     */
    protected volatile Map<String, Property> properties;
    /**
     * 上下文
     */
    protected volatile Map<String, String> envs;

    @Override
    public void put(final String key, final Object value) {
        if (key == null) {
            return;
        }
        getProperties().put(key, new Property(key, value));
        envs = null;
    }

    @Override
    public Property getProperty(String key) {
        return getProperties().get(key);
    }

    @Override
    public Collection<Property> properties() {
        return getProperties().values();
    }

    /**
     * 延迟加载环境变量
     *
     * @return
     */
    protected Map<String, Property> getProperties() {
        if (properties == null) {
            synchronized (this) {
                if (properties == null) {
                    build();
                }
            }
        }
        return properties;
    }

    @Override
    public Map<String, String> env() {
        if (envs != null) {
            return envs;
        }
        synchronized (this) {
            if (envs == null) {
                build();
            }
        }
        return envs;
    }

    @Override
    public void print() {
        //从系统环境获取
        Map<String, String> envs = env();
        final String line = System.getProperty("line.separator", "\n");
        final StringBuilder builder = new StringBuilder(1024 * 10).append(line);
        builder.append("=====================Environment=======================");
        envs.forEach((k, v) -> builder.append(k).append('=').append(v).append(line));
        builder.append("=======================================================");
        logger.info(builder.toString());
    }

    /**
     * 构造环境变量
     */
    protected void build() {
        //从系统环境获取
        Map<String, String> envs = new ConcurrentHashMap<>(200);
        //从插件进行加载
        Iterable<EnvironmentSupplier> suppliers = SYSTEM.extensions();
        if (suppliers != null) {
            for (EnvironmentSupplier supplier : suppliers) {
                Map<String, String> env = supplier.environment();
                if (env != null && !env.isEmpty()) {
                    env.forEach((k, v) -> {
                        if (k != null && v != null) {
                            envs.put(k, v);
                        }
                    });
                }
            }
        }

        //重命名规则
        List<String> names = Resource.lines(new String[]{"META-INF/system_env", "user_env"}, true);
        for (String name : names) {
            //判断重命名
            int pos = name.indexOf('=');
            if (pos >= 0) {
                String alias = name.substring(0, pos);
                String source = name.substring(pos + 1);
                if (!alias.isEmpty()) {
                    String[] parts = split(source, SEMICOLON_COMMA_WHITESPACE);
                    for (String part : parts) {
                        String value = envs.get(part);
                        if (value != null) {
                            envs.putIfAbsent(alias, value);
                            break;
                        }
                    }
                }
            }
        }

        //构建属性值对
        Map<String, Property> result = new ConcurrentHashMap<>(envs.size());
        envs.forEach((k, v) -> result.put(k, new Property(k, v)));

        this.envs = envs;
        this.properties = result;


    }


}
