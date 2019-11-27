package io.joyrpc.constants;

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

import io.joyrpc.context.OsType;
import io.joyrpc.event.PublisherConfig;
import io.joyrpc.extension.URL;
import io.joyrpc.extension.URLBiOption;
import io.joyrpc.extension.URLOption;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import static io.joyrpc.Plugin.ENVIRONMENT;
import static io.joyrpc.context.Environment.OS_TYPE;

/**
 * 常量定义
 */
public class Constants {

    /*======================= configuration information item key name =======================*/
    /**
     * 配置key:interface | interfaceClazz
     */
    public static final String CONFIG_KEY_INTERFACE = "interface";

    /**
     * 默认超时时间
     */
    public static final int DEFAULT_TIMEOUT = 5000;

    /**
     * 默认高水位值
     */
    public static final int DEFAULT_HIGH_WATER_MARK = 64 * 1024;

    /**
     * 默认低水位值
     */
    public static final int DEFAULT_LOW_WATER_MARK = 32 * 1024;

    /**
     * 默认 数据包限制
     */
    public final static int DEFAULT_PAYLOAD = 8 * 1024 * 1024;

    /**
     * url method prex
     */
    public final static String URL_METHOD_PREX = "mc.";

    /**
     * url side is provider
     */
    public final static String SIDE_PROVIDER = "provider";

    /**
     * url side is consumer
     */
    public final static String SIDE_CONSUMER = "consumer";

    /**
     * url side is config
     */
    public final static String SIDE_CONFIG = "config";

    /**
     * 当前实例在注册中心的关键字
     */
    public final static String KEY_INSTANCEKEY = "instanceKey";

    /**
     * url timestamp key
     */
    public final static String TIMESTAMP_KEY = "timestamp";

    /**
     * failfast集群策略
     */
    public final static String FAIL_FAST_DISTRIBUTE = "failfast";

    /**
     * fix 注册中心
     */
    public final static String FIX_REGISTRY = "fix";

    /**
     * http
     */
    public final static String HTTP_PROTOCOL = "http";

    /**
     * 跨机房调用，首选机房
     */
    public final static String CIRCUIT_KEY = "circuit";

    /**
     * 自定义设置：Server是否开启http的keepAlive特性
     */
    public static final String SETTING_HTTP_KEEP_ALIVE = "http.keepAlive";

    /**
     * 自定义设置：序列化是否检测Object的类型（父子类检查）
     */
    public static final URLOption<Boolean> SERIALIZE_CHECK_CLASS_OPTION = new URLOption<>("serialize.check.class", Boolean.TRUE);

    public static final URLOption<Boolean> SERIALIZE_CHECK_REFERENCE_OPTION = new URLOption<>("serialize.check.reference", Boolean.TRUE);
    /**
     * junit测试时，不需要加载其它的
     */
    public final static URLOption<Boolean> UNIT_TEST_OPTION = new URLOption<>("unitTestMode", Boolean.FALSE);
    /**
     * 配置变更计数器
     */
    public final static String COUNTER = "counter";

    public final static String PROTOCOL_SEPARATOR = "://";
    public final static String PATH_SEPARATOR = "/";
    public final static String QUESTION_MARK_SEPARATOR = "?";
    public final static String COLON_SEPARATOR = ":";
    public final static String COMMA_SEPARATOR = ",";
    public final static String EQUAL_SIGN_SEPARATOR = "=";
    public final static String AND_SEPARATOR = "&";

    /**
     * 不能修改的属性
     */
    public final static Map<String, String> EXCLUDE_CHANGED_ATTR_MAP = new ConcurrentHashMap<String, String>() {{
        put("id", "");
        put("interfaceClazz", "");
        put("ref", "");
        put("server", "");
        put("delay", "");
        put("proxy", "");
        put("registry", "");
        put("generic", "");
        put("dynamic", "");
        put("register", "");
        put("subscribe", "");
        put("interfaceValidator", "");
    }};


