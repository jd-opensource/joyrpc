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

import java.util.Set;

/**
 * 熔断配置
 */
public class McCircuitBreakerConfig {

    /**
     * 名称
     */
    protected String name;

    /**
     * 是否启用
     */
    protected boolean enabled;
    /**
     * 熔断周期，默认10秒
     */
    protected Long period;
    /**
     * 恢复期（默认5秒）
     */
    protected Long decubation;
    /**
     * 连续失败次数阈值，高于或等于该值则熔断
     */
    protected Integer successiveFailures;
    /**
     * 可用性阈值，小于等于该阈值则熔断
     */
    protected Integer availability;
    /**
     * 白名单
     */
    protected Set<Class<? extends Throwable>> whites;
    /**
     * 黑名单
     */
    protected Set<Class<? extends Throwable>> blacks;

    public McCircuitBreakerConfig(String name, boolean enabled,
                                  Long period, Long decubation,
                                  Integer successiveFailures, Integer availability,
                                  Set<Class<? extends Throwable>> whites,
                                  Set<Class<? extends Throwable>> blacks) {
        this.name = name;
        this.enabled = enabled;
        this.period = period;
        this.decubation = decubation;
        this.successiveFailures = successiveFailures;
        this.availability = availability;
        this.whites = whites;
        this.blacks = blacks;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Long getPeriod() {
        return period;
    }

    public void setPeriod(Long period) {
        this.period = period;
    }

    public Long getDecubation() {
        return decubation;
    }

    public void setDecubation(Long decubation) {
        this.decubation = decubation;
    }

    public Integer getSuccessiveFailures() {
        return successiveFailures;
    }

    public void setSuccessiveFailures(Integer successiveFailures) {
        this.successiveFailures = successiveFailures;
    }

    public Integer getAvailability() {
        return availability;
    }

    public void setAvailability(Integer availability) {
        this.availability = availability;
    }

    public Set<Class<? extends Throwable>> getWhites() {
        return whites;
    }

    public void setWhites(Set<Class<? extends Throwable>> whites) {
        this.whites = whites;
    }

    public Set<Class<? extends Throwable>> getBlacks() {
        return blacks;
    }

    public void setBlacks(Set<Class<? extends Throwable>> blacks) {
        this.blacks = blacks;
    }

    /**
     * 合并
     *
     * @param source
     */
    public void merge(final McCircuitBreakerConfig source) {
        if (source == null) {
            return;
        }
        if (period == null) {
            period = source.period;
        }
        if (decubation == null) {
            decubation = source.decubation;
        }
        if (successiveFailures == null) {
            successiveFailures = source.successiveFailures;
        }
        if (availability == null) {
            availability = source.availability;
        }
        if (whites == null || whites.isEmpty()) {
            whites = source.whites;
        }
        if (blacks == null || blacks.isEmpty()) {
            blacks = source.blacks;
        }
    }
}
