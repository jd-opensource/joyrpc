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
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import io.joyrpc.cluster.Shard;
import io.joyrpc.cluster.discovery.backup.Backup;
import io.joyrpc.cluster.discovery.registry.AbstractRegistry;
import io.joyrpc.cluster.event.ClusterEvent;
import io.joyrpc.cluster.event.ConfigEvent;
import io.joyrpc.constants.Constants;
import io.joyrpc.context.GlobalContext;
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
import static io.joyrpc.util.StringUtils.SEMICOLON_COMMA_WHITESPACE;
import static io.joyrpc.util.StringUtils.split;

/**
 * nacos注册中心
 */
public class NacosRegistry extends AbstractRegistry {

    private static final Logger logger = LoggerFactory.getLogger(NacosRegistry.class);

    public static final URLOption<String> NACOS_GROUP_OPTION = new URLOption<>("nacos.group", DEFAULT_GROUP);

    private NamingService namingService;

    private String group;


    public NacosRegistry(String name, URL url, Backup backup) {
        super(name, url, backup);
        this.group = url.getString(NACOS_GROUP_OPTION);
    }

    @Override
    protected void doOpen() {
        //TODO 与nacos连接的参数配置
        Properties nacosProperties = new Properties();
        nacosProperties.put(SERVER_ADDR, url.getString(Constants.ADDRESS_OPTION));
        try {
            namingService = NacosFactory.createNamingService(nacosProperties);
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
                doUpdate(booking);
                future.complete(null);
            });
        }

        @Override
        protected CompletableFuture<Void> doUnsubscribe(ClusterBooking booking) {
            return super.doUnsubscribe(booking);
        }

        @Override
        protected CompletableFuture<Void> doSubscribe(ConfigBooking booking) {
            return Futures.call(future -> {
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
         * @param booking 订阅
         */
        protected void doUpdate(final ClusterBooking booking) throws NacosException {
            //获取要订阅的serviceName
            URL url = booking.getUrl();
            String interfaceName = url.getPath();
            String version = url.getString(SERVICE_VERSION_OPTION);
            String group = url.getString(ALIAS_OPTION);
            String serviceName = "providers:" + interfaceName + ":" + version + ":" + group;
            //查询所有实例
            List<Instance> instances = registry.namingService.getAllInstances(serviceName);
            //触发集群事件
            doUpdate(booking, instances);
            //订阅
            registry.namingService.subscribe(serviceName, event -> {
                if (event instanceof NamingEvent) {
                    NamingEvent e = (NamingEvent) event;
                    doUpdate(booking, e.getInstances());
                }
            });
        }

        /**
         * 更新集群
         *
         * @param booking   订阅
         * @param instances 实例
         */
        protected void doUpdate(final ClusterBooking booking, List<Instance> instances) {
            String defProtocol = GlobalContext.getString(Constants.PROTOCOL_KEY);
            List<ClusterEvent.ShardEvent> shards = new LinkedList<>();
            for (Instance ins : instances) {
                if (!ins.isEnabled()) {
                    continue;
                }
                Map<String, String> meta = ins.getMetadata();
                String protocol = meta == null ? null : meta.remove(Constants.PROTOCOL_KEY);
                protocol = protocol == null || protocol.isEmpty() ? defProtocol : protocol;
                URL providerUrl = new URL(protocol, ins.getIp(), ins.getPort(), booking.getUrl().getPath(), meta);
                shards.add(new ClusterEvent.ShardEvent(new Shard.DefaultShard(providerUrl), ClusterEvent.ShardEventType.UPDATE));
            }
            booking.handle(new ClusterEvent(registry, null, UpdateEvent.UpdateType.FULL, booking.getVersion() + 1, shards));
        }

    }

    protected static class NacosRegistion extends Registion {

        protected String serviceName;

        protected Instance instance;

        public NacosRegistion(URL url, String key) {
            super(url, key);
            this.serviceName = createServiceName(url);
            this.instance = createInstance(url);
        }

        protected String createServiceName(URL url) {
            String interfaceName = url.getPath();
            String version = url.getString(SERVICE_VERSION_OPTION);
            String group = url.getString(ALIAS_OPTION);
            String pre = SIDE_PROVIDER.equals(url.getString(ROLE_OPTION)) ? "providers:" : "consumers:";
            return pre + interfaceName + ":" + version + ":" + group;
        }

        protected Instance createInstance(URL url) {
            Instance instance = new Instance();
            instance.setIp(url.getHost());
            instance.setPort(url.getPort());
            instance.setMetadata(url.getParameters());
            return instance;
        }

        public Instance getInstance() {
            return instance;
        }

        public String getServiceName() {
            return serviceName;
        }

    }
}
