package io.joyrpc.cluster.distribution.circuitbreaker;

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
import io.joyrpc.exception.OverloadException;
import io.joyrpc.apm.metric.TPMetric;
import io.joyrpc.apm.metric.TPWindow;
import io.joyrpc.permission.BlackWhiteList;
import io.joyrpc.permission.ExceptionBlackWhiteList;
import io.joyrpc.util.MilliPeriod;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.context.Variable.VARIABLE;
import static io.joyrpc.util.ClassUtils.forName;
import static io.joyrpc.util.StringUtils.SEMICOLON_COMMA_WHITESPACE;
import static io.joyrpc.util.StringUtils.split;

/**
 * 熔断器
 */
public class McCircuitBreaker implements CircuitBreaker {
    /**
     * 是否启用
     */
    protected boolean enabled;
    /**
     * 熔断周期，默认10秒
     */
    protected long period;
    /**
     * 恢复期（默认5秒）
     */
    protected long decubation;
    /**
     * 连续失败次数阈值，高于或等于该值则熔断
     */
    protected int successiveFailures;
    /**
     * 可用性阈值，小于等于该阈值则熔断
     */
    protected double availability;
    /**
     * 异常白名单，进行熔断
     */
    protected BlackWhiteList<Class<? extends Throwable>> blackWhiteList;

    /**
     * 构造函数
     *
     * @param config
     */
    public McCircuitBreaker(final McCircuitBreakerConfig config) {
        Objects.requireNonNull(config, "config can not be null.");
        this.enabled = config.enabled == null ? Boolean.FALSE : config.enabled;
        this.period = config.period != null && config.period > 0 ? config.period : DEFAULT_BROKEN_PERIOD;
        this.decubation = config.decubation != null && config.decubation > 0 ? config.decubation : DEFAULT_DECUBATION;
        this.successiveFailures = config.successiveFailures != null && config.successiveFailures > 0 ? config.successiveFailures : 0;
        //连续失败次数和可用率可以并存
        this.availability = config.availability != null && config.availability > 0 ? config.availability : 0;
        //默认加上服务端过载和超时异常，避免对所有异常进行熔断
        Set<Class<? extends Throwable>> whites = config.whites == null ? new HashSet<>() : config.whites;
        Set<Class<? extends Throwable>> blacks = config.blacks;
        addWhites(whites);
        this.blackWhiteList = new ExceptionBlackWhiteList(whites.isEmpty() ? null : whites, blacks, true);
    }

    /**
     * 添加白名单
     *
     * @param whites 白名单
     */
    protected void addWhites(final Set<Class<? extends Throwable>> whites) {
        //增加异常白名单
        whites.add(OverloadException.class);
        whites.add(TimeoutException.class);
        //从全局配置里面添加熔断异常
        String value = VARIABLE.getString(CIRCUIT_BREAKER_EXCEPTION);
        String[] parts = split(value, SEMICOLON_COMMA_WHITESPACE);
        if (parts != null) {
            for (String part : parts) {
                try {
                    Class<?> aClass = forName(part);
                    if (Throwable.class.isAssignableFrom(aClass)) {
                        whites.add((Class<? extends Throwable>) aClass);
                    }
                } catch (ClassNotFoundException e) {
                }
            }
        }
    }

    @Override
    public void apply(final Throwable throwable, final TPWindow window) {
        if (!enabled) {
            return;
        }
        MilliPeriod brokenPeriod = window.getBrokenPeriod();
        if (brokenPeriod != null && brokenPeriod.between()) {
            //熔断中
            return;
        }
        //获取统计信息快照
        TPMetric metric = window.getSnapshot();
        //先判断连续失败次数,再判断失败率
        if (successiveFailures > 0 && metric.getSuccessiveFailures() >= successiveFailures
                || availability > 0 && metric.getSnapshot().getAvailability() <= availability) {
            //熔断，内部处理了并发调用
            window.broken(period, decubation);
        }
    }

    @Override
    public boolean support(final Throwable throwable) {
        return throwable == null ? false : blackWhiteList.isValid(throwable.getClass());
    }
}
