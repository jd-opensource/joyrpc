package io.joyrpc.cluster.discovery;

import io.joyrpc.cluster.Region;
import io.joyrpc.constants.Version;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.extension.URL;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static io.joyrpc.constants.Constants.*;

/**
 * 标准化
 */
public interface Normalizer {

    /**
     * 由refer与export的url到注册中心存储url的转换function
     */
    Function<URL, URL> NORMALIZE_FUNCTION = url -> {
        Map<String, String> params = new HashMap<>();
        params.put(ALIAS_OPTION.getName(), url.getString(ALIAS_OPTION));
        params.put(BUILD_VERSION_KEY, String.valueOf(Version.BUILD_VERSION));
        params.put(VERSION_KEY, GlobalContext.getString(PROTOCOL_VERSION_KEY));
        params.put(KEY_APPAPTH, GlobalContext.getString(KEY_APPAPTH));
        params.put(KEY_APPID, GlobalContext.getString(KEY_APPID));
        params.put(KEY_APPNAME, GlobalContext.getString(KEY_APPNAME));
        params.put(KEY_APPINSID, GlobalContext.getString(KEY_APPINSID));
        params.put(Region.REGION, GlobalContext.getString(Region.REGION));
        params.put(Region.DATA_CENTER, GlobalContext.getString(Region.DATA_CENTER));
        //保留和原有的代码兼容
        params.put(JAVA_VERSION_KEY, GlobalContext.getString(KEY_JAVA_VERSION));
        params.put(ROLE_OPTION.getName(), url.getString(ROLE_OPTION));
        params.put(SERIALIZATION_OPTION.getName(), url.getString(SERIALIZATION_OPTION));
        params.put(TIMEOUT_OPTION.getName(), url.getString(TIMEOUT_OPTION.getName()));
        params.put(WEIGHT_OPTION.getName(), url.getString(WEIGHT_OPTION.getName()));
        params.put(DYNAMIC_OPTION.getName(), url.getString(DYNAMIC_OPTION.getName()));
        if (url.getBoolean(SSL_ENABLE)) {
            //ssl标识
            params.put(SSL_ENABLE.getName(), "true");
        }
        if (url.getBoolean(GENERIC_OPTION)) {
            //泛化调用标识
            params.put(GENERIC_OPTION.getName(), "true");
        }
        return new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getPath(), params);
    };

    /**
     * 标准化
     *
     * @param url
     * @return
     */
    default URL normalize(URL url) {
        return url == null ? null : NORMALIZE_FUNCTION.apply(url);
    }
}
