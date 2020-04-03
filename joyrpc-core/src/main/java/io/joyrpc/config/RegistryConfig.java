package io.joyrpc.config;

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

import io.joyrpc.cluster.discovery.registry.RegistryFactory;
import io.joyrpc.config.validator.ValidatePlugin;
import io.joyrpc.constants.Constants;
import io.joyrpc.context.GlobalContext;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static io.joyrpc.constants.Constants.REGISTRY_ADDRESS_KEY;
import static io.joyrpc.constants.Constants.REGISTRY_PROTOCOL_KEY;

/**
 * 注册中心配置
 */
public class RegistryConfig extends AbstractIdConfig implements Serializable {
    // 默认注册中心
    protected static transient Supplier<RegistryConfig> DEFAULT_REGISTRY_SUPPLIER = () -> new RegistryConfig(
            GlobalContext.getString(REGISTRY_PROTOCOL_KEY),
            GlobalContext.getString(REGISTRY_ADDRESS_KEY));
    /**
     * 注册中心实现
     */
    @ValidatePlugin(extensible = RegistryFactory.class, name = "REGISTRY")
    protected String registry;
    /**
     * 地址
     */
    protected String address;
    /**
     * 调用注册中心超时时间
     */
    protected Integer timeout;
    /**
     * 是否备份
     */
    protected Boolean backupEnabled;
    /**
     * 保存到本地文件的位置，默认$HOME下
     */
    protected String backupPath;
    /**
     * 备份数量
     */
    protected Integer backupDatum;
    /**
     * 备份间隔
     */
    protected Long backupInterval;
    /**
     * 任务重试间隔
     */
    protected Long taskRetryInterval;
    /**
     * 最大连接重试次数
     */
    protected Integer maxConnectRetryTimes;
    /**
     * The Parameters. 自定义参数
     */
    protected Map<String, String> parameters;


    public RegistryConfig() {
    }

    public RegistryConfig(String registry) {
        this.registry = registry;
    }

    public RegistryConfig(String registry, String address) {
        this.registry = registry;
        this.address = address;
    }

    public String getRegistry() {
        return registry;
    }

    public void setRegistry(String registry) {
        this.registry = registry;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public void setParameter(String key, String value) {
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        parameters.put(key, value);
    }

    /**
     * Gets parameter.
     *
     * @param key the key
     * @return the value
     */
    public String getParameter(String key) {
        return parameters == null ? null : parameters.get(key);
    }

    public String getBackupPath() {
        return backupPath;
    }

    public void setBackupPath(String backupPath) {
        this.backupPath = backupPath;
    }

    @Override
    public String toString() {
        return "RegistryConfig{" +
                "id='" + id + '\'' +
                ", registry='" + registry + '\'' +
                ", address='" + address + '\'' +
                ", timeout=" + timeout +
                ", parameters=" + parameters +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RegistryConfig that = (RegistryConfig) o;
        return Objects.equals(this.id, that.id)
                && Objects.equals(this.registry, that.registry)
                && Objects.equals(this.address, that.address)
                && Objects.equals(this.timeout, that.timeout)
                && Objects.equals(this.parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, registry, address, timeout, parameters);
    }

    @Override
    protected Map<String, String> addAttribute2Map(final Map<String, String> params) {
        super.addAttribute2Map(params);
        //在ConsumerConfig里面会生成独立的registryURL，这些参数不会和ConsumerConfig里面的冲突
        addElement2Map(params, Constants.TIMEOUT_OPTION, timeout);
        addElement2Map(params, Constants.REGISTRY_BACKUP_ENABLED_OPTION, backupEnabled);
        addElement2Map(params, Constants.REGISTRY_BACKUP_PATH_OPTION, backupPath);
        addElement2Map(params, Constants.REGISTRY_BACKUP_DATUM_OPTION, backupDatum);
        addElement2Map(params, Constants.REGISTRY_BACKUP_INTERVAL_OPTION, backupInterval);
        addElement2Map(params, Constants.REGISTRY_TASK_RETRY_INTERVAL_OPTION, taskRetryInterval);
        addElement2Map(params, Constants.REGISTRY_MAX_CONNECT_RETRY_TIMES_OPTION, maxConnectRetryTimes);
        if (null != parameters) {
            parameters.forEach((key, value) -> addElement2Map(params, key, value));
        }
        return params;

    }
}
