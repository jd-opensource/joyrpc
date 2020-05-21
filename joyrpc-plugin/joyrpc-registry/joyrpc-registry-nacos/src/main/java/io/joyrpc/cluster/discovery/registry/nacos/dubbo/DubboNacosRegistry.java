package io.joyrpc.cluster.discovery.registry.nacos.dubbo;

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

import com.alibaba.nacos.api.naming.pojo.Instance;
import io.joyrpc.cluster.discovery.backup.Backup;
import io.joyrpc.cluster.discovery.registry.AbstractRegistry;
import io.joyrpc.cluster.discovery.registry.URLKey;
import io.joyrpc.cluster.discovery.registry.nacos.NacosRegistry;
import io.joyrpc.cluster.event.ClusterEvent;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.event.Publisher;
import io.joyrpc.extension.MapParametric;
import io.joyrpc.extension.Parametric;
import io.joyrpc.extension.URL;
import io.joyrpc.util.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.constants.Constants.ROLE_OPTION;

/**
 * nacos注册中心
 */
public class DubboNacosRegistry extends NacosRegistry {

    private static final Logger logger = LoggerFactory.getLogger(DubboNacosRegistry.class);

    private final static String DUBBO_PROTOCOL_KEY = "protocol";
    private final static String DUBBO_PROTOCOL_VERSION_KEY = "dubbo";
    private final static String DUBBO_SERVICE_VERSION_KEY = "version";
    private final static String DUBBO_SERVICE_REVERSION_KEY = "revision";
    private final static String DUBBO_GROUP_KEY = "group";
    private final static String DUBBO_PATH_KEY = "path";
    private final static String DUBBO_INTERFACE_KEY = "interface";
    private final static String DUBBO_APPLICATION_KEY = "application";
    private final static String DUBBO_TIMESTAMP_KEY = "timestamp";
    private final static String DUBBO_GENERIC_KEY = "generic";
    private final static String DUBBO_PID_KEY = "pid";
    private final static String DUBBO_DEFAULT_KEY = "default";
    private final static String DUBBO_DYNAMIC_KEY = "dynamic";
    private final static String DUBBO_CATEGORY_KEY = "category";
    private final static String DUBBO_ANYHOST_KEY = "anyhost";
    private final static String DUBBO_RELEASE_KEY = "release";

    private final static String DUBBO_RELEASE_VALUE = "2.7.5";
    private final static String DUBBO_PROTOCOL_VALUE = "dubbo";
    private final static String DUBBO_PROTOCOL_VERSION_VALUE = "2.0.2";
    private final static String DUBBO_CATEGORY_PROVIDERS = "providers";
    private final static String DUBBO_CATEGORY_CONSUMERS = "consumers";


    public DubboNacosRegistry(String name, URL url, Backup backup) {
        super(name, url, backup);
    }

    @Override
    protected RegistryController<? extends AbstractRegistry> create() {
        return new DubboNacosRegistryController(this);
    }

    @Override
    protected Registion createRegistion(final URL url, final String key) {
        return new DubboNacosRegistion(url, key);
    }

    /**
     * dubbo nacos控制器
     */
    protected static class DubboNacosRegistryController extends NacosRegistryController {

        /**
         * 构造函数
         *
         * @param registry 注册中心对象
         */
        public DubboNacosRegistryController(DubboNacosRegistry registry) {
            super(registry);
        }

        @Override
        protected ClusterBooking createClusterBooking(URLKey key) {
            return new DubboNacosClusterBooking(key, this::dirty, getPublisher(key.getKey()));
        }
    }

    /**
     * dubbo nacos内部集群订阅
     */
    protected static class DubboNacosClusterBooking extends NacosClusterBooking {

        /**
         * 构造方法
         *
         * @param key
         * @param dirty
         * @param publisher
         */
        public DubboNacosClusterBooking(URLKey key, Runnable dirty, Publisher<ClusterEvent> publisher) {
            super(key, dirty, publisher);
        }

        @Override
        protected URL createShardUrl(String defProtocol, Instance instance) {
            Map<String, String> meta = instance.getMetadata();
            String alias = meta.remove(DUBBO_GROUP_KEY);
            if (!instance.isEnabled() || !url.getString(ALIAS_OPTION).equals(alias)) {
                return null;
            }
            String protocol = meta.remove(DUBBO_PROTOCOL_KEY);
            protocol = protocol == null || protocol.isEmpty() ? defProtocol : protocol;
            String ifaceName = meta.remove(DUBBO_PATH_KEY);
            String serviceVersion = meta.remove(DUBBO_SERVICE_VERSION_KEY);
            //重置alias
            meta.put(ALIAS_OPTION.getName(), alias);
            //重置service.version
            meta.put(SERVICE_VERSION_OPTION.getName(), serviceVersion);
            //创建URL
            return new URL(protocol, instance.getIp(), instance.getPort(), ifaceName, meta);
        }
    }

    /**
     * 注册信息
     */
    protected static class DubboNacosRegistion extends NacosRegistion {

        /**
         * 构造方法
         *
         * @param url
         * @param key
         */
        public DubboNacosRegistion(URL url, String key) {
            super(url, key);
        }

        @Override
        protected Instance createInstance(URL url) {
            Parametric context = new MapParametric(GlobalContext.getContext());
            //metadata
            Map<String, String> meta = new HashMap<>();
            String side = url.getString(ROLE_OPTION);
            meta.put(ROLE_OPTION.getName(), side);
            meta.put(DUBBO_RELEASE_KEY, DUBBO_RELEASE_VALUE);
            meta.put(DUBBO_PROTOCOL_VERSION_KEY, DUBBO_PROTOCOL_VERSION_VALUE);
            meta.put(DUBBO_PID_KEY, context.getString(KEY_PID));
            meta.put(DUBBO_INTERFACE_KEY, url.getPath());
            meta.put(DUBBO_SERVICE_VERSION_KEY, url.getString(SERVICE_VERSION_OPTION));
            meta.put(DUBBO_GENERIC_KEY, String.valueOf(url.getBoolean(GENERIC_OPTION)));
            meta.put(DUBBO_SERVICE_REVERSION_KEY, url.getString(SERVICE_VERSION_OPTION));
            meta.put(DUBBO_PATH_KEY, url.getPath());
            meta.put(DUBBO_DEFAULT_KEY, "true");
            meta.put(DUBBO_PROTOCOL_KEY, DUBBO_PROTOCOL_VALUE);
            meta.put(DUBBO_APPLICATION_KEY, context.getString(KEY_APPNAME));
            meta.put(DUBBO_DYNAMIC_KEY, String.valueOf(url.getBoolean(DYNAMIC_OPTION)));
            meta.put(DUBBO_CATEGORY_KEY, SIDE_PROVIDER.equals(side) ? DUBBO_CATEGORY_PROVIDERS : DUBBO_CATEGORY_CONSUMERS);
            meta.put(DUBBO_ANYHOST_KEY, "true");
            meta.put(DUBBO_GROUP_KEY, url.getString(ALIAS_OPTION));
            meta.put(DUBBO_TIMESTAMP_KEY, String.valueOf(SystemClock.now()));
            //创建instace
            Instance instance = new Instance();
            instance.setIp(url.getHost());
            instance.setPort(url.getPort());
            instance.setMetadata(meta);
            return instance;
        }
    }


}
