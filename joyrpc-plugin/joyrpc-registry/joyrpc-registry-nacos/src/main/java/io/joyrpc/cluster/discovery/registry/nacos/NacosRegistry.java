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
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import io.joyrpc.cluster.Shard;
import io.joyrpc.cluster.discovery.backup.Backup;
import io.joyrpc.cluster.discovery.registry.AbstractRegistry;
import io.joyrpc.cluster.discovery.registry.URLKey;
import io.joyrpc.cluster.event.ClusterEvent;
import io.joyrpc.cluster.event.ConfigEvent;
import io.joyrpc.constants.Constants;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.event.Publisher;
import io.joyrpc.event.UpdateEvent;
import io.joyrpc.extension.URL;
import io.joyrpc.extension.URLOption;
import io.joyrpc.util.Futures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.alibaba.nacos.api.PropertyKeyConst.SERVER_ADDR;
import static com.alibaba.nacos.api.common.Constants.DEFAULT_GROUP;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.util.StringUtils.split;

/**
 * nacos注册中心
 */
public class NacosRegistry extends AbstractRegistry {

    private static final Logger logger = LoggerFactory.getLogger(NacosRegistry.class);

    public static final URLOption<String> NACOS_GROUP_OPTION = new URLOption<>("nacos.group", DEFAULT_GROUP);

    /**
     * nacos的服务注册服务
     */
    private NamingService namingService;

    /**
     * namingService的初始化配置
     */
    private Properties properties;

    /**
     * 服务分组 //TODO 暂时没用
     */
    private String group;


    /**
     * 构造方法
     *
     * @param name
     * @param url
     * @param backup
     */
    public NacosRegistry(String name, URL url, Backup backup) {
        super(name, url, backup);
        this.group = url.getString(NACOS_GROUP_OPTION);
        this.properties = initProperties(url);
    }

    /**
     * 初始化namingService配置
     *
     * @param url
     * @return
     */
    protected Properties initProperties(URL url) {
        Properties properties = new Properties();
        properties.put(SERVER_ADDR, url.getString(Constants.ADDRESS_OPTION));
        return properties;
    }

    /**
     * 标准化
     *
     * @param url
     * @return
     */
    public URL normalize(URL url) {
        URL resUrl = super.normalize(url);
        String tagKey = url.getString(TAG_KEY_OPTION);
        return resUrl == null ? null : resUrl
                .add(SERVICE_VERSION_OPTION.getName(), url.getString(SERVICE_VERSION_OPTION))
                .add(tagKey, url.getString(tagKey));
    }

