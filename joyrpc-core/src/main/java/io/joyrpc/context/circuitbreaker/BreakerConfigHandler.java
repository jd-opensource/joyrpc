package io.joyrpc.context.circuitbreaker;

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


import io.joyrpc.cluster.distribution.circuitbreaker.McCircuitBreakerConfig;
import io.joyrpc.cluster.distribution.circuitbreaker.McIntfCircuitBreakerConfig;
import io.joyrpc.codec.serialization.TypeReference;
import io.joyrpc.context.ConfigEventHandler;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.MapParametric;
import io.joyrpc.extension.Parametric;
import io.joyrpc.util.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static io.joyrpc.Plugin.JSON;
import static io.joyrpc.constants.Constants.GLOBAL_SETTING;
import static io.joyrpc.constants.Constants.SETTING_INVOKE_CONSUMER_CIRCUITBREAKER;
import static io.joyrpc.context.ConfigEventHandler.BREAKER_ORDER;
import static io.joyrpc.context.circuitbreaker.BreakerConfiguration.BREAKER;


/**
 * 熔断配置信息
 */
@Extension(value = "circuitBreaker", order = BREAKER_ORDER)
public class BreakerConfigHandler implements ConfigEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(BreakerConfigHandler.class);

    @Override
    public void handle(final String className, final Map<String, String> oldAttrs, final Map<String, String> newAttrs) {
        if (!GLOBAL_SETTING.equals(className)) {
            String oldAttr = oldAttrs.get(SETTING_INVOKE_CONSUMER_CIRCUITBREAKER);
            String newAttr = newAttrs.get(SETTING_INVOKE_CONSUMER_CIRCUITBREAKER);
            if (!Objects.equals(oldAttr, newAttr)) {
                try {
                    BREAKER.update(className, parse(className, newAttr));
                } catch (Exception e) {
                    logger.error("Error occurs while parsing circuitBreaker config. caused by " + e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public String[] getKeys() {
        return new String[]{SETTING_INVOKE_CONSUMER_CIRCUITBREAKER};
    }

    /**
     * 解析配置
     *
     * @param className
     * @param text
     * @return
     */
    protected McIntfCircuitBreakerConfig parse(final String className, final String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        //方法的熔断配置
        List<Map> results = JSON.get().parseObject(text, new TypeReference<List<Map>>() {
        });

        Map<String, McCircuitBreakerConfig> configs = new HashMap<>(results.size());
        McCircuitBreakerConfig defConfig = null;
        McCircuitBreakerConfig config;
        //遍历解析方法的熔断配置
        for (Map result : results) {
            //解析方法的熔断配置
            config = parse(result, className);
            if ("*".equals(config.getName())) {
                //默认配置
                defConfig = config;
            } else {
                //方法配置
                configs.put(config.getName(), config);
            }
        }
        return new McIntfCircuitBreakerConfig(defConfig, configs);

    }

    /**
     * 构造配置
     *
     * @param map
     * @param className
     * @return
     */
    protected McCircuitBreakerConfig parse(final Map map, final String className) {
        Parametric parametric = new MapParametric(map);
        String method = parametric.getString("method");
        Boolean enabled = parametric.getBoolean("enabled");
        Long period = parametric.getPositive("period", (Long) null);
        Long decubation = parametric.getPositive("decubation", (Long) null);
        Integer successiveFailures = parametric.getPositive("successiveFailures", (Integer) null);
        Integer availability = parametric.getPositive("availability", (Integer) null);
        Set<Class<? extends Throwable>> whites = buildThrowable(parametric.getObject("whites"), className);
        Set<Class<? extends Throwable>> blacks = buildThrowable(parametric.getObject("blacks"), className);
        return new McCircuitBreakerConfig(method, enabled, period, decubation, successiveFailures, availability, whites, blacks);
    }

    /**
     * 构建异常
     *
     * @param types
     * @param className
     * @return
     */
    protected Set<Class<? extends Throwable>> buildThrowable(String[] types, final String className) {
        if (types == null || types.length == 0) {
            return null;
        }
        Set<Class<? extends Throwable>> result = new HashSet<>(types.length);
        Class clazz;
        for (String type : types) {
            clazz = ClassUtils.forNameQuiet(type);
            if (clazz == null) {
                logger.warn(String.format("Error occurs while setting circuit breaker of %. caused by class is not found. %s ", className, type));
            } else if (!Throwable.class.isAssignableFrom(clazz)) {
                logger.warn(String.format("Error occurs while setting circuit breaker of %. caused by class is not throwable. %s ", className, type));
            } else {
                result.add(clazz);
            }
        }
        return result;
    }
}