    /*======================= Registration center configuration information item name =======================*/
    /**
     * 全局配置的key
     */
    public static final String GLOBAL_SETTING = "global_setting";
    /**
     * 全局设置：全局注册中心心跳间隔
     */
    public static final String SETTING_REGISTRY_HEARTBEAT_INTERVAL = "reg.hb.interval";
    /**
     * 全局设置：全局注册中心检测间隔
     */
    public static final String SETTING_REGISTRY_CHECK_INTERVAL = "reg.ck.interval";

    /**
     * 全局设置：全局高级操作密码
     */
    public static final String SETTING_SERVER_SUDO_PASSWD = "srv.sudo.passwd";

    /**
     * 全局设置：全局高级操作密码加密算法
     */
    public static final String SETTING_SERVER_SUDO_CRYPTO = "srv.sudo.crypto";

    /**
     * 全局设置：全局高级操作密码加密秘钥
     */
    public static final String SETTING_SERVER_SUDO_CRYPTO_KEY = "srv.sudo.crypto.key";

    /**
     * 全局设置：全局高级操作的白名单
     */
    public static final String SETTING_SERVER_SUDO_WHITELIST = "srv.sudo.whitelist";

    /**
     * 接口级设置：远程调用的token
     */
    public static final String SETTING_INVOKE_TOKEN = "invoke.token";
    /**
     * 接口级设置：是否开启黑白名单
     */
    public static final String SETTING_INVOKE_WB_OPEN = "invoke.wb.open";
    /**
     * 接口级设置：调用白名单
     */
    public static final String SETTING_INVOKE_WHITELIST = "invoke.whitelist";
    /**
     * 接口级设置：调用黑名单
     */
    public static final String SETTING_INVOKE_BLACKLIST = "invoke.blacklist";

    /**
     * 接口级设置：路由是否开启
     */
    public static final String SETTING_ROUTER_OPEN = "router.open";
    /**
     * 接口级设置：路由规则
     */
    public static final String SETTING_ROUTER_RULE = "router.rule";
    /**
     * 接口级设置: 分组路由（参数和分组的映射关系）
     */
    public static final String SETTING_MAP_PARAM_ALIAS = "map.param.alias";
    /**
     * 接口级设置：模拟的调用返回结果
     */
    public static final String SETTING_INVOKE_MOCKRESULT = "invoke.mockresult";
    /**
     * 接口级设置:provider端限流
     */
    public static final String SETTING_INVOKE_PROVIDER_LIMIT = "invoke.provider.limit";
    /**
     * 接口级设置:provider端限流
     */
    public static final String SETTING_INVOKE_CONSUMER_CIRCUITBREAKER = "invoke.consumer.circuitbreaker";

    /**
     * 自适应负载均衡
     */
    public static final String SETTING_LOADBALANCE_ADAPTIVE = "loadbalance.adaptive";
    /**
     * 自定义设置: 是否忽略Consumer变化时最终的删除命令，默认false
     */
    public static final String SETTING_CONSUMER_PROVIDER_NULLABLE = "consumer.provider.nullable";
    /**
     * 自定义设置：callback的线程池初始大小
     */
    public static final String SETTING_CALLBACK_POOL_CORE_SIZE = "callback.pool.coresize";
    /**
     * 自定义设置：callback的线程池最大大小
     */
    public static final String SETTING_CALLBACK_POOL_MAX_SIZE = "callback.pool.maxsize";
    /**
     * 自定义设置：callback的线程池队列
     */
    public static final String SETTING_CALLBACK_POOL_QUEUE = "callback.pool.queue";

    /**
     * properties 文件里默认注册中心key
     */
    public static final String REGISTRY_ADDRESS_KEY = "registry.address";
    /**
     * properties 文件里默认注册中心key
     */
    public static final String REGISTRY_PROTOCOL_KEY = "registry.protocol";
    /**
     * properties 文件里默认telnet命令prompt
     */
    public static final URLOption<String> TELNET_PROMPT_OPTION = new URLOption<>("telnet.cmd.prompt", "rpc>");
    /**
     * 上下文资源
     */
    public static final String CONTEXT_RESOURCE = "context.resource";

    public static final String PROTOCOL_KEY = "protocol";

    public static final String PROTOCOL_VERSION_KEY = "protocol.version";

