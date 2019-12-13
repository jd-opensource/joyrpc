package io.joyrpc.cluster.discovery.registry.http;

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

import io.joyrpc.cluster.discovery.backup.file.FileBackup;
import io.joyrpc.cluster.discovery.config.ConfigHandler;
import io.joyrpc.cluster.discovery.naming.ClusterHandler;
import io.joyrpc.cluster.discovery.naming.http.HttpProvider;
import io.joyrpc.cluster.discovery.naming.http.HttpRegistrar;
import io.joyrpc.cluster.discovery.registry.Registry;
import io.joyrpc.cluster.event.ConfigEvent;
import io.joyrpc.extension.URL;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * 目录服务注册中心
 */
public class HttpRegistry implements Registry {

    /**
     * HTTP目录服务
     */
    protected HttpRegistrar registrar;
    /**
     * 区域
     */
    protected String region;
    /**
     * 数据中心
     */
    protected String dataCenter;

    public HttpRegistry() {
    }

    public HttpRegistry(final String name, final URL url, final HttpProvider httpProvider,
                        final long expireTime, final FileBackup backup,
                        final ExecutorService executorService) {
        this(new HttpRegistrar(name, url, httpProvider, expireTime, backup, executorService));
    }

    public HttpRegistry(final HttpRegistrar registrar) {
        this.registrar = registrar;
        if (registrar != null) {
            this.region = registrar.getRegion();
            this.dataCenter = registrar.getDataCenter();
        }
    }

    @Override
    public CompletableFuture<Void> open() {
        return registrar.open();
    }

    @Override
    public CompletableFuture<Void> close() {
        return registrar.close();
    }

    @Override
    public CompletableFuture<URL> register(final URL url) {
        return CompletableFuture.completedFuture(url);
    }

    @Override
    public CompletableFuture<URL> deregister(URL url, int maxRetryTimes) {
        return CompletableFuture.completedFuture(url);
    }

    @Override
    public boolean subscribe(final URL url, final ClusterHandler handler) {
        return registrar.subscribe(url, handler);
    }

    @Override
    public boolean unsubscribe(final URL url, final ClusterHandler handler) {
        return registrar.unsubscribe(url, handler);
    }

    @Override
    public String getRegion() {
        return region;
    }

    @Override
    public String getDataCenter() {
        return dataCenter;
    }

    @Override
    public URL getUrl() {
        return registrar == null ? null : registrar.getUrl();
    }

    @Override
    public boolean subscribe(URL url, ConfigHandler handler) {
        handler.handle(new ConfigEvent(this, null, -1, new HashMap<>()));
        return true;
    }

    @Override
    public boolean unsubscribe(URL url, ConfigHandler handler) {
        handler.handle(new ConfigEvent(this, null, -1, new HashMap<>()));
        return true;
    }
}
