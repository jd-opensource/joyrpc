package io.joyrpc.cluster.discovery.registry.memory;

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

import io.joyrpc.cluster.Region;
import io.joyrpc.cluster.Shard;
import io.joyrpc.cluster.discovery.backup.Backup;
import io.joyrpc.cluster.discovery.config.ConfigHandler;
import io.joyrpc.cluster.discovery.naming.ClusterHandler;
import io.joyrpc.cluster.discovery.registry.AbstractRegistry;
import io.joyrpc.cluster.discovery.registry.URLKey;
import io.joyrpc.cluster.event.ClusterEvent;
import io.joyrpc.cluster.event.ClusterEvent.ShardEvent;
import io.joyrpc.cluster.event.ClusterEvent.ShardEventType;
import io.joyrpc.cluster.event.ConfigEvent;
import io.joyrpc.context.Environment;
import io.joyrpc.event.UpdateEvent.UpdateType;
import io.joyrpc.extension.URL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BiFunction;

import static io.joyrpc.Plugin.ENVIRONMENT;

/**
 * 内存注册中心，便于测试
 */
public class MemoryRegistry extends AbstractRegistry {

    protected Region region;

    /**
     * 配置地址
     */
    protected Map<String, AtomicReference<Config>> configDatum = new ConcurrentHashMap<>();

    /**
     * 构造函数
     *
     * @param url
     */
    public MemoryRegistry(URL url) {
        this(null, url, null);
    }

    /**
     * 构造函数
     *
     * @param name
     * @param url
     */
    public MemoryRegistry(String name, URL url) {
        this(name, url, null);
    }

    /**
     * 构造函数
     *
     * @param name
     * @param url
     * @param backup
     */
    public MemoryRegistry(String name, URL url, Backup backup) {
        super(name, url, backup);
        Environment environment = ENVIRONMENT.get();
        region = new DefaultRegion(environment.getString(Environment.REGION),
                environment.getString(Environment.DATA_CENTER));
    }

    @Override
    public String getRegion() {
        return region.getRegion();
    }

    @Override
    public String getDataCenter() {
        return region.getDataCenter();
    }

    @Override
    protected RegistryController<? extends AbstractRegistry> create() {
        return new MemoryController(this);
    }

    /**
     * 更新配置
     *
     * @param url
     * @param values
     */
    public void update(final URL url, final Map<String, String> values) {
        if (url == null) {
            return;
        }
        update(getConfigKey(url), values);
    }

    /**
     * 更新配置
     *
     * @param values
     */
    public void update(final String key, final Map<String, String> values) {
        if (key == null) {
            return;
        }
        AtomicReference<Config> ref = configDatum.computeIfAbsent(key, k -> new AtomicReference<>());
        Config oldDatum;
        Config newDatum;
        long version;
        Map<String, String> datum;
        while (true) {
            oldDatum = ref.get();
            version = oldDatum == null ? 0 : oldDatum.getVersion() + 1;
            datum = values == null ? new HashMap<>() : new HashMap<>(values);
            newDatum = new Config(version, datum);
            if (ref.compareAndSet(oldDatum, newDatum)) {
                ConfigBooking meta = configs.get(key);
                if (meta != null) {
                    meta.handle((new ConfigEvent(this, null, version, datum)));
                }
                return;
            }
            LockSupport.parkNanos(1);
        }
    }


    /**
     * 创建分片
     *
     * @param url
     * @return
     */
    protected Shard createShard(final URL url) {
        //TODO 目前只支持单URL
        String region = url.getString("region");
        String dataCenter = url.getString("dataCenter");
        region = region == null || region.isEmpty() ? this.region.getRegion() : region;
        dataCenter = dataCenter == null || dataCenter.isEmpty() ? this.region.getDataCenter() : dataCenter;

        return new Shard.DefaultShard(url.getAddress(), region, dataCenter, url.getProtocol(),
                url, 100, Shard.ShardState.INITIAL);
    }

    /**
     * 控制器
     */
    protected static class MemoryController extends RegistryController<MemoryRegistry> {

        /**
         * 注册的地址
         */
        protected Map<String, AtomicReference<Topology>> urls = new ConcurrentHashMap<>();

        /**
         * 构造函数
         *
         * @param registry
         */
        public MemoryController(MemoryRegistry registry) {
            super(registry);
        }