    /**
     * 当前所在文件夹地址
     */
    public final static String KEY_APPAPTH = "appPath";
    /**
     * 自动部署的appId
     */
    public final static String KEY_APPID = "appId";
    /**
     * 自动部署的appName
     */
    public final static String KEY_APPNAME = "appName";
    /**
     * 自动部署的appInsId
     */
    public final static String KEY_APPINSID = "appInsId";
    /**
     * token
     */
    public final static String KEY_TOKEN = "token";

    /*======================= RPC Context Parameter Key name =======================*/
    /**
     * 内部使用的key前缀，防止和自定义key冲突
     */
    public static final char INTERNAL_KEY_PREFIX = '_';
    /**
     * 内部使用的key：自动部署appId
     */
    @Deprecated
    public static final String INTERNAL_KEY_APPID = INTERNAL_KEY_PREFIX + KEY_APPID;
    /**
     * 内部使用的key：自动部署appName
     */
    @Deprecated
    public static final String INTERNAL_KEY_APPNAME = INTERNAL_KEY_PREFIX + KEY_APPNAME;
    /**
     * 内部使用的key：自动部署实例Id
     */
    @Deprecated
    public static final String INTERNAL_KEY_APPINSID = INTERNAL_KEY_PREFIX + KEY_APPINSID;

    /**
     * 隐藏的key前缀，隐藏的key只能在filter里拿到，在RpcContext里拿不到，不过可以设置
     */
    public static final char HIDE_KEY_PREFIX = '.';
    /**
     * 隐藏属性的key：token
     */
    public static final String HIDDEN_KEY_TOKEN = HIDE_KEY_PREFIX + KEY_TOKEN;

    /**
     * 隐藏属性的key：consumer发布是否警告检查
     */
    public static final String HIDDEN_KEY_WARNNING = HIDE_KEY_PREFIX + "warnning";
    /**
     * 隐藏属性的key：consumer是否自动销毁（例如Registry和Monitor不需要自动销毁）
     */
    public static final String HIDDEN_KEY_DESTROY = HIDE_KEY_PREFIX + "destroy";
    /**
     * 隐藏属性的key：自动部署appId
     */
    public static final String HIDDEN_KEY_APPID = HIDE_KEY_PREFIX + "appId";
    /**
     * 隐藏属性的key：自动部署appName
     */
    public static final String HIDDEN_KEY_APPNAME = HIDE_KEY_PREFIX + "appName";
    /**
     * 隐藏属性的key：自动部署实例Id
     */
    public static final String HIDDEN_KEY_APPINSID = HIDE_KEY_PREFIX + "appInsId";
    /**
     * 隐藏属性的key：session
     */
    public static final String HIDDEN_KEY_SESSION = HIDE_KEY_PREFIX + "session";
    /**
     * 隐藏属性的key：目标参数
     */
    public static final String HIDDEN_KEY_DST_PARAM = HIDE_KEY_PREFIX + "dstParam";
    /**
     * 隐藏属性的key：对端的语言
     */
    public static final String HIDDEN_KEY_DST_LANGUAGE = HIDE_KEY_PREFIX + "dstLan";
    /**
     * 隐藏属性的key: 请求超时时间
     */
    public static final String HIDDEN_KEY_TIME_OUT = HIDE_KEY_PREFIX + "timeout";

    /**
     * 隐藏属性的key: 指定调用IP
     */
    public static final String HIDDEN_KEY_PINPOINT = HIDE_KEY_PREFIX + "pinpoint";
    /**
     * 定时器线程数
     */
    public static final String TIMER_THREADS = "timer.threads";
    /**
     * SERVICE_MESH的键名称
     */
    public static final String SERVICE_MESH_KEY = "service_mesh";
    /**
     * grpc header 状态key
     */
    public static final String GRPC_STATUS_KEY = "grpc-status";
    /**
     * grpc header 异常消息key
     */
    public static final String GRPC_MESSAGE_KEY = "grpc-message";

