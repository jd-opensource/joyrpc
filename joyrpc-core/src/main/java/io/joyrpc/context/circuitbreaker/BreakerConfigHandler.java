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


import io.joyrpc.cluster.distribution.CircuitBreaker;
import io.joyrpc.cluster.distribution.circuitbreaker.McCircuitBreakerConfig;
import io.joyrpc.codec.serialization.TypeReference;
import io.joyrpc.context.ConfigEventHandler;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.MapParametric;
import io.joyrpc.extension.Parametric;
import io.joyrpc.util.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static io.joyrpc.Plugin.JSON;
import static io.joyrpc.constants.Constants.SETTING_INVOKE_CONSUMER_CIRCUITBREAKER;
import static io.joyrpc.context.ConfigEventHandler.ZERO_ORDER;
import static io.joyrpc.context.GlobalContext.update;
import static io.joyrpc.util.ClassUtils.forNameQuiet;
import static io.joyrpc.util.ClassUtils.getPublicMethod;


/**
 * 熔断配置信息
 */
@Extension(value = "circuitBreaker", order = ZERO_ORDER)
public class BreakerConfigHandler implements ConfigEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(BreakerConfigHandler.class);

    @Override
    public void handle(final String className, final Map<String, String> attrs) {
        if (update(className, attrs, SETTING_INVOKE_CONSUMER_CIRCUITBREAKER, null)) {
            try {
                doUpdate(className);
            } catch (Exception e) {
                logger.error("Error occurs while parsing circuitBreaker config. caused by " + e.getMessage(), e);
            }
        }
    }

    protected void doUpdate(final String className) {
        Class clazz = forNameQuiet(className);
        List<Map> results = JSON.get().parseObject(GlobalContext.asParametric(className).getString(SETTING_INVOKE_CONSUMER_CIRCUITBREAKER), new TypeReference<List<Map>>() {
        });

        Map<String, McCircuitBreakerConfig> configs = new HashMap<>(results.size());
        McCircuitBreakerConfig defConfig = null;
        McCircuitBreakerConfig config;
        //遍历配置
        for (Map result : results) {
            //构造熔断配置
            config = parse(result, className);
            configs.put(config.getName(), config);
            if ("*".equals(config.getName())) {
                //默认配置
                defConfig = config;
            }
        }
        //遍历方法，获取熔断配置
        if (clazz != null) {
            List<Method> methods = getPublicMethod(clazz);
            Map<String, Optional<CircuitBreaker>> breakers = new ConcurrentHashMap<>(methods.size());
            for (Method method : methods) {
                BreakerConfiguration.build(defConfig, configs, breakers, method.getName());
            }
            BreakerConfiguration.BREAKER.update(className, new BreakerConfiguration.MethodBreaker(breakers));
        } else {
            BreakerConfiguration.BREAKER.update(className, new BreakerConfiguration.MethodBreaker(defConfig, configs));
        }
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
        boolean enabled = parametric.getBoolean("enabled", Boolean.TRUE);
        long period = parametric.getPositive("period", 0L);
        long decubation = parametric.getPositive("decubation", 0L);
        int successiveFailures = parametric.getPositive("successiveFailures", 0);
        int availability = parametric.getPositive("availability", 0);
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
                logger.warn(String.format("Error occurs while setting circuitbreaker of %. caused by class is not found. %s ", className, type));
            } else if (!Throwable.class.isAssignableFrom(clazz)) {
                logger.warn(String.format("Error occurs while setting circuitbreaker of %. caused by class is not throwable. %s ", className, type));
            } else {
                result.add(clazz);
            }
        }
        return result;
    }
}
