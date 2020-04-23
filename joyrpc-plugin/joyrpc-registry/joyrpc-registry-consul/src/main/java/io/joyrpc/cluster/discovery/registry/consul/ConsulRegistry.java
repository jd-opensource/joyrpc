package io.joyrpc.cluster.discovery.registry.consul;

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

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.ConsulRawClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.Member;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.agent.model.Self;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import com.ecwid.consul.v1.health.model.HealthService;
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
import io.joyrpc.util.Futures;
import io.joyrpc.util.StringUtils;
import io.joyrpc.util.SystemClock;
import io.joyrpc.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.joyrpc.util.Timer.timer;

public class ConsulRegistry extends AbstractRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ConsulRegistry.class);
    /**
     * 服务ID函数
     */
    protected static final Function<URL, String> serviceIdFunction = u -> u.getHost() + ":" + u.getPort() + "_" + GlobalContext.getPid();

    protected List<String> addresses = Collections.emptyList();

    protected boolean check;
    protected int checkInterval;
    protected int checkTimeout;

    public ConsulRegistry(String name, URL url, Backup backup) {
        super(name, url, backup);
        String address = url.getString(Constants.ADDRESS_OPTION);
        if (StringUtils.isNotBlank(address)) {
            this.addresses = Stream.of(address.split(",")).distinct()
                    .collect(Collectors.toList());
        }
        check = url.getBoolean("consul.check", true);
        checkInterval = url.getPositive("consul.checkInterval", 10000) + ThreadLocalRandom.current().nextInt(10000);
        checkTimeout = url.getPositive("consul.checkTimeout", 1000);
    }

    @Override
    protected RegistryController<? extends AbstractRegistry> create() {
        return new ConsulRegistryController(this);
    }

    /**
     * 随机获取地址
     *
     * @return
     */
    protected String randomAddress() {
        int size = addresses.size();
        switch (size) {
            case 0:
                return null;
            case 1:
                return addresses.get(0);
            default:
                return addresses.get(ThreadLocalRandom.current().nextInt(size));
        }
    }


    /**
     * Consul控制器
     */
    protected static class ConsulRegistryController extends RegistryController<ConsulRegistry> {
        /**
         * Consul可贺的
         */
        protected ConsulClient client;
        /**
         * 连续续约失败次数
         */
        protected AtomicInteger leaseErr = new AtomicInteger();
        /**
         * 续约间隔
         */
        protected int leaseInterval;
        /**
         * 续约任务名称
         */
        protected String leaseTaskName;

        public ConsulRegistryController(ConsulRegistry registry) {
            super(registry);
            this.leaseTaskName = "Lease-" + registry.registryId;
            this.leaseInterval = 5000 + ThreadLocalRandom.current().nextInt(5000);
        }

        @Override
        protected ClusterBooking createClusterBooking(URLKey key) {
            return new ConsulClusterBooking(key, this::dirty, getPublisher(key.getKey()), key.getUrl().getPath());
        }

        @Override
        protected ConfigBooking createConfigBooking(URLKey key) {
            return new ConsulConfigBooking(key, this::dirty, getPublisher(key.getKey()), key.getUrl().getPath());
        }

        @Override
        protected CompletableFuture<Void> doConnect() {
            leaseErr.set(0);
            URL url = URL.valueOf(registry.randomAddress(), "http", 8500, null);
            ConsulRawClient.Builder builder = ConsulRawClient.Builder.builder();
            this.client = new ConsulClient(builder.setHost(url.getHost()).setPort(url.getPort()).build());
            return Futures.call(future -> {
                client.getAgentSelf();
                //续约
                timer().add(new Timer.DelegateTask(leaseTaskName, SystemClock.now() + leaseInterval, this::lease));
                future.complete(null);
            });
        }

        @Override
        protected CompletableFuture<Void> doRegister(final Registion registion) {
            URL url = registion.getUrl();
            String serviceName = url.getPath();
            String ip = url.getHost();
            int port = url.getPort();
            String serviceId = serviceIdFunction.apply(url);
            //注册
            NewService service = new NewService();
            service.setId(serviceId);
            service.setMeta(filterIllegalParameters(url.getParameters()));
            service.setAddress(ip);
            service.setName(serviceName);
            service.setPort(port);
            service.setTags(Arrays.asList(port > 0 ? Constants.SIDE_PROVIDER : Constants.SIDE_CONSUMER));
            if (registry.check && port > 0) {
                NewService.Check check = new NewService.Check();
                check.setTcp(url.getAddress());
                check.setInterval(registry.checkInterval + "ms");
                check.setTimeout(registry.checkTimeout + "ms");
                check.setStatus("passing");
                service.setCheck(check);
            }
            return Futures.call(future -> {
                client.agentServiceRegister(service);
                future.complete(null);
            });
        }

        @Override
        protected CompletableFuture<Void> doDeregister(Registion registion) {
            return Futures.call(future -> {
                client.agentServiceDeregister(serviceIdFunction.apply(registion.getUrl()));
                future.complete(null);
            });
        }

        private boolean deregisterFromConsul(URL url) {
            String serviceId = serviceIdFunction.apply(url);
            if (getServiceById(serviceId, url.getPath()) != null) {
                client.agentServiceDeregister(serviceId);
                return true;
            }
            return false;
        }

        @Override
        protected CompletableFuture<Void> doSubscribe(ClusterBooking booking) {
            return Futures.call(future -> {
                ConsulClusterBooking clusterBooking = (ConsulClusterBooking) booking;
                //周期性地从服务端检查
                clusterBooking.getExecutorService().scheduleAtFixedRate(() -> {
                    List<HealthService.Service> services = lookupProvider(clusterBooking.getPath(),
                            clusterBooking.getUrl().getString(Constants.ALIAS_OPTION), true);
                    if (services == null || services.isEmpty()) {
                        //移除服务订阅
                        if (!clusterBooking.getRegisteredUrls().isEmpty()) {
                            clusterBooking.handle(new ClusterEvent(registry, null,
                                    UpdateEvent.UpdateType.UPDATE, clusterBooking.getEventVersion().incrementAndGet(),
                                    clusterBooking.getRegisteredUrls().stream()
                                            .map(url -> new ClusterEvent.ShardEvent(new Shard.DefaultShard(url), ClusterEvent.ShardEventType.DELETE))
                                            .collect(Collectors.toList())));
                        }
                    } else {
                        List<URL> urls = services.stream().map(this::getUrl)
                                .collect(Collectors.toList());
                        List<URL> notRegisteredUrls = urls.stream()
                                .filter(url -> !clusterBooking.getRegisteredUrls().contains(url))
                                .collect(Collectors.toList());

                        Function<URL, String> keyFunc = url -> url.getHost() + ":" + url.getPort();

                        Set<String> availableProviders = urls.stream().map(keyFunc).collect(Collectors.toSet());
                        //需要移除
                        List<ClusterEvent.ShardEvent> deleteEvents = clusterBooking.getRegisteredUrls().stream()
                                .filter(url -> !availableProviders.contains(keyFunc.apply(url)))
                                .peek(url -> clusterBooking.getRegisteredUrls().remove(url))
                                .map(url -> new ClusterEvent.ShardEvent(new Shard.DefaultShard(url), ClusterEvent.ShardEventType.DELETE))
                                .collect(Collectors.toList());
                        //检查是否存在更新
                        Map<String, URL> registeredUrlMap = clusterBooking.getRegisteredUrls().stream()
                                .collect(Collectors.toMap(keyFunc, url -> url));

                        List<ClusterEvent.ShardEvent> updateEvents = notRegisteredUrls.stream().peek(url -> {
                            String key = keyFunc.apply(url);
                            if (registeredUrlMap.containsKey(key)) {
                                clusterBooking.getRegisteredUrls().remove(registeredUrlMap.get(key));
                            }
                            clusterBooking.getRegisteredUrls().add(url);
                        }).map(url -> new ClusterEvent.ShardEvent(new Shard.DefaultShard(url), ClusterEvent.ShardEventType.UPDATE))
                                .collect(Collectors.toList());

                        List<ClusterEvent.ShardEvent> events = Stream.of(deleteEvents, updateEvents)
                                .flatMap(List::stream).collect(Collectors.toList());
                        if (!events.isEmpty()) {
                            clusterBooking.handle(new ClusterEvent(registry, null,
                                    UpdateEvent.UpdateType.FULL,
                                    clusterBooking.getEventVersion().incrementAndGet(), events));
                        }

                    }
                }, 0, 10, TimeUnit.SECONDS);
            });
        }

        @Override
        protected CompletableFuture<Void> doSubscribe(ConfigBooking booking) {
            return Futures.call(future -> {
                ConsulConfigBooking configBooking = (ConsulConfigBooking) booking;
                List<HealthService.Service> services = lookupProvider(configBooking.getPath(),
                        configBooking.getUrl().getString(Constants.ALIAS_OPTION), true);
                if (services == null || services.isEmpty()) {
                    configBooking.handle(new ConfigEvent(registry, null,
                            configBooking.getEventVersion().incrementAndGet(), new HashMap<>()));
                    return;
                }
                services.stream().forEach(service -> configBooking.handle(new ConfigEvent(registry, null,
                        configBooking.getEventVersion().incrementAndGet(), service.getMeta())));
            });
        }

        @Override
        protected CompletableFuture<Void> doUnsubscribe(ClusterBooking booking) {
            ConsulClusterBooking clusterBooking = ((ConsulClusterBooking) booking);
            clusterBooking.getExecutorService().shutdown();
            clusterBooking.getRegisteredUrls().stream().forEach(this::deregisterFromConsul);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected CompletableFuture<Void> doUnsubscribe(ConfigBooking booking) {
            return super.doUnsubscribe(booking);
        }

        private HealthService.Service getServiceById(String serviceId, String serviceName) {
            checkConnection();
            Response<List<HealthService>> response = this.client
                    .getHealthServices(serviceName, HealthServicesRequest.newBuilder()
                            .setPassing(true).build());
            Optional<HealthService> optional = Optional.empty();
            for (HealthService healthService : response.getValue()) {
                if (healthService.getService().getId().equals(serviceId)) {
                    optional = Optional.of(healthService);
                    break;
                }
            }
            return optional.map(HealthService::getService).orElse(null);
        }

        private List<HealthService.Service> lookupProvider(String serviceName, String alias, boolean passing) {
            checkConnection();
            return client.getHealthServices(serviceName,
                    HealthServicesRequest.newBuilder().setPassing(passing)
                            .setTag(Constants.SIDE_PROVIDER)
                            .build()).getValue().stream()
                    .map(HealthService::getService)
                    .filter(service -> service.getMeta().containsKey(Constants.ALIAS_OPTION.getName())
                            && service.getMeta().get(Constants.ALIAS_OPTION.getName()).equals(alias))
                    .collect(Collectors.toList());

        }

        private boolean checkConnection() {
            try {
                Response<List<Member>> response = client.getAgentMembers();
                return !response.getValue().isEmpty();
            } catch (Exception e) {
                if (leaseErr.incrementAndGet() >= 3) {
                    logger.error(String.format("Error occurs while lease than 3 times, caused by %s. reconnect....", e.getMessage()));
                    //先关闭连接，再重连
                    doDisconnect().whenComplete((v, t) -> {
                        if (isOpen()) {
                            reconnect(new CompletableFuture<>(), 0, registry.maxConnectRetryTimes);
                        }
                    });
                }
            }
            CompletableFuture<Void> future = new CompletableFuture<>();
            reconnect(future, 1, 10);
            //没有异常即认为重连成功
            return !future.isCompletedExceptionally();
        }

        /**
         * 续约
         */
        protected void lease() {
            //TODO 是否要加在注册中心的任务里面执行
            if (isOpen()) {
                try {
                    Response<Self> response = client.getAgentSelf();
                    if (isOpen()) {
                        if (response.getValue() == null) {
                            //先关闭连接，再重连
                            doDisconnect().whenComplete((v, t) -> {
                                if (isOpen()) {
                                    reconnect(new CompletableFuture<>(), 0, registry.maxConnectRetryTimes);
                                }
                            });
                        } else {
                            leaseErr.set(0);
                            //继续续约
                            timer().add(new Timer.DelegateTask(leaseTaskName, SystemClock.now() + leaseInterval, this::lease));
                        }
                    }
                } catch (Exception e) {
                    if (isOpen()) {
                        if (leaseErr.incrementAndGet() >= 3) {
                            logger.error(String.format("Error occurs while lease than 3 times, caused by %s. reconnect....", e.getMessage()));
                            //先关闭连接，再重连
                            doDisconnect().whenComplete((v, t) -> {
                                if (isOpen()) {
                                    reconnect(new CompletableFuture<>(), 0, registry.maxConnectRetryTimes);
                                }
                            });
                        } else {
                            //继续续约
                            timer().add(new Timer.DelegateTask(leaseTaskName, SystemClock.now() + leaseInterval, this::lease));
                        }
                    }
                }
            }
        }

        private Map<String, String> filterIllegalParameters(Map<String, String> parameters) {
            return parameters.entrySet().stream()
                    .filter(entry -> !entry.getKey().contains(".") || entry.getKey().startsWith("."))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        }


        private URL getUrl(HealthService.Service service) {
            return new URL(service.getMeta().get(Constants.PROTOCOL_KEY),
                    service.getAddress(), service.getPort(), service.getService(), service.getMeta());
        }
    }


    protected static class ConsulConfigBooking extends ConfigBooking {

        //事件版本
        private AtomicLong eventVersion = new AtomicLong();

        public ConsulConfigBooking(URLKey key, Runnable dirty, Publisher<ConfigEvent> publisher, String path) {
            super(key, dirty, publisher, path);
        }

        public AtomicLong getEventVersion() {
            return eventVersion;
        }
    }

    protected static class ConsulClusterBooking extends ClusterBooking {
        //事件版本
        private AtomicLong eventVersion = new AtomicLong();

        //已注册的服务URL
        private Set<URL> registeredUrls = Collections.synchronizedSet(new HashSet<>());

        private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

        public ConsulClusterBooking(URLKey key, Runnable dirty, Publisher<ClusterEvent> publisher, String path) {
            super(key, dirty, publisher, path);
        }

        public Set<URL> getRegisteredUrls() {
            return registeredUrls;
        }

        public AtomicLong getEventVersion() {
            return eventVersion;
        }

        public ScheduledExecutorService getExecutorService() {
            return executorService;
        }
    }


}
