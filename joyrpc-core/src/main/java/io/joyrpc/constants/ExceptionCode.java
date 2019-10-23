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

/**
 * Exception Code definition
 */
public abstract class ExceptionCode {
    private static final String PREFIX = "RPC-";
    // 公共模块
    private static final String COMMON_PREFIX = PREFIX + "01";
    // CONSUMER 模块
    private static final String CONSUMER_PREFIX = PREFIX + "02";
    // PROVIDER 模块异常
    private static final String PROVIDER_PREFIX = PREFIX + "03";
    // 注册中心模块
    private static final String REGISTER_PREFIX = PREFIX + "04";
    // 集群模块
    private static final String CLUSTER_PREFIX = PREFIX + "05";
    // Codec模块
    private static final String CODEC_PREFIX = PREFIX + "06";
    // FILTER模块
    private static final String FILTER_PREFIX = PREFIX + "07";
    // Transport模块
    private static final String TRANSPORT_PREFIX = PREFIX + "08";

    private static final String SYS_LEVEL = "1";
    private static final String CONFIG_LEVEL = "2";
    private static final String BIZ_LEVEL = "3";


    // 公共 模块
    public static final String COMMON_VALUE_ILLEGAL = COMMON_PREFIX + CONFIG_LEVEL + "001";
    public static final String COMMON_PLUGIN_ILLEGAL = COMMON_PREFIX + CONFIG_LEVEL + "002";
    public static final String COMMON_NOT_RIGHT_INTERFACE = COMMON_PREFIX + CONFIG_LEVEL + "003";
    public static final String COMMON_CLASS_NOT_FOUND = COMMON_PREFIX + CONFIG_LEVEL + "004";
    public static final String COMMON_CACHE_REF = COMMON_PREFIX + CONFIG_LEVEL + "005";
    public static final String COMMON_SERVER_PORT_ILLEGAL = COMMON_PREFIX + CONFIG_LEVEL + "007";
    public static final String COMMON_ABUSE_HIDE_KEY = COMMON_PREFIX + CONFIG_LEVEL + "008";
    public static final String COMMON_CONN_STRATEGY_INTERVAL = COMMON_PREFIX + CONFIG_LEVEL + "009";
    public static final String COMMON_DUPLICATE_ALIAS = COMMON_PREFIX + CONFIG_LEVEL + "010";
    public static final String COMMON_URL_ENCODING = COMMON_PREFIX + CONFIG_LEVEL + "011";
    public static final String COMMON_PROTOCOL_NOT_FOUND = COMMON_PREFIX + CONFIG_LEVEL + "012";
    public static final String COMMON_SERIALIZATION_NOT_FOUND = COMMON_PREFIX + CONFIG_LEVEL + "013";
    public static final String COMMON_CALL_BACK_ERROR = COMMON_PREFIX + CONFIG_LEVEL + "014";

    // CONSUMER 模块
    public static final String CONSUMER_ALIAS_IS_NULL = CONSUMER_PREFIX + CONFIG_LEVEL + "001";
    public static final String CONSUMER_REFER_WAIT_ERROR = CONSUMER_PREFIX + CONFIG_LEVEL + "002";
    public static final String CONSUMER_NO_ALIVE_PROVIDER = CONSUMER_PREFIX + BIZ_LEVEL + "003";
    public static final String CONSUMER_RPC_EXCEPTION = CONSUMER_PREFIX + SYS_LEVEL + "004";
    public static final String CONSUMER_GROUP_NO_REFER = CONSUMER_PREFIX + BIZ_LEVEL + "006";
    public static final String CONSUMER_GROUP_ARGS_INDEX = CONSUMER_PREFIX + BIZ_LEVEL + "007";
    public static final String CONSUMER_FAILOVER_CLASS = CONSUMER_PREFIX + BIZ_LEVEL + "008";
    //路由配置错误
    public static final String CONSUMER_ROUTE_CONF = CONSUMER_PREFIX + CONFIG_LEVEL + "009";
    public static final String CONSUMER_DUPLICATE_REFER = CONSUMER_PREFIX + CONFIG_LEVEL + "010";