    /**
     * eventbus 默认常量
     */
    public static final String EVENT_PUBLISHER_CLIENT_NAME = "event.client";
    public static final String EVENT_PUBLISHER_SERVER_NAME = "event.server";
    public static final PublisherConfig EVENT_PUBLISHER_TRANSPORT_CONF = PublisherConfig.builder().timeout(1000).build();



    /*======================= URL prarmters type =======================*/

    public static final BiFunction<String, String, String> METHOD_KEY = (method, key) -> new StringBuilder(60).append(URL_METHOD_PREX).append(method).append(".").append(key).toString();

    /*------------------------ 通用配置 ------------------------*/
    public static final URLOption<String> FILTER_OPTION = new URLOption<>("filter", "");
    public static final URLOption<String> ADDRESS_OPTION = new URLOption<>("address", "");
    public static final URLOption<String> ALIAS_OPTION = new URLOption<>("alias", "");
    public static final URLOption<Long> START_TIME_OPTION = new URLOption<>("startTime", 0L); //provider启动时间戳
    public static final URLOption<Boolean> SERVICE_MESH_OPTION = new URLOption<>(SERVICE_MESH_KEY, false);
    /**
     * 消费者调用超时时间
     */
    public static final URLOption<Integer> TIMEOUT_OPTION = new URLOption<>("timeout", DEFAULT_TIMEOUT);
    public static final URLOption<Boolean> REGISTER_OPTION = new URLOption<>("register", true);
    public static final URLOption<Boolean> SUBSCRIBE_OPTION = new URLOption<>("subscribe", true);
    public static final URLOption<String> INTERFACE_CLAZZ_OPTION = new URLOption<>("interfaceClazz", "");
    public static final URLOption<String> URL_OPTION = new URLOption<>("url", "");
    public static final URLOption<String> INSTANCE_KEY_OPTION = new URLOption<>("insKey", "");
    public static final URLOption<String> ROLE_OPTION = new URLOption<>("side", SIDE_CONSUMER);
    public static final URLOption<Boolean> ENABLE_VALIDATOR_OPTION = new URLOption<>("enableValidator", true);
    public static final URLOption<String> INTERFACE_VALIDATOR_OPTION = new URLOption<>("interfaceValidator", "standard");
    public static final URLOption<String> WARMUP_OPTION = new URLOption<>("warmup", "standard");

    /*------------------------ consumer配置 ------------------------*/
    public static final URLOption<Boolean> GENERIC_OPTION = new URLOption<>("generic", false);
    public static final URLOption<Boolean> SYSTEM_OPTION = new URLOption<>("system.service", false);
    public static final URLOption<Boolean> ASYNC_OPTION = new URLOption<>("async", false);
    public static final URLOption<String> ROUTE_OPTION = new URLOption<>("route", "failover");
    public static final URLOption<String> FAILOVER_WHEN_THROWABLE_OPTION = new URLOption<>("failoverWhenThrowable", "");
    public static final URLOption<String> FAILOVER_PREDICATION_OPTION = new URLOption<>("failoverPredication", "");
    public static final URLOption<Boolean> FROM_GROUP_OPTION = new URLOption<>("_fromGroup", false);
    public static final URLOption<String> CHANNEL_FACTORY_OPTION = new URLOption<>("channelFactory", "shared");
    public static final URLOption<String> AUTHENTICATION_OPTION = new URLOption<>("authentication", "");

    /**
     * 优雅下线
     */
    public static final URLOption<Boolean> GRACEFULLY_SHUTDOWN_OPTION = new URLOption<>("gracefullyShutdown", Boolean.TRUE);
    /**
     * 通知客户端下线超时时间
     */
    public static final URLOption<Long> OFFLINE_TIMEOUT_OPTION = new URLOption<>("offlineTimeout", 5000L);
    /**
     * 关闭的超时时间
     */
    public static final URLOption<Long> SHUTDOWN_TIMEOUT_OPTION = new URLOption<>("shutdownTimeout", 15000L);

