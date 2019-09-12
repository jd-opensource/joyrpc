package io.joyrpc.transport.resteasy.deployment;

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

import io.joyrpc.config.ProviderConfig;
import io.joyrpc.transport.resteasy.server.RestServer;
import org.jboss.resteasy.plugins.server.resourcefactory.SingletonResource;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.util.GetRestful;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * restfull接口注册管理
 */
public class DeploymentManager {

    private static final Map<Integer, RestServer> REST_SERVERS = new ConcurrentHashMap<>();

    private static final Map<Integer, Set<ProviderConfig>> NOT_REGISTERED = new ConcurrentHashMap<>();


    public static void register(ProviderConfig providerConfig) {
        int port = providerConfig.getServerConfig().getPort();
        RestServer server = REST_SERVERS.get(port);
        if (server == null) {
            NOT_REGISTERED.computeIfAbsent(port, p -> new CopyOnWriteArraySet<>()).add(providerConfig);
        } else {
            doRegister(providerConfig, server);
        }
    }

    private static void doRegister(ProviderConfig providerConfig, RestServer server) {
        ResteasyDeployment deployment = server.getDeployment();
        Class restful = GetRestful.getRootResourceClass(providerConfig.getInterfaceClass());
        Object ref = providerConfig.getRef();
        if (restful != null) {
            deployment.getRegistry().addSingletonResource(ref, "/");
        } else {
            restful = GetRestful.getRootResourceClass(ref.getClass());
            if (restful == null) {
                String errorMsg = "It, or one of its interfaces must be annotated with @Path: " + providerConfig.getInterfaceClazz();
                throw new RuntimeException(errorMsg);
            }
            deployment.getRegistry().addResourceFactory(new SingletonResource(ref), "/", restful);
        }
    }

    public static void addServer(RestServer server) {
        REST_SERVERS.computeIfAbsent(server.getUrl().getPort(), port -> {
            Set<ProviderConfig> providerConfigs = NOT_REGISTERED.remove(port);
            if (providerConfigs != null && !providerConfigs.isEmpty()) {
                providerConfigs.forEach((providerConfig) -> {
                    doRegister(providerConfig, server);
                });
            }
            return server;
        });
    }

    public static void removeServer(int port) {
        REST_SERVERS.remove(port);
    }
}