    // PROVIDER 模块
    public static final String PROVIDER_INVOKER_MISMATCH = PROVIDER_PREFIX + CONFIG_LEVEL + "001";
    public static final String PROVIDER_REF_NO_FOUND = PROVIDER_PREFIX + CONFIG_LEVEL + "002";
    public static final String PROVIDER_HAS_EXPORTED = PROVIDER_PREFIX + CONFIG_LEVEL + "003";
    public static final String PROVIDER_ALIAS_IS_NULL = PROVIDER_PREFIX + CONFIG_LEVEL + "004";
    public static final String PROVIDER_SERVER_IS_NULL = PROVIDER_PREFIX + CONFIG_LEVEL + "005";
    public static final String PROVIDER_SERVER_OPEN_EXCEPTION = PROVIDER_PREFIX + SYS_LEVEL + "006";
    public static final String PROVIDER_THREAD_EXHAUSTED = PROVIDER_PREFIX + BIZ_LEVEL + "007";
    public static final String PROVIDER_INVALID_TOKEN = PROVIDER_PREFIX + BIZ_LEVEL + "008";
    public static final String PROVIDER_OFFLINE = PROVIDER_PREFIX + SYS_LEVEL + "009";
    public static final String PROVIDER_METHOD_OVERLOADING = PROVIDER_PREFIX + CONFIG_LEVEL + "010";
    //限流配置异常
    public static final String PROVIDER_LIMITER_CONF = PROVIDER_PREFIX + CONFIG_LEVEL + "011";
    //配置多个provider异常
    public static final String PROVIDER_DUPLICATE_BEAN = PROVIDER_PREFIX + CONFIG_LEVEL + "012";
    //发送消息异常
    public static final String PROVIDER_SEND_MESSAGE_ERROR = PROVIDER_PREFIX + BIZ_LEVEL + "013";
    //丢弃超时消息
    public static final String PROVIDER_DISCARD_TIMEOUT_MESSAGE = PROVIDER_PREFIX + BIZ_LEVEL + "014";
    //授权失败
    public static final String PROVIDER_AUTH_FAIL = PROVIDER_PREFIX + BIZ_LEVEL + "015";
    //执行task时异常
    public static final String PROVIDER_TASK_FAIL = PROVIDER_PREFIX + BIZ_LEVEL + "016";
    //session失效
    public static final String PROVIDER_TASK_SESSION_EXPIRED = PROVIDER_PREFIX + BIZ_LEVEL + "017";
    public static final String PROVIDER_DUPLICATE_EXPORT = PROVIDER_PREFIX + CONFIG_LEVEL + "018";

    // FILTER 模块
    public static final String FILTER_PLUGIN_NO_EXISTS = FILTER_PREFIX + CONFIG_LEVEL + "001";
    public static final String FILTER_PLUGIN_USE_WRONG = FILTER_PREFIX + CONFIG_LEVEL + "002";
    public static final String FILTER_INVOKE_LIMIT = FILTER_PREFIX + CONFIG_LEVEL + "003";
    //consumer并发超时异常
    public static final String FILTER_CONCURRENT_CONSUMER_TIMEOUT = FILTER_PREFIX + CONFIG_LEVEL + "004";
    //泛化转换异常
    public static final String FILTER_GENERIC_CONVERT = FILTER_PREFIX + CONFIG_LEVEL + "005";
    //valid校验provider异常
    public static final String FILTER_VALID_PROVIDER = FILTER_PREFIX + CONFIG_LEVEL + "006";
    //valid校验consumer异常
    public static final String FILTER_VALID_CONSUMER = FILTER_PREFIX + CONFIG_LEVEL + "007";
    //privider超时异常
    public static final String FILTER_PROVIDER_TIMEOUT = FILTER_PREFIX + BIZ_LEVEL + "008";
    //provider并发超时异常
    public static final String FILTER_CONCURRENT_PROVIDER_TIMEOUT = FILTER_PREFIX + CONFIG_LEVEL + "009";


    // 注册中心模块
    public static final String REGISTRY_NOT_CONFIG = REGISTER_PREFIX + CONFIG_LEVEL + "001";
    public static final String REGISTRY_IS_NULL = REGISTER_PREFIX + CONFIG_LEVEL + "002";
    public static final String REGISTRY_CREATE_FAIL = REGISTER_PREFIX + CONFIG_LEVEL + "003";

    // Codec模块
    //msgpack 序列化异常
    //不支持注解异常
    public static final String CODEC_NOT_SUPPORT_ANNOTATION = CODEC_PREFIX + CONFIG_LEVEL + "001";
    //找不到序列化模版异常
    public static final String CODEC_NOT_FOUND_TEMPLATE = CODEC_PREFIX + CONFIG_LEVEL + "002";
    //找不到序列化class异常
    public static final String CODEC_NOT_FOUND_CLASS = CODEC_PREFIX + CONFIG_LEVEL + "003";
    //序列化数据格式异常
    public static final String CODEC_NOT_FOUND_FORMAT = CODEC_PREFIX + CONFIG_LEVEL + "004";
    //序列化黑名单
    public static final String CODEC_BLACK_LIST_SERIALIZER = CODEC_PREFIX + CONFIG_LEVEL + "005";
    //类型不匹配异常
    public static final String CODEC_TYPE_NOT_MATCH = CODEC_PREFIX + CONFIG_LEVEL + "006";
    //编解码时出现的异常
    //序列化异常
    public static final String CODEC_SERIALIZER_EXCEPTION = CODEC_PREFIX + CONFIG_LEVEL + "007";
    //编解码IO异常
    public static final String CODEC_IO_EXCEPTION = CODEC_PREFIX + CONFIG_LEVEL + "008";
    //编解码header格式异常
    public static final String CODEC_HEADER_FORMAT_EXCEPTION = CODEC_PREFIX + CONFIG_LEVEL + "008";
    //编解码异常
    public static final String CODEC_DEFAULT_EXCEPTION = CODEC_PREFIX + CONFIG_LEVEL + "100";


    /**
     * 格式化errorCode
     *
     * @param code
     * @return
     */
    public static String format(final String code) {
        return new StringBuffer(20).append("[").append(code).append("]").toString();
    }
}