    /**
     * 失败重试次数
     */
    public static final URLOption<Integer> RETRIES_OPTION = new URLOption<>("retries", 0);
    /**
     * 每个节点只重试一次
     */
    public static final URLOption<Boolean> RETRY_ONLY_ONCE_PER_NODE_OPTION = new URLOption<>("retryOnlyOncePerNode", false);
    /**
     * 重试目标节点选择器
     */
    public static final URLOption<String> FAILOVER_SELECTOR_OPTION = new URLOption<>("failoverSelector", "simple");

    public static final URLOption<String> LOADBALANCE_OPTION = new URLOption<>("loadbalance", "randomWeight");
    public static final URLOption<Boolean> STICKY_OPTION = new URLOption<>("sticky", false);
    public static final URLOption<Boolean> IN_JVM_OPTION = new URLOption<>("injvm", true);
    public static final URLOption<Boolean> CHECK_OPTION = new URLOption<>("check", false);
    public static final URLOption<String> SERIALIZATION_OPTION = new URLOption<>("serialization", "hessian");
    public static final URLOption<String> PROXY_OPTION = new URLOption<>("proxy", "bytebuddy");
    public static final URLOption<Boolean> VALIDATION_OPTION = new URLOption<>("validation", false);
    public static final URLOption<String> ROUTER_OPTION = new URLOption<>("router", (String) null);
    //默认不压缩
    public static final URLOption<String> COMPRESS_OPTION = new URLOption<>("compress", (String) null);
    public static final URLOption<String> CANDIDATURE_OPTION = new URLOption<>("candidature", "region");

    /*------------------------ consumer group配置 ------------------------*/
    public static final URLOption<String> ALIAS_ADAPTIVE_OPTION = new URLOption<>("aliasAdaptive", "");
    public static final URLOption<Integer> DST_PARAM_OPTION = new URLOption<>("dstParam", (Integer) null);
    public static final URLOption<Boolean> MOCK_OPTION = new URLOption<>("mock", true);
    public static final URLOption<String> GROUP_ROUTER_OPTION = new URLOption<>("groupRouter", "parameter");

    /*------------------------ Provider配置 ------------------------*/
    public static final URLOption<Integer> WEIGHT_OPTION = new URLOption<>("weight", 100);
    public static final URLOption<String> METHOD_INCLUDE_OPTION = new URLOption<>("include", "*");
    public static final URLOption<Integer> DELAY_OPTION = new URLOption<>("delay", -1);
    public static final URLOption<Boolean> DYNAMIC_OPTION = new URLOption<>("dynamic", true);
    public static final URLOption<Integer> CONCURRENCY_OPTION = new URLOption<>("concurrency", 0);
    public static final URLOption<Boolean> LIMITER_OPTION = new URLOption<>("limiter", false);
    public static final URLOption<String> METHOD_EXCLUDE_OPTION = new URLOption<>("exclude", "");
    public static final URLOption<String> CONTEXT_PATH_OPTION = new URLOption<>("contextpath", "/");

    public static final String JAVA_VERSION_KEY = "javaVersion";

    public static final String BUILD_VERSION_KEY = "buildVersion";

    public static final String VERSION_KEY = "version";

    public static final String REGISTRY_ID = "registryId";

    /*------------------------ IO配置 ------------------------*/
    public static final URLOption<Integer> IO_THREAD_OPTION = new URLOption<>("ioThreads", 50);
    public static final URLOption<Integer> BOSS_THREAD_OPTION = new URLOption<>("bossThreads", () -> (ENVIRONMENT.get().cpuCores() + 1) / 2);
    /**
     * Epoll，默认打开，网络层会根据当前操作系统来进行判断
     */
    public static final URLOption<Boolean> EPOLL_OPTION = new URLOption<>("useEpoll", true);
    /**
     * 默认IO的buffer大小
     */
    public static final URLOption<Integer> BUFFER_OPTION = new URLOption<>("buffer", 8 * 1024);
    /**
     * 默认启动端口，包括不配置或者随机，都从此端口开始计算
     */
    public static final URLOption<Integer> PORT_OPTION = new URLOption<>("port", 22000);
    /**
     * 客户端和服务端创建工厂
     */
    public static final URLOption<String> ENDPOINT_FACTORY_OPTION = new URLOption<>("endpointFactory", "default");
    /**
     * 传输实现工厂
     */
    public static final URLOption<String> TRANSPORT_FACTORY_OPTION = new URLOption<>("transportFactory", "netty4");