    @Override
    protected void doOpen() {
        try {
            namingService = NacosFactory.createNamingService(properties);
        } catch (NacosException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getErrMsg(), e);
            }
            throw new IllegalStateException(e);
        }
        super.doOpen();
    }

    @Override
    protected void doClose() {
        super.doClose();
    }

    @Override
    protected RegistryController<? extends AbstractRegistry> create() {
        return new NacosRegistryController(this);
    }

    @Override
    protected Registion createRegistion(final URL url, final String key) {
        return new NacosRegistion(url, key);
    }


    /**
     * nacos控制器
     */
    protected static class NacosRegistryController extends RegistryController<NacosRegistry> {

        /**
         * 构造函数
         *
         * @param registry 注册中心对象
         */
        public NacosRegistryController(NacosRegistry registry) {
            super(registry);
        }

        @Override
        protected ClusterBooking createClusterBooking(final URLKey key) {
            return new NacosClusterBooking(key, this::dirty, getPublisher(key.getKey()));
        }

        @Override
        protected CompletableFuture<Void> doRegister(Registion registion) {
            NacosRegistion nacosRegistion = (NacosRegistion) registion;
            String serviceName = nacosRegistion.getServiceName();
            Instance instance = nacosRegistion.getInstance();
            return Futures.call(future -> {
                registry.namingService.registerInstance(serviceName, instance);
                future.complete(null);
            });
        }

        @Override
        protected CompletableFuture<Void> doDeregister(Registion registion) {
            NacosRegistion nacosRegistion = (NacosRegistion) registion;
            String serviceName = nacosRegistion.getServiceName();
            Instance instance = nacosRegistion.getInstance();
            return Futures.call(future -> {
                registry.namingService.deregisterInstance(serviceName, instance.getIp(), instance.getPort());
                future.complete(null);
            });
        }

        @Override
        protected CompletableFuture<Void> doSubscribe(ClusterBooking booking) {
            return Futures.call(future -> {
                //TODO 此处应该定时lookup
                doUpdate((NacosClusterBooking) booking);
                future.complete(null);
            });
        }

        @Override
        protected CompletableFuture<Void> doUnsubscribe(ClusterBooking booking) {
            return Futures.call(future -> {
                NacosClusterBooking ncBooking = (NacosClusterBooking) booking;
                EventListener listener = ncBooking.removeListener();
                if (listener != null) {
                    registry.namingService.unsubscribe(ncBooking.getServiceName(), listener);
                }
                future.complete(null);
            });
        }

        @Override
        protected CompletableFuture<Void> doSubscribe(ConfigBooking booking) {
            return Futures.call(future -> {
                //TODO 暂时没有实现 配置订阅
                booking.handle(new ConfigEvent(registry, null, 0, new HashMap<>()));
            });
        }

        @Override
        protected CompletableFuture<Void> doUnsubscribe(ConfigBooking booking) {
            return super.doUnsubscribe(booking);
        }

        /**
         * 更新集群
         *
         * @param ncBooking 订阅
         */
        protected void doUpdate(final NacosClusterBooking ncBooking) throws NacosException {
            //查询所有实例
            List<Instance> instances = registry.namingService.getAllInstances(ncBooking.getServiceName());
            //触发集群事件
            doUpdate(ncBooking, instances);
            //创建listener
            EventListener listener = event -> {
                if (event instanceof NamingEvent) {
                    NamingEvent e = (NamingEvent) event;
                    doUpdate(ncBooking, e.getInstances());
                }
            };
            ncBooking.setListener(listener);
            //订阅
            registry.namingService.subscribe(ncBooking.getServiceName(), listener);
        }

        /**
         * 更新集群
         *
         * @param ncBooking 订阅
         * @param instances 实例
         */
        protected void doUpdate(final NacosClusterBooking ncBooking, List<Instance> instances) {
            String defProtocol = GlobalContext.getString(Constants.PROTOCOL_KEY);
            List<ClusterEvent.ShardEvent> shards = new LinkedList<>();
            for (Instance ins : instances) {
                URL shardUrl = ncBooking.createShardUrl(defProtocol, ins);
                if (shardUrl != null) {
                    shards.add(new ClusterEvent.ShardEvent(new Shard.DefaultShard(shardUrl), ClusterEvent.ShardEventType.UPDATE));
                }
            }
            ncBooking.handle(new ClusterEvent(registry, null, UpdateEvent.UpdateType.FULL, ncBooking.getVersion() + 1, shards));
        }


    }

    /**
     * nacos内部集群订阅
     */
    protected static class NacosClusterBooking extends ClusterBooking {

        /**
         * 服务名称
         */
        protected String serviceName;
        /**
         * 订阅listener
         */
        protected EventListener listener;

        /**
         * 构造方法
         *
         * @param key
         * @param dirty
         * @param publisher
         */
        public NacosClusterBooking(URLKey key, Runnable dirty, Publisher<ClusterEvent> publisher) {
            super(key, dirty, publisher);
            this.serviceName = createServiceName(this.getUrl());
        }

        /**
         * 生成服务名称
         *
         * @param url
         * @return
         */
        protected String createServiceName(URL url) {
            String interfaceName = url.getPath();
            String version = url.getString(SERVICE_VERSION_OPTION);
            String group = url.getString(ALIAS_OPTION);
            return "providers:" + interfaceName + ":" + version + ":" + group;
        }

        /**
         * 生成分片URL
         *
         * @param defProtocol
         * @param instance
         * @return
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

        /**
         * 获取服务名称
         *
         * @return
         */
        public String getServiceName() {
            return serviceName;
        }


        public EventListener getListener() {
            return listener;
        }

        public void setListener(EventListener listener) {
            this.listener = listener;
        }

        public EventListener removeListener() {
            EventListener res = listener;
            this.listener = null;
            return res;
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
         * nacos的instance对象
         */
        protected Instance instance;

        public NacosRegistion(URL url, String key) {
            super(url, key);
            this.serviceName = createServiceName(url);
            this.instance = createInstance(url);
        }

        /**
         * 生成服务名称
         *
         * @param url
         * @return
         */
        protected String createServiceName(URL url) {
            String interfaceName = url.getPath();
            String version = url.getString(SERVICE_VERSION_OPTION);
            String group = url.getString(ALIAS_OPTION);
            String pre = SIDE_PROVIDER.equals(url.getString(ROLE_OPTION)) ? "providers:" : "consumers:";
            return pre + interfaceName + ":" + version + ":" + group;
        }

        /**
         * 生成实例对象
         *
         * @param url
         * @return
         */
        protected Instance createInstance(URL url) {
            Instance instance = new Instance();
            instance.setIp(url.getHost());
            instance.setPort(url.getPort());
            instance.setMetadata(url.getParameters());
            return instance;
        }

        /**
         * 获取实例对象
         *
         * @return
         */
        public Instance getInstance() {
            return instance;
        }

        /**
         * 获取服务名称
         *
         * @return
         */
        public String getServiceName() {
            return serviceName;
        }

    }
}
