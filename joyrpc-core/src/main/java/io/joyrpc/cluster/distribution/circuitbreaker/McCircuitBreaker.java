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
import io.joyrpc.metric.TPMetric;
import io.joyrpc.metric.TPWindow;
import io.joyrpc.permission.BlackWhiteList;
import io.joyrpc.permission.ExceptionBlackWhiteList;
import io.joyrpc.util.MilliPeriod;

import java.util.Objects;

import static io.joyrpc.constants.Constants.DEFAULT_BROKEN_PERIOD;
import static io.joyrpc.constants.Constants.DEFAULT_DECUBATION;

/**
 * 内置熔断器
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
        this.enabled = config.enabled;
        this.period = config.period != null && config.period > 0 ? config.period : DEFAULT_BROKEN_PERIOD;
        this.decubation = config.decubation != null && config.decubation > 0 ? config.decubation : DEFAULT_DECUBATION;
        this.successiveFailures = config.successiveFailures != null && config.successiveFailures > 0 ? config.successiveFailures : 0;
        //如果启用了连续失败次数，则禁用可用率
        this.availability = successiveFailures > 0 ? 0 : (config.availability != null && config.availability > 0 ? config.availability : 0);
        this.blackWhiteList = new ExceptionBlackWhiteList(config.whites, config.blacks, true);
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
