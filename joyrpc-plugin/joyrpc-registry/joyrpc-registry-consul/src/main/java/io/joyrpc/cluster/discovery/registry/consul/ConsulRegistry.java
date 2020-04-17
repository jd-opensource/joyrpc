package io.joyrpc.cluster.discovery.registry.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.ConsulRawClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.Member;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import com.ecwid.consul.v1.health.model.HealthService;
import io.joyrpc.cluster.Shard;
import io.joyrpc.cluster.discovery.backup.Backup;
import io.joyrpc.cluster.discovery.registry.AbstractRegistry;
import io.joyrpc.cluster.discovery.registry.URLKey;
import io.joyrpc.cluster.event.ClusterEvent;
import io.joyrpc.cluster.event.ConfigEvent;
import io.joyrpc.constants.Constants;
import io.joyrpc.event.Publisher;
import io.joyrpc.event.UpdateEvent;
import io.joyrpc.extension.URL;
import io.joyrpc.util.Futures;
import io.joyrpc.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConsulRegistry extends AbstractRegistry {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private List<String> configAddresses = Collections.emptyList();

    private final String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];

    private Function<URL, String> serviceIdFunction = u -> u.getPath() + "_" + u.getHost() + ":" + u.getPort() + "_" + this.pid;

    public ConsulRegistry(String name, URL url, Backup backup) {
        super(name, url, backup);
        String address = url.getString(Constants.ADDRESS_OPTION);
        if (StringUtils.isNotBlank(address)) {
            this.configAddresses = Stream.of(address.split(",")).distinct()
                    .collect(Collectors.toList());
        }
    }

    @Override
    protected RegistryController<? extends AbstractRegistry> create() {
        return new ConsulRegistryController(this);
    }

    private String randomAddress() {
        return configAddresses.get(Math.abs(new Random().nextInt()) % configAddresses.size());
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


    private class ConsulRegistryController extends RegistryController<ConsulRegistry> {

        private ConsulRawClient consulRawClient;

        private ConsulClient consulClient;

        public ConsulRegistryController(ConsulRegistry registry) {
            super(registry);
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
            return CompletableFuture.runAsync(() -> {
                do {
                    String address = randomAddress();
                    String[] hostPort = address.split(":");
                    this.consulRawClient = ConsulRawClient.Builder.builder()
                            .setHost(hostPort[0]).setPort(Integer.parseInt(hostPort[1]))
                            .build();
                    this.consulClient = new ConsulClient(this.consulRawClient);
                    logger.info("connected to consul {}", address);
                } while (!checkConnection());
            });
        }

        @Override
        protected CompletableFuture<Void> doDisconnect() {
            return super.doDisconnect();
        }

        @Override
        protected CompletableFuture<Void> doRegister(Registion registion) {
            return CompletableFuture.runAsync(() -> {
                checkConnection();
                URL url = registion.getUrl();
                String serviceName = url.getPath();
                String ip = url.getHost();
                int port = url.getPort();
                String serviceId = serviceIdFunction.apply(url);
                if (getServiceById(serviceId, serviceName) != null) {
                    return;
                }
                //注册
                NewService service = new NewService();
                service.setId(serviceId);
                service.setMeta(filterIllegalParameters(url.getParameters()));
                service.setAddress(ip);
                service.setName(serviceName);
                service.setPort(port);
                service.setTags(Arrays.asList(
                        port > 0 ? Constants.SIDE_PROVIDER : Constants.SIDE_CONSUMER
                ));
                if (port > 0) {
                    NewService.Check check = new NewService.Check();
                    check.setTcp(ip + ":" + port);
                    check.setInterval("10s");
                    check.setTimeout("1s");
                    check.setStatus("passing");
                    service.setCheck(check);
                }
                try {
                    consulClient.agentServiceRegister(service);
                    logger.info("register service {}", serviceName);
                } catch (Exception e) {
                    logger.warn("register service {} failed, cause: {}", serviceName, e.getMessage());
                }
            });
        }

        @Override
        protected CompletableFuture<Void> doDeregister(Registion registion) {
            checkConnection();
            logger.info("deregister {} {}", registion.getKey(),
                    deregisterFromConsul(registion.getUrl()) ? "succeed" : "failed");
            return CompletableFuture.completedFuture(null);
        }

        private boolean deregisterFromConsul(URL url) {
            String serviceId = serviceIdFunction.apply(url);
            if (getServiceById(serviceId, url.getPath()) != null) {
                consulClient.agentServiceDeregister(serviceId);
                return true;
            }
            return false;
        }

        @Override
        protected CompletableFuture<Void> doSubscribe(ClusterBooking booking) {
            return Futures.call(future -> {
                ConsulClusterBooking clusterBooking = (ConsulClusterBooking) booking;
                List<HealthService.Service> services = lookupProvider(clusterBooking.getPath(),
                        clusterBooking.getUrl().getString(Constants.ALIAS_OPTION), true);
                if (services == null || services.isEmpty()) {
                    //移除服务订阅
                    if (!clusterBooking.getRegisteredUrls().isEmpty()) {
                        clusterBooking.handle(new ClusterEvent(ConsulRegistry.this, null,
                                UpdateEvent.UpdateType.UPDATE, clusterBooking.getEventVersion().incrementAndGet(),
                                clusterBooking.getRegisteredUrls().stream()
                                        .map(url -> new ClusterEvent.ShardEvent(new Shard.DefaultShard(url), ClusterEvent.ShardEventType.DELETE))
                                        .collect(Collectors.toList())));
                    }
                } else {
                    List<URL> urls = services.stream().map(ConsulRegistry.this::getUrl)
                            .collect(Collectors.toList());
                    List<URL> notRegisteredUrls = urls.stream()
                            .filter(url -> !clusterBooking.getRegisteredUrls().contains(url))
                            .collect(Collectors.toList());
                    //检查是否存在更新
                    Map<String, URL> registeredUrlMap = clusterBooking.getRegisteredUrls().stream()
                            .collect(Collectors.toMap(url -> url.getHost() + ":" + url.getPort(), url -> url));
                    clusterBooking.handle(new ClusterEvent(ConsulRegistry.this, null,
                            UpdateEvent.UpdateType.FULL,
                            clusterBooking.getEventVersion().incrementAndGet(),
                            notRegisteredUrls.stream().peek(url -> {
                                String key = url.getHost() + ":" + url.getPort();
                                if (registeredUrlMap.containsKey(key)) {
                                    clusterBooking.getRegisteredUrls().remove(registeredUrlMap.get(key));
                                }
                            }).map(url -> new ClusterEvent.ShardEvent(new Shard.DefaultShard(url), ClusterEvent.ShardEventType.UPDATE))
                                    .collect(Collectors.toList())));
                }
            });
        }

        @Override
        protected CompletableFuture<Void> doSubscribe(ConfigBooking booking) {
            return Futures.call(future -> {
                ConsulConfigBooking configBooking = (ConsulConfigBooking) booking;
                List<HealthService.Service> services = lookupProvider(configBooking.getPath(),
                        configBooking.getUrl().getString(Constants.ALIAS_OPTION), true);
                if (services == null || services.isEmpty()) {
                    configBooking.handle(new ConfigEvent(ConsulRegistry.this, null,
                            configBooking.getEventVersion().incrementAndGet(), new HashMap<>()));
                    return;
                }
                services.stream().forEach(service -> configBooking.handle(new ConfigEvent(ConsulRegistry.this, null,
                        configBooking.getEventVersion().incrementAndGet(), service.getMeta())));
            });
        }

        @Override
        protected CompletableFuture<Void> doUnsubscribe(ClusterBooking booking) {
            return Futures.call(future -> ((ConsulClusterBooking) booking).getRegisteredUrls().stream().forEach(this::deregisterFromConsul));
        }

        @Override
        protected CompletableFuture<Void> doUnsubscribe(ConfigBooking booking) {
            return super.doUnsubscribe(booking);
        }

        private HealthService.Service getServiceById(String serviceId, String serviceName) {
            checkConnection();
            Response<List<HealthService>> response = this.consulClient
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
            return consulClient.getHealthServices(serviceName,
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
                Response<List<Member>> response = consulClient.getAgentMembers();
                return !response.getValue().isEmpty();
            } catch (Exception e) {
                logger.warn("consul registry can't connect {}", e.getMessage(), e);
            }
            CompletableFuture<Void> future = new CompletableFuture<>();
            reconnect(future, 1, 10);
            //没有异常即认为重连成功
            return !future.isCompletedExceptionally();
        }
    }

    private class ConsulConfigBooking extends ConfigBooking {

        //事件版本
        private AtomicLong eventVersion = new AtomicLong();

        public ConsulConfigBooking(URLKey key, Runnable dirty, Publisher<ConfigEvent> publisher, String path) {
            super(key, dirty, publisher, path);
        }

        public AtomicLong getEventVersion() {
            return eventVersion;
        }
    }

    private class ConsulClusterBooking extends ClusterBooking {
        //事件版本
        private AtomicLong eventVersion = new AtomicLong();

        //已注册的服务URL
        private Set<URL> registeredUrls = Collections.synchronizedSet(new HashSet<>());

        public ConsulClusterBooking(URLKey key, Runnable dirty, Publisher<ClusterEvent> publisher, String path) {
            super(key, dirty, publisher, path);
        }

        public Set<URL> getRegisteredUrls() {
            return registeredUrls;
        }

        public AtomicLong getEventVersion() {
            return eventVersion;
        }
    }


}
