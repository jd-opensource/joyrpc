package io.joyrpc.spring.boot;

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

import io.joyrpc.spring.*;

import java.util.List;
import java.util.Map;

/**
 * RPC配置
 */
public class RpcProperties {

    private List<String> packages;
    /**
     * 服务
     */
    private ServerBean server;
    /**
     * 注册中心
     */
    private RegistryBean registry;
    /**
     * 消费者
     */
    private List<ConsumerBean<?>> consumers;
    /**
     * 消费者
     */
    private List<ConsumerGroupBean<?>> groups;
    /**
     * 服务提供者
     */
    private List<ProviderBean<?>> providers;
    /**
     * 服务
     */
    private List<ServerBean> servers;
    /**
     * 注册中心
     */
    private List<RegistryBean> registries;
    /**
     * 全局参数
     */
    private Map<String, String> parameters;

    public List<String> getPackages() {
        return packages;
    }

    public void setPackages(List<String> packages) {
        this.packages = packages;
    }

    public ServerBean getServer() {
        return server;
    }

    public void setServer(ServerBean server) {
        this.server = server;
    }

    public RegistryBean getRegistry() {
        return registry;
    }

    public void setRegistry(RegistryBean registry) {
        this.registry = registry;
    }

    public List<ConsumerBean<?>> getConsumers() {
        return consumers;
    }

    public void setConsumers(List<ConsumerBean<?>> consumers) {
        this.consumers = consumers;
    }

    public List<ConsumerGroupBean<?>> getGroups() {
        return groups;
    }

    public void setGroups(List<ConsumerGroupBean<?>> groups) {
        this.groups = groups;
    }

    public List<ProviderBean<?>> getProviders() {
        return providers;
    }

    public void setProviders(List<ProviderBean<?>> providers) {
        this.providers = providers;
    }

    public List<ServerBean> getServers() {
        return servers;
    }

    public void setServers(List<ServerBean> servers) {
        this.servers = servers;
    }

    public List<RegistryBean> getRegistries() {
        return registries;
    }

    public void setRegistries(List<RegistryBean> registries) {
        this.registries = registries;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }
}
