package io.joyrpc.context.limiter;

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


import io.joyrpc.codec.serialization.TypeReference;
import io.joyrpc.constants.Constants;
import io.joyrpc.context.ConfigEventHandler;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.MapParametic;
import io.joyrpc.extension.Parametric;
import io.joyrpc.cluster.distribution.RateLimiter;
import io.joyrpc.cluster.distribution.limiter.RateLimiterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.joyrpc.Plugin.JSON;
import static io.joyrpc.Plugin.LIMITER;
import static io.joyrpc.cluster.distribution.RateLimiter.DELIMITER;


/**
 * 限流配置信息
 */
@Extension(value = "limiter", order = ConfigEventHandler.BIZ_ORDER)
public class LimiterConfigHandler implements ConfigEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(LimiterConfigHandler.class);

    public final static String DEFAULT_LIMITER_TYPE = "leakyBucket";

    @Override
    public void handle(final String className, final Map<String, String> attrs) {
        if (GlobalContext.update(className, attrs, Constants.SETTING_INVOKE_PROVIDER_LIMIT, null)) {
            Map<String, RateLimiterConfig> configs = parse(GlobalContext.asParametric(className).getString(Constants.SETTING_INVOKE_PROVIDER_LIMIT));
            Map<String, RateLimiter> limiters = load(configs, LimiterConfiguration.LIMITERS.get(className));
            //全量更新
            LimiterConfiguration.LIMITERS.update(className, limiters);
        }
    }

    /**
     * 加载配置
     *
     * @param configs
     * @param limiters
     * @return
     */
    protected Map<String, RateLimiter> load(final Map<String, RateLimiterConfig> configs, final Map<String, RateLimiter> limiters) {
        Map<String, RateLimiter> result = new HashMap<>(configs.size());
        RateLimiterConfig config;
        RateLimiter rateLimiter;
        //遍历老的限流器
        if (limiters != null && !limiters.isEmpty()) {
            for (Map.Entry<String, RateLimiter> old : limiters.entrySet()) {
                //从新的移除
                config = configs.remove(old.getKey());
                rateLimiter = old.getValue();
                if (config != null) {
                    //新的存在，则判断类型是否变更
                    if (!rateLimiter.type().equals(config.getType())) {
                        //类型不同，重新加载插件
                        rateLimiter = load(config);
                        if (rateLimiter != null) {
                            result.put(old.getKey(), rateLimiter);
                        }
                    } else {
                        //重新加载配置
                        rateLimiter.reload(config);
                        result.put(old.getKey(), rateLimiter);
                    }
                }
            }
        }
        //新增的配置
        for (Map.Entry<String, RateLimiterConfig> entry : configs.entrySet()) {
            rateLimiter = load(entry.getValue());
            if (rateLimiter != null) {
                result.put(entry.getKey(), rateLimiter);
            }
        }
        return result;
    }

    /**
     * 加载配置
     *
     * @param config
     * @return
     */
    protected RateLimiter load(final RateLimiterConfig config) {
        RateLimiter rateLimiter = LIMITER.get(config.getType());
        if (rateLimiter == null) {
            //插件没有
            logger.error("rate limiter is not found. " + config.getType());
        } else {
            //重新加载配置
            rateLimiter.reload(config);
        }
        return rateLimiter;
    }

    /**
     * 解析配置
     *
     * @param limitStr
     * @return
     */
    protected Map<String, RateLimiterConfig> parse(String limitStr) {
        Map<String, RateLimiterConfig> configs = new HashMap<>();
        List<Map> results = JSON.get().parseObject(limitStr, new TypeReference<List<Map>>() {
        });
        if (null == results) {
            return configs;
        }
        Parametric parametric;
        //遍历限流配置
        for (Map result : results) {
            parametric = new MapParametic(result);
            String alias = parametric.getString("alias", "");
            String methodName = parametric.getString("method", "");
            String appId = parametric.getString("appId", "");
            String type = parametric.getString("type", DEFAULT_LIMITER_TYPE);
            boolean open = parametric.getBoolean("enabled", "open", Boolean.TRUE);
            int limit = parametric.getInteger("limit", 0);
            long periodNanos = TimeUnit.MILLISECONDS.toNanos(parametric.getLong("period", 1000L));
            if (type != null && open && limit > 0 && periodNanos >= TimeUnit.MILLISECONDS.toNanos(1)) {
                //限流开关没有关闭，限流数大于0，限流周期大于等于1ms
                String limitKey = String.join(DELIMITER, methodName, alias, appId);
                configs.put(limitKey, RateLimiterConfig.builder().type(type).limitCount(limit).limitPeriodNanos(periodNanos).build());
            }
        }
        return configs;
    }
}
