package io.joyrpc.cluster.discovery.registry.nacos;

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

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import io.joyrpc.cluster.Shard;
import io.joyrpc.cluster.discovery.backup.Backup;
import io.joyrpc.cluster.discovery.registry.AbstractRegistry;
import io.joyrpc.cluster.discovery.registry.URLKey;
import io.joyrpc.cluster.event.ClusterEvent;
import io.joyrpc.cluster.event.ConfigEvent;
import io.joyrpc.codec.serialization.TypeReference;
import io.joyrpc.constants.Constants;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.event.Publisher;
import io.joyrpc.event.UpdateEvent;
import io.joyrpc.exception.SerializerException;
import io.joyrpc.extension.URL;
import io.joyrpc.util.Futures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import static com.alibaba.nacos.api.PropertyKeyConst.SERVER_ADDR;
import static io.joyrpc.Plugin.JSON;
import static io.joyrpc.constants.Constants.*;

/**
 * nacos注册中心
 */
public class NacosRegistry extends AbstractRegistry {

    private static final Logger logger = LoggerFactory.getLogger(NacosRegistry.class);

    public static final String DEFAULT_GROUP = "DEFAULT_GROUP";
    public static final String DATAID = "nacos.dataId";
    public static final String GROUP = "nacos.group";
    public static final String TIMEOUT = "nacos.timeout";

    /**
     * 构造方法
     *
     * @param name   名称
     * @param url    url
     * @param backup 备份
     */
    public NacosRegistry(String name, URL url, Backup backup) {
        super(name, url, backup);
    }

    /**
     * 标准化
     *
     * @param url url
     * @return 标准化后的URL
     */
    public URL normalize(URL url) {
        URL resUrl = super.normalize(url);
        String tagKey = url.getString(TAG_KEY_OPTION);
        return resUrl == null ? null : resUrl
                .add(SERVICE_VERSION_OPTION.getName(), url.getString(SERVICE_VERSION_OPTION))
                .add(TAG_KEY_OPTION.getName(), tagKey)
                .add(tagKey, url.getString(tagKey));
    }

    @Override
    protected RegistryController<? extends AbstractRegistry> create() {
        return new NacosRegistryController(this);
    }

    @Override
    protected Registion createRegistion(final URLKey key) {
        return new NacosRegistion(key);
    }

    /**
     * nacos控制器
     */
    protected static class NacosRegistryController extends RegistryController<NacosRegistry> {

        /**
         * 目录服务
         */
        protected NamingService namingService;
        /**
         * 配置服务
         */
        protected ConfigService configService;

        /**
         * 构造函数
         *
         * @param registry 注册中心对象
         */
        public NacosRegistryController(NacosRegistry registry) {
            super(registry);
        }

        @Override
        protected CompletableFuture<Void> doConnect() {
            try {
                Properties properties = new Properties();
                String address = registry.url.getString(ADDRESS_OPTION);
                URL url = URL.valueOf(address, "http", 8848, null);
                properties.put(SERVER_ADDR, url.getAddress());
                namingService = NacosFactory.createNamingService(properties);
                configService = NacosFactory.createConfigService(properties);
                return super.doConnect();
            } catch (Throwable e) {
                shutdown();
                return Futures.completeExceptionally(e);
            }
        }

        @Override
        protected CompletableFuture<Void> doDisconnect() {
            shutdown();
            return super.doDisconnect();
        }

        /**
         * 关闭资源
         */
        protected void shutdown() {
            //连接断开后关闭目录服务和配置服务
            //关闭时间较长，采用线程异步关闭
            final NamingService namingService = this.namingService;
            Thread thread1 = new Thread(() -> {
                if (namingService != null) {
                    try {
                        namingService.shutDown();
                    } catch (NacosException e) {
                    }
                }
            }, "shutdown-nacos-namingService");
            thread1.setDaemon(true);
            thread1.start();
            final ConfigService configService = this.configService;
            Thread thread2 = new Thread(() -> {
                if (configService != null) {
                    try {
                        configService.shutDown();
                    } catch (NacosException e) {
                    }
                }
            }, "shutdown-nacos-configService");
            thread2.setDaemon(true);
            thread2.start();
        }

        @Override
        protected ClusterBooking createClusterBooking(final URLKey key) {
            return new NacosClusterBooking(key, this::dirty, getPublisher(key.getKey()));
        }