    public static final URLOption<Integer> CONNECTION_ACCEPTS = new URLOption<>("accepts", Integer.MAX_VALUE);

    /*------------------------ 业务线程池配置 ------------------------*/
    public static final URLOption<String> THREADPOOL_OPTION = new URLOption<>("threadpool", "adaptive");
    public static final URLBiOption<Integer> CORE_SIZE_OPTION = new URLBiOption<>("thread.coreSize", "core.size", 20);
    public static final URLBiOption<Integer> MAX_SIZE_OPTION = new URLBiOption<>("thread.maxSize", "max.Size", 200);
    public static final URLOption<Integer> KEEP_ALIVE_TIME_OPTION = new URLOption<>("thread.keepAliveTime", 60000);
    public static final URLOption<Integer> QUEUES_OPTION = new URLOption<>("queues", 0);
    public static final URLOption<String> QUEUE_TYPE_OPTION = new URLOption<>("queueType", "normal");


    /*------------------------ regisry配置 ------------------------*/
    public static final String REGISTRY_NAME_KEY = "name";
    public static final URLOption<String> REGISTRY_BACKUP_PATH_OPTION = new URLOption<>("backupPath", (String) null);
    public static final URLOption<Integer> REGISTRY_BACKUP_DATUM_OPTION = new URLOption<>("backupDatum", 3);
    public static final URLOption<Boolean> SYSTEM_REFER_OPTION = new URLOption<>("systemRefer", Boolean.FALSE);
    public static final URLOption<Long> TASK_RETRY_INTERVAL_OPTION = new URLOption<>("taskRetryInterval", 5000L);


    /*------------------------ cache配置 ------------------------*/
    public static final URLOption<Boolean> CACHE_OPTION = new URLOption<>("cache", false);
    public static final URLOption<String> CACHE_PROVIDER_OPTION = new URLOption<>("cacheProvider", "caffeine");
    public static final URLOption<String> CACHE_KEY_GENERATOR_OPTION = new URLOption<>("cacheKeyGenerator", "default");
    public static final URLOption<Integer> CACHE_EXPIRE_TIME_OPTION = new URLOption<>("cacheExpireTime", -1);
    public static final URLOption<Integer> CACHE_CAPACITY_OPTION = new URLOption<>("cacheCapacity", 10000);
    public static final URLOption<Boolean> CACHE_NULLABLE_OPTION = new URLOption<>("cacheNullable", Boolean.FALSE);

    /**
     * 指标窗口时间（毫秒）
     */
    public static final URLOption<Long> METRIC_WINDOWS_TIME_OPTION = new URLOption<>("metric.window.time", 1000L);

    /**
     * 插件默认常量
     */
    public static final URLOption<String> CHANNEL_MANAGER_FACTORY_OPTION = new URLOption<>("channelManagerFactory", "shared");

    public static final URLOption<Integer> PAYLOAD = new URLOption<>("payload", 8388608);

    public static final String IO_THREADS_KEY = "ioThreads";
    public static final String BOSS_THREADS_KEY = "bossThreads";

    public static final String BUFFER_PREFER_DIRECT_KEY = "buffer.preferDirect";

    public static final URLOption<Boolean> TCP_NODELAY = new URLOption<>("tcpNoDelay", Boolean.TRUE);
    public static final String USE_EPOLL_KEY = "useEpoll";