        /**
         * 原子更新
         *
         * @param url
         * @param ref
         * @param function
         * @return 最新的版本
         */
        protected long update(final URLKey url,
                              final AtomicReference<Topology> ref,
                              final BiFunction<List<URL>, URL, Boolean> function) {
            Topology oldDatum;
            Topology newDatum;
            long version;
            List<URL> urls;
            while (true) {
                oldDatum = ref.get();
                version = oldDatum == null ? 0 : oldDatum.getVersion() + 1;
                urls = oldDatum == null ? new ArrayList<>(0) : new ArrayList<>(oldDatum.urls);
                if (!function.apply(urls, url.getUrl())) {
                    return -1;
                }
                newDatum = new Topology(version, urls);
                if (ref.compareAndSet(oldDatum, newDatum)) {
                    return version;
                }
                LockSupport.parkNanos(1);
            }
        }

        @Override
        protected CompletableFuture<Void> doRegister(final URLKey url) {
            AtomicReference<Topology> ref = urls.computeIfAbsent(url.getKey(), key -> new AtomicReference<>());
            long version = update(url, ref, List::add);
            if (version >= 0) {
                ClusterBooking meta = clusters.get(url.getKey());
                if (meta != null) {
                    List<ShardEvent> shards = new ArrayList<>(1);
                    shards.add(new ShardEvent(registry.createShard(url.getUrl()), ShardEventType.ADD));
                    meta.handle((new ClusterEvent(this, null, UpdateType.UPDATE, version, shards)));
                }
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected CompletableFuture<Void> doDeregister(final URLKey url) {
            AtomicReference<Topology> ref = urls.get(url.getKey());
            if (ref != null) {
                long version = update(url, ref, List::remove);
                if (version >= 0) {
                    ClusterBooking meta = clusters.get(url.getKey());
                    if (meta != null) {
                        List<ShardEvent> shards = new ArrayList<>(1);
                        shards.add(new ShardEvent(registry.createShard(url.getUrl()), ShardEventType.DELETE));
                        meta.handle((new ClusterEvent(this, null, UpdateType.UPDATE, ref.get().getVersion(),
                                shards)));
                    }
                }
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected CompletableFuture<Void> doSubscribe(URLKey url, ClusterHandler handler) {
            ClusterBooking meta = clusters.get(urlKey.getKey());
            if (meta != null) {
                AtomicReference<Topology> ref = urls.get(urlKey.getKey());
                Topology topology = ref != null ? ref.get() : null;
                List<ShardEvent> shards = new ArrayList<>(topology == null ? 0 : topology.getUrls().size());
                if (topology != null) {
                    topology.getUrls().forEach(u -> shards.add(new ShardEvent(registry.createShard(u), ShardEventType.ADD)));
                }
                long version = topology == null ? 0 : topology.getVersion();
                meta.handle(new ClusterEvent(this, null, UpdateType.FULL, version, shards));
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected CompletableFuture<Void> doUnsubscribe(URLKey url, ClusterHandler handler) {
            return super.doUnsubscribe(url, handler);
        }

        @Override
        protected CompletableFuture<Void> doSubscribe(URLKey url, ConfigHandler handler) {
            ClusterBooking meta = clusters.get(url.getKey());
            if (meta != null) {
                AtomicReference<Topology> ref = urls.get(url.getKey());
                Topology topology = ref != null ? ref.get() : null;
                List<ShardEvent> shards = new ArrayList<>(topology == null ? 0 : topology.getUrls().size());
                if (topology != null) {
                    topology.getUrls().forEach(u -> shards.add(new ShardEvent(registry.createShard(u), ShardEventType.ADD)));
                }
                long version = topology == null ? 0 : topology.getVersion();
                meta.handle(new ClusterEvent(this, null, UpdateType.FULL, version, shards));
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected CompletableFuture<Void> doUnsubscribe(URLKey url, ConfigHandler handler) {
            return super.doUnsubscribe(url, handler);
        }
    }

    /**
     * 集群的拓扑结构数据
     */
    protected static class Topology {
        //当前版本
        protected long version;
        //URL
        protected List<URL> urls;

        /**
         * 构造函数
         *
         * @param version
         * @param urls
         */
        public Topology(long version, List<URL> urls) {
            this.version = version;
            this.urls = urls;
        }

        public long getVersion() {
            return version;
        }

        public List<URL> getUrls() {
            return urls;
        }

    }

    /**
     * 配置数据
     */
    protected static class Config {
        //当前版本
        protected long version;
        //URL
        protected Map<String, String> data;

        /**
         * 构造函数
         *
         * @param version
         * @param data
         */
        public Config(long version, Map<String, String> data) {
            this.version = version;
            this.data = data;
        }

        public long getVersion() {
            return version;
        }

        public Map<String, String> getData() {
            return data;
        }
    }
}