        @Override
        protected ConfigBooking createConfigBooking(final URLKey key) {
            return new NacosConfigBooking(key, this::dirty, getPublisher(key.getKey()));
        }

        @Override
        protected CompletableFuture<Void> doRegister(final Registion registion) {
            NacosRegistion nr = (NacosRegistion) registion;
            return Futures.call(future -> {
                //内部会自动注册
                namingService.registerInstance(nr.getServiceName(), nr.getGroup(), nr.getInstance());
                future.complete(null);
            });
        }

        @Override
        protected CompletableFuture<Void> doDeregister(final Registion registion) {
            NacosRegistion nr = (NacosRegistion) registion;
            return Futures.call(future -> {
                namingService.deregisterInstance(nr.getServiceName(), nr.getGroup(), nr.getInstance());
                future.complete(null);
            });
        }

        @Override
        protected CompletableFuture<Void> doSubscribe(final ClusterBooking booking) {
            return Futures.call(future -> {
                NacosClusterBooking ncb = (NacosClusterBooking) booking;
                //订阅，内部会自动定时更新，当返回节点为空时候会自我保护
                namingService.subscribe(ncb.getServiceName(), ncb.getGroup(), ncb);
                future.complete(null);
            });
        }

        @Override
        protected CompletableFuture<Void> doUnsubscribe(final ClusterBooking booking) {
            return Futures.call(future -> {
                NacosClusterBooking ncb = (NacosClusterBooking) booking;
                namingService.unsubscribe(ncb.getServiceName(), ncb.getGroup(), ncb);
                future.complete(null);
            });
        }

        @Override
        protected CompletableFuture<Void> doSubscribe(final ConfigBooking booking) {
            return Futures.call(new Futures.Executor<Void>() {
                @Override
                public void execute(CompletableFuture<Void> future) throws Exception {
                    NacosConfigBooking ncb = (NacosConfigBooking) booking;
                    //同步调用一下
                    ncb.receiveConfigInfo(configService.getConfig(ncb.getDataId(), ncb.getGroup(), ncb.getTimeout()));
                    //监听器
                    configService.addListener(ncb.dataId, ncb.group, ncb);
                    future.complete(null);
                }

                @Override
                public void onException(Exception e) {
                    logger.error("Error occurs while subscribe config. caused by " + e.getMessage(), e);
                }
            });
        }

        @Override
        protected CompletableFuture<Void> doUnsubscribe(final ConfigBooking booking) {
            return Futures.call(future -> {
                NacosConfigBooking ncb = (NacosConfigBooking) booking;
                configService.removeListener(ncb.dataId, ncb.group, ncb);
                future.complete(null);
            });
        }
    }

    /**
     * 集群订阅
     */
    protected static class NacosClusterBooking extends ClusterBooking implements EventListener {
        /**
         * 服务名称
         */
        protected String serviceName;
        /**
         * nacos分组
         */
        protected String group;
        /**
         * 监听器
         */
        protected EventListener listener;

        public NacosClusterBooking(URLKey key, Runnable dirty, Publisher<ClusterEvent> publisher) {
            super(key, dirty, publisher);
            String name = key.getService();
            String version = url.getString(SERVICE_VERSION_OPTION);
            String group = url.getString(ALIAS_OPTION);
            this.serviceName = "providers:" + name + ":" + version + ":" + group;
            this.group = url.getString(GROUP, DEFAULT_GROUP);
        }

        /**
         * 生成服务名称
         *
         * @param url url
         * @return 服务名称
         */
        protected String createServiceName(URL url) {
            String name = url.getString(SERVICE_NAME_KEY, url.getPath());
            String version = url.getString(SERVICE_VERSION_OPTION);
            String group = url.getString(ALIAS_OPTION);
            return "providers:" + name + ":" + version + ":" + group;
        }

        /**
         * 生成分片URL
         *
         * @param defProtocol 默认协议
         * @param instance    实例
         * @return 分片URL
         */
        protected URL createShardUrl(String defProtocol, Instance instance) {
            Map<String, String> meta = instance.getMetadata();
            if (!instance.isEnabled() || !url.getString(ALIAS_OPTION).equals(meta.get(ALIAS_OPTION.getName()))) {
                return null;
            }
            String protocol = meta.remove(Constants.PROTOCOL_KEY);
            protocol = protocol == null || protocol.isEmpty() ? defProtocol : protocol;
            return new URL(protocol, instance.getIp(), instance.getPort(), this.getPath(), meta);
        }