    public static final URLOption<Boolean> BUFFER_POOLED_OPTION = new URLOption<>("buffer.pooled", false);
    public static final URLOption<Integer> INIT_SIZE_OPTION = new URLOption<>("initSize", 5);
    public static final URLOption<Integer> MIN_SIZE_OPTION = new URLOption<>("minSize", 0);
    public static final URLOption<Long> INIT_TIMEOUT_OPTION = new URLOption<>("initTimeout", 30000L);
    public static final URLOption<Integer> CONNECT_TIMEOUT_OPTION = new URLOption<>("connectTimeout", 5000);
    public static final URLOption<Integer> WRITE_BUFFER_HIGH_WATERMARK_OPTION = new URLOption<>("highWaterMark", 64 * 1024);
    public static final URLOption<Integer> WRITE_BUFFER_LOW_WATERMARK_OPTION = new URLOption<>("lowWaterMark", 32 * 1024);
    public static final URLOption<Integer> SO_RECEIVE_BUF_OPTION = new URLOption<>("soRevBuf", 8192 * 128);
    public static final URLOption<Integer> SO_SEND_BUF_OPTION = new URLOption<>("soSndBuf", 8192 * 128);
    public static final URLOption<Boolean> SO_KEEPALIVE_OPTION = new URLOption<>("soKeepAlive", Boolean.TRUE);
    public static final URLOption<Integer> SO_BACKLOG_OPTION = new URLOption<>("soBacklog", 35536);
    public static final URLOption<Integer> SO_TIMEOUT_OPTION = new URLOption<>("soTimeout", 10000);
    public static final String REUSE_PORT_KEY = "reusePort";

    /**
     * 会话超时时间
     */
    public static final URLOption<Long> SESSION_TIMEOUT_OPTION = new URLOption<>("sessionTimeout", 90000L);
    /**
     * 发送心跳的超时时间
     */
    public static final URLOption<Integer> SEND_TIMEOUT_OPTION = new URLOption<>("sendTimeout", 5000);
    /**
     * 心跳时间间隔
     */
    public static final URLOption<Integer> HEARTBEAT_INTERVAL_OPTION = new URLOption<>("hbInterval", 10000);
    public static final URLOption<Integer> HEARTBEAT_TIMEOUT_OPTION = new URLOption<>("hbTimeout", 5000);
    public static final URLOption<String> HEARTBEAT_MODE_OPTION = new URLOption<>("heartbeatMode", "TIMING");
    /**
     * 集群节点事件，是否开启空保护
     */
    public static final URLOption<Boolean> PROTECT_NULL_DATUM_OPTION = new URLOption<>("protectNullDatum", true);
    /**
     * 预热启动，默认1分钟
     */
    public static final URLOption<Integer> WARMUP_DURATION_OPTION = new URLOption<>("warmupDuration", 1000 * 60 * 1);
    /**
     * 预热权重
     */
    public static final URLOption<Integer> WARMUP_ORIGIN_WEIGHT_OPTION = new URLOption<>("originWeight", 0);

    /**
     * 默认熔断期
     */
    public static final int DEFAULT_BROKEN_PERIOD = 10 * 1000;
    /**
     * 默认恢复期
     */
    public static final int DEFAULT_DECUBATION = 10 * 1000;

