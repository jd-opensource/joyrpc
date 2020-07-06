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

import io.joyrpc.extension.Parametric;
import io.joyrpc.extension.WrapperParametric;

import java.util.HashSet;
import java.util.Set;

import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.util.ClassUtils.forName;
import static io.joyrpc.util.StringUtils.SEMICOLON_COMMA_WHITESPACE;
import static io.joyrpc.util.StringUtils.split;

/**
 * 方法熔断配置
 */
public class McCircuitBreakerConfig implements Cloneable {

    /**
     * 名称
     */
    protected String name;

    /**
     * 是否启用
     */
    protected Boolean enabled;
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

    public McCircuitBreakerConfig(String name) {
        this.name = name;
    }

    public McCircuitBreakerConfig(String name, Boolean enabled,
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

    public McCircuitBreakerConfig(final Parametric parametric) {
        this.name = parametric instanceof WrapperParametric ? ((WrapperParametric) parametric).getName() : "*";
        this.enabled = parametric.getBoolean(CIRCUIT_BREAKER_ENABLE);
        this.period = parametric.getPositive(CIRCUIT_BREAKER_PERIOD, (Long) null);
        this.decubation = parametric.getPositive(CIRCUIT_BREAKER_DECUBATION, (Long) null);
        this.successiveFailures = parametric.getPositive(CIRCUIT_BREAKER_SUCCESSIVE_FAILURES, (Integer) null);
        this.availability = parametric.getPositive(CIRCUIT_BREAKER_AVAILABILITY, (Integer) null);
        String value = parametric.getString(CIRCUIT_BREAKER_EXCEPTION);
        String[] parts = split(value, SEMICOLON_COMMA_WHITESPACE);
        if (parts != null) {
            this.whites = new HashSet<>(parts.length);
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getEnabled() {
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

    public void addWhite(Class<? extends Throwable> clazz) {
        if (clazz != null) {
            if (whites == null) {
                whites = new HashSet<>();
            }
            whites.add(clazz);
        }
    }

    @Override
    public McCircuitBreakerConfig clone() {
        try {
            return (McCircuitBreakerConfig) super.clone();
        } catch (CloneNotSupportedException ignored) {
            return null;
        }
    }

    /**
     * 合并
     *
     * @param source
     */
    public McCircuitBreakerConfig merge(final McCircuitBreakerConfig source) {
        if (source == null || source == this) {
            return this;
        }
        if (enabled == null) {
            enabled = source.enabled;
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
        //合并白名单
        if (whites == null || whites.isEmpty()) {
            whites = source.whites;
        } else if (source.whites != null) {
            whites.addAll(source.whites);
        }
        //合并黑名单
        if (blacks == null || blacks.isEmpty()) {
            blacks = source.blacks;
        } else if (source.blacks != null) {
            blacks.addAll(source.blacks);
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        McCircuitBreakerConfig that = (McCircuitBreakerConfig) o;

        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if (enabled != null ? !enabled.equals(that.enabled) : that.enabled != null) {
            return false;
        }
        if (period != null ? !period.equals(that.period) : that.period != null) {
            return false;
        }
        if (decubation != null ? !decubation.equals(that.decubation) : that.decubation != null) {
            return false;
        }
        if (successiveFailures != null ? !successiveFailures.equals(that.successiveFailures) : that.successiveFailures != null) {
            return false;
        }
        if (availability != null ? !availability.equals(that.availability) : that.availability != null) {
            return false;
        }
        if (whites != null ? !whites.equals(that.whites) : that.whites != null) {
            return false;
        }
        return blacks != null ? blacks.equals(that.blacks) : that.blacks == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (enabled != null ? enabled.hashCode() : 0);
        result = 31 * result + (period != null ? period.hashCode() : 0);
        result = 31 * result + (decubation != null ? decubation.hashCode() : 0);
        result = 31 * result + (successiveFailures != null ? successiveFailures.hashCode() : 0);
        result = 31 * result + (availability != null ? availability.hashCode() : 0);
        result = 31 * result + (whites != null ? whites.hashCode() : 0);
        result = 31 * result + (blacks != null ? blacks.hashCode() : 0);
        return result;
    }
}
