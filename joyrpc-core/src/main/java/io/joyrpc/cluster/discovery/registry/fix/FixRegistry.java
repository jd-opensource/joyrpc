package io.joyrpc.cluster.discovery.registry.fix;

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

import io.joyrpc.cluster.discovery.config.ConfigHandler;
import io.joyrpc.cluster.discovery.naming.ClusterHandler;
import io.joyrpc.cluster.discovery.naming.fix.FixRegistar;
import io.joyrpc.cluster.discovery.registry.AbstractRegistry;
import io.joyrpc.cluster.discovery.registry.URLKey;
import io.joyrpc.cluster.event.ConfigEvent;
import io.joyrpc.event.UpdateEvent;
import io.joyrpc.extension.URL;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

/**
 * 固定注册中心
 */
public class FixRegistry extends AbstractRegistry {

    protected FixRegistar registar;

    /**
     * 构造函数
     *
     * @param url
     */
    public FixRegistry(URL url) {
        super(url);
        registar = new FixRegistar(url);
    }

    @Override
    protected void doOpen(CompletableFuture<Void> future) {
        connected.set(true);
        registar.open().whenComplete((v, err) -> {
            if (err == null) {
                future.complete(v);
            } else {
                future.completeExceptionally(err);
            }
        });
    }

    @Override
    protected CompletableFuture<Void> doSubscribe(final URLKey url, final ClusterHandler handler) {
        registar.subscribe(url.getUrl(), handler);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected CompletableFuture<Void> doUnsubscribe(final URLKey url, final ClusterHandler handler) {
        registar.unsubscribe(url.getUrl(), handler);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean subscribe(URL url, ConfigHandler handler) {
        handler.handle(new ConfigEvent(this, null, UpdateEvent.UpdateType.FULL, -1, new HashMap<>()));
        return true;
    }

    @Override
    public boolean unsubscribe(URL url, ConfigHandler handler) {
        handler.handle(new ConfigEvent(this, null, UpdateEvent.UpdateType.FULL, -1, new HashMap<>()));
        return true;
    }

}