    /**
     * 自适应负载均衡，综合评分算法
     */
    public static final URLOption<String> ADAPTIVE_ARBITER = new URLOption<>("adaptive.arbiter", "");
    /**
     * 自适应负载均衡，选举算法
     */
    public static final URLOption<String> ADAPTIVE_ELECTION = new URLOption<>("adaptive.election", "randomWeight");
    /**
     * 自适应负载均衡，满足评分良好的节点数则返回
     */
    public static final URLOption<Integer> ADAPTIVE_ENOUGH_GOODS = new URLOption<>("adaptive.enoughGoods", 20);
    /**
     * 自适应负载均衡，QPS阈值
     */
    public static final URLOption<Long> ADAPTIVE_QPS_FAIR = new URLOption<>("adaptive.qps.fair", (Long) null);
    /**
     * 自适应负载均衡，QPS阈值
     */
    public static final URLOption<Long> ADAPTIVE_QPS_POOR = new URLOption<>("adaptive.qps.poor", (Long) null);
    /**
     * 自适应负载均衡，并发数阈值
     */
    public static final URLOption<Long> ADAPTIVE_CONCURRENCY_FAIR = new URLOption<>("adaptive.concurrency.fair", (Long) null);
    /**
     * 自适应负载均衡，并发数阈值
     */
    public static final URLOption<Long> ADAPTIVE_CONCURRENCY_POOR = new URLOption<>("adaptive.concurrency.poor", (Long) null);
    /**
     * 自适应负载均衡，可用率一般阈值
     */
    public static final URLOption<Double> ADAPTIVE_AVAILABILITY_FAIR = new URLOption<>("adaptive.availability.fair", (Double) null);
    /**
     * 自适应负载均衡，可用率差阈值
     */
    public static final URLOption<Double> ADAPTIVE_AVAILABILITY_POOR = new URLOption<>("adaptive.availability.poor", (Double) null);
    /**
     * 自适应负载均衡，可用率禁用阈值
     */
    public static final URLOption<Double> ADAPTIVE_AVAILABILITY_DISABLE = new URLOption<>("adaptive.availability.disable", (Double) null);
    /**
     * 自适应负载均衡，TP一般阈值
     */
    public static final URLOption<Integer> ADAPTIVE_TP_FAIR = new URLOption<>("adaptive.tp.fair", (Integer) null);
    /**
     * 自适应负载均衡，TP差阈值
     */
    public static final URLOption<Integer> ADAPTIVE_TP_POOR = new URLOption<>("adaptive.tp.poor", (Integer) null);
    /**
     * 自适应负载均衡，TP禁用阈值
     */
    public static final URLOption<Integer> ADAPTIVE_TP_DISABLE = new URLOption<>("adaptive.tp.disable", (Integer) null);
    /**
     * 自适应负载均衡，熔断恢复期
     */
    public static final URLOption<Long> ADAPTIVE_DECUBATION = new URLOption<>("adaptive.decubation", 0L);
    /**
     * 自适应负载均衡，排除的机房
     */
    public static final URLOption<String> ADAPTIVE_EXCLUSION_ROOMS = new URLOption<>("adaptive.exclusionRooms", "");
    /**
     * 自适应负载均衡，节点TP
     */
    public static final URLOption<String> ADAPTIVE_NODE_TP = new URLOption<>("adaptive.nodeTp", "tp90");
    /**
     * 自适应负载均衡，集群TP
     */
    public static final URLOption<String> ADAPTIVE_CLUSTER_TP = new URLOption<>("adaptive.clusterTp", "tp30");

    /**
     * 是否启用epoll
     *
     * @param url url对象波·
     * @return boolean
     */
    public static boolean isUseEpoll(final URL url) {
        boolean linux = isLinux(url);
        return url == null ? false : url.getBoolean(USE_EPOLL_KEY, linux) && linux;
    }

    /**
     * 是否为linux系统
     *
     * @param url url对象
     * @return boolean
     */
    public static boolean isLinux(final URL url) {
        try {
            return url == null ? false : OsType.valueOf(url.getString(OS_TYPE, OsType.OTHER.name())) == OsType.LINUX;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * ssl开关
     */
    public static final URLOption<Boolean> SSL_ENABLE = new URLOption<>("ssl.enable", false);
    /**
     * ssl协议
     */
    public static final URLOption<String> SSL_PROTOCOLS = new URLOption<>("ssl.protocols", (String) null);
    /**
     * ssl秘钥库文件路径
     */
    public static final URLOption<String> SSL_PK_PATH = new URLOption<>("ssl.pkPath", (String) null);
    /**
     * ssl信任库文件路径
     */
    public static final URLOption<String> SSL_CA_PATH = new URLOption<>("ssl.caPath", (String) null);
    /**
     * ssl秘钥
     */
    public static final URLOption<String> SSL_PASSWORD = new URLOption<>("ssl.password", "");
    /**
     * keyStore算法
     */
    public static final URLOption<String> SSL_KEYSTORE = new URLOption<>("ssl.keystore", "JKS");
    /**
     * keyStore算法
     */
    public static final URLOption<String> SSL_CERTIFICATE = new URLOption<>("ssl.certificate", "SunX509");

    /**
     * 是否需要客户端验证（REQUIRE:需要客户端认证 及双向认证，OPTIONAL:可选 ，NONE:不需要客户端认证 单项认证）
     */
    public static final URLOption<String> SSL_CLIENT_AUTH = new URLOption<>("ssl.clientAuth", "NONE");
}
