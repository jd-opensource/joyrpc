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

import io.joyrpc.cluster.discovery.naming.fix.FixRegistar;
import io.joyrpc.cluster.discovery.registry.AbstractRegistry;
import io.joyrpc.cluster.event.ConfigEvent;
import io.joyrpc.extension.URL;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

/**
 * 固定注册中心
 */
public class FixRegistry extends AbstractRegistry {


    /**
     * 构造函数
     *
     * @param url url
     */
    public FixRegistry(URL url) {
        super(url);
    }

    @Override
    protected RegistryController<? extends AbstractRegistry> create() {
        return new FixController(this);
    }

    /**
     * 控制器
     */
    protected static class FixController extends RegistryController<FixRegistry> {

        /**
         * 注册中心
         */
        protected FixRegistar registar;

        /**
         * 构造函数
         *
         * @param registry 注册中心
         */
        public FixController(final FixRegistry registry) {
            super(registry);
            registar = new FixRegistar(registry.getUrl());
        }

        @Override
        protected CompletableFuture<Void> doConnect() {
            return registar.open();
        }

        @Override
        protected CompletableFuture<Void> doSubscribe(final ClusterBooking booking) {
            registar.subscribe(booking.getUrl(), booking);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected CompletableFuture<Void> doUnsubscribe(final ClusterBooking booking) {
            registar.unsubscribe(booking.getUrl(), booking);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected CompletableFuture<Void> doSubscribe(final ConfigBooking booking) {
            booking.handle(new ConfigEvent(this, null, -1, new HashMap<>()));
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected CompletableFuture<Void> doUnsubscribe(final ConfigBooking booking) {
            return CompletableFuture.completedFuture(null);
        }
    }


}
