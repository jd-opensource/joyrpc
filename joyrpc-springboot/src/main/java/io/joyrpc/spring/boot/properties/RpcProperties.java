package io.joyrpc.spring.boot.properties;

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

import io.joyrpc.spring.ConsumerBean;
import io.joyrpc.spring.ProviderBean;
import io.joyrpc.spring.RegistryBean;
import io.joyrpc.spring.ServerBean;

import java.util.List;
import java.util.Set;

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

/**
 * @description:
 */
public class RpcProperties {

    private Set<String> packages;

    private ServerBean server;

    private RegistryBean registry;

    private List<ConsumerBean> consumers;

    private List<ProviderBean> providers;

    private List<ServerBean> servers;

    private List<RegistryBean> registries;

    public Set<String> getPackages() {
        return packages;
    }

    public void setPackages(Set<String> packages) {
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

    public List<ConsumerBean> getConsumers() {
        return consumers;
    }

    public void setConsumers(List<ConsumerBean> consumers) {
        this.consumers = consumers;
    }

    public List<ProviderBean> getProviders() {
        return providers;
    }

    public void setProviders(List<ProviderBean> providers) {
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

}
