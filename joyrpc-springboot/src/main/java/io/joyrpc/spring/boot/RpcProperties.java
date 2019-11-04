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

import io.joyrpc.spring.RegistryBean;
import io.joyrpc.spring.ServerBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.joyrpc.spring.boot.RpcProperties.PREFIX;

/**
 * @description:
 */
@Component
@ConfigurationProperties(prefix = PREFIX)
public class RpcProperties {

    public final static String PREFIX = "rpc";

    private String basePackage;

    private ServerBean server;

    private RegistryBean registry;

    private List<ServerBean> servers;

    private List<RegistryBean> registries;

    public String getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
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