        @Override
        public void onEvent(final Event event) {
            if (event instanceof NamingEvent) {
                NamingEvent e = (NamingEvent) event;
                String defProtocol = GlobalContext.getString(Constants.PROTOCOL_KEY);
                List<ClusterEvent.ShardEvent> shards = new LinkedList<>();
                for (Instance ins : e.getInstances()) {
                    URL shardUrl = createShardUrl(defProtocol, ins);
                    if (shardUrl != null) {
                        shards.add(new ClusterEvent.ShardEvent(new Shard.DefaultShard(shardUrl), ClusterEvent.ShardEventType.UPDATE));
                    }
                }
                handle(new ClusterEvent(this, null, UpdateEvent.UpdateType.FULL, getVersion() + 1, shards));
            }
        }

        public String getServiceName() {
            return serviceName;
        }

        public String getGroup() {
            return group;
        }
    }

    /**
     * 集群订阅
     */
    protected static class NacosConfigBooking extends ConfigBooking implements Listener {
        /**
         * 数据ID
         */
        protected String dataId;
        /**
         * 分组
         */
        protected String group;
        /**
         * 超时时间
         */
        protected long timeout;
        /**
         * 内容
         */
        protected String content;
        /**
         * 版本
         */
        protected AtomicLong version = new AtomicLong(-1);

        public NacosConfigBooking(URLKey key, Runnable dirty, Publisher<ConfigEvent> publisher) {
            super(key, dirty, publisher);
            String pre = SIDE_PROVIDER.equals(url.getString(ROLE_OPTION)) ? "providers:" : "consumers:";
            //分组和应用名称可能有特殊字符，不符合规范，可以手工配置nacos.group
            String appName = GlobalContext.getString(KEY_APPNAME);
            if (appName == null || appName.isEmpty()) {
                appName = "default";
            }
            //按照接口粒度进行配置
            this.dataId = url.getString(DATAID, pre + url.getPath());
            this.group = url.getString(GROUP, url.getString(ALIAS_OPTION));
            this.timeout = url.getPositive(TIMEOUT, 5000L);
        }

        public String getDataId() {
            return dataId;
        }

        public String getGroup() {
            return group;
        }

        public long getTimeout() {
            return timeout;
        }

        @Override
        public Executor getExecutor() {
            return null;
        }

        @Override
        public void receiveConfigInfo(String s) {
            if (s == null || s.isEmpty()) {
                handle(new ConfigEvent(this, null, version.incrementAndGet(), new HashMap<>()));
            } else if (!s.equals(content)) {
                try {
                    Map<String, String> datum = JSON.get().parseObject(s, new TypeReference<Map<String, String>>() {
                    });
                    handle(new ConfigEvent(this, null, version.incrementAndGet(), datum));
                    content = s;
                } catch (SerializerException e) {
                    logger.error("Error occurs while parsing config. caused by " + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 注册信息
     */
    protected static class NacosRegistion extends Registion {

        /**
         * 服务名称
         */
        protected String serviceName;
        /**
         * 分组
         */
        protected String group;
        /**
         * 实例对象
         */
        protected Instance instance;

        public NacosRegistion(URLKey key) {
            super(key);
            String name = key.getService();
            String version = url.getString(SERVICE_VERSION_OPTION);
            String group = url.getString(ALIAS_OPTION);
            String pre = SIDE_PROVIDER.equals(url.getString(ROLE_OPTION)) ? "providers:" : "consumers:";

            this.serviceName = pre + name + ":" + version + ":" + group;
            this.group = url.getString(GROUP, DEFAULT_GROUP);
            this.instance = createInstance(url);
        }

        /**
         * 构建实例
         *
         * @param url url
         * @return 实例
         */
        protected Instance createInstance(final URL url) {
            Instance ins = new Instance();
            ins.setIp(url.getHost());
            ins.setPort(url.getPort());
            ins.setMetadata(PARAMETER_FUNCTION.apply(url));
            return ins;
        }

        public Instance getInstance() {
            return instance;
        }

        public String getServiceName() {
            return serviceName;
        }

        public String getGroup() {
            return group;
        }
    }
}
