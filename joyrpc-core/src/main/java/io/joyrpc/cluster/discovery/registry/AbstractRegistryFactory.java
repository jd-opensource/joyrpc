package io.joyrpc.cluster.discovery.registry;

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

import io.joyrpc.cluster.discovery.backup.Backup;
import io.joyrpc.cluster.discovery.backup.file.FileBackup;
import io.joyrpc.constants.Constants;
import io.joyrpc.exception.InitializationException;
import io.joyrpc.extension.URL;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static io.joyrpc.constants.Constants.*;

/**
 * 抽象注册中心工厂类
 */
public abstract class AbstractRegistryFactory implements RegistryFactory {

    protected Map<String, Registry> registries = new ConcurrentHashMap<>();

    protected static final Function<URL, String> REGISTRY_KEY_FUNC = o -> o.toString(false, true, Constants.ADDRESS_OPTION.getName());

    @Override
    public Registry getRegistry(URL url) {
        return getRegistry(url, REGISTRY_KEY_FUNC);
    }

    @Override
    public Registry getRegistry(final URL url, final Function<URL, String> function) {
        if (url == null) {
            throw new InitializationException("url can not be null.");
        }
        Function<URL, String> keyFunc = function == null ? KEY_FUNC : function;
        return registries.computeIfAbsent(keyFunc.apply(url), o -> createRegistry(url));
    }

    /**
     * 创建注册中心
     *
     * @param url
     * @return
     */
    protected Registry createRegistry(final URL url) {
        // 创建注册中心实例
        try {
            String name = url.getString(REGISTRY_NAME_KEY, url.getProtocol());
            Backup backup = null;
            //判断是否开启备份
            boolean enabled = url.getBoolean(REGISTRY_BACKUP_ENABLED_OPTION);
            if (enabled) {
                //改注册中心设置的备份路径
                String path = url.getString(REGISTRY_BACKUP_PATH_OPTION);
                if (path == null || path.isEmpty()) {
                    //用户目录
                    path = System.getProperty(KEY_USER_HOME) + File.separator + "rpc_backup";
                }
                String application = url.getString(KEY_APPNAME, "no_app");
                File directory = new File(path + File.separator + name + File.separator + application + File.separator);
                backup = new FileBackup(directory, url.getInteger(REGISTRY_BACKUP_DATUM_OPTION));
            }
            return createRegistry(name, url, backup);
        } catch (IOException e) {
            throw new InitializationException("Error occurs while creating registry. caused by: ", e);
        }
    }

    /**
     * 创建注册中心
     *
     * @param name   名称
     * @param url    url
     * @param backup 备份
     * @return 注册中心
     */
    protected Registry createRegistry(String name, URL url, Backup backup) {
        return null;
    }

}
