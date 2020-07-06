package io.joyrpc.cluster.distribution.loadbalance.adaptive;

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

import java.io.Serializable;
import java.util.function.BiFunction;

/**
 * 评分
 */
public class RankScore<T> implements Serializable, Cloneable {

    private static final long serialVersionUID = -4708948741189908093L;

    public static final BiFunction<Long, Long, Integer> LONG_ASCENDING = (o1, o2) -> (int) (o1 - o2);

    public static final BiFunction<Long, Long, Integer> LONG_DESCENDING = (o1, o2) -> (int) (o2 - o1);

    public static final BiFunction<Integer, Integer, Integer> INT_ASCENDING = (o1, o2) -> o1 - o2;

    public static final BiFunction<Integer, Integer, Integer> INT_DESCENDING = (o1, o2) -> o2 - o1;

    public static final BiFunction<Double, Double, Integer> DOUBLE_ASCENDING = (o1, o2) -> {
        double result = o1 - o2;
        if (result > 0) {
            return 1;
        } else if (result < 0) {
            return -1;
        }
        return 0;
    };

    public static final BiFunction<Double, Double, Integer> DOUBLE_DESCENDING = (o1, o2) -> {
        double result = o2 - o1;
        if (result > 0) {
            return 1;
        } else if (result < 0) {
            return -1;
        }
        return 0;
    };

    /**
     * 一般的阈值
     */
    protected T fair;
    /**
     * 差的阈值
     */
    protected T poor;
    /**
     * 禁用的阈值
     */
    protected T disable;

    /**
     * 构造函数
     */
    public RankScore() {
    }

    /**
     * 构造函数
     *
     * @param fair
     * @param poor
     * @param disable
     */
    public RankScore(T fair, T poor, T disable) {
        this.fair = fair;
        this.poor = poor;
        this.disable = disable;
    }

    public T getFair() {
        return fair;
    }

    public void setFair(T fair) {
        this.fair = fair;
    }

    public T getPoor() {
        return poor;
    }

    public void setPoor(T poor) {
        this.poor = poor;
    }

    public T getDisable() {
        return disable;
    }

    public void setDisable(T disable) {
        this.disable = disable;
    }

    /**
     * 对值进行评分
     *
     * @param value
     * @param comparable
     * @return
     */
    public Rank score(final T value, final BiFunction<T, T, Integer> comparable) {
        int result;
        Rank max = Rank.Good;
        if (fair != null) {
            result = comparable.apply(value, fair);
            if (result > 0) {
                return Rank.Good;
            } else if (result == 0) {
                return Rank.Fair;
            }
            max = Rank.Fair;
        }
        if (poor != null) {
            result = comparable.apply(value, poor);
            if (result > 0) {
                return max;
            } else if (result == 0) {
                return Rank.Poor;
            }
            max = Rank.Poor;
        }
        if (disable != null && comparable.apply(value, disable) <= 0) {
            return Rank.Disabled;
        }
        return max;
    }

    public boolean isEmpty() {
        return fair == null && poor == null && disable == null;
    }

    @Override
    public RankScore<T> clone() {
        try {
            return (RankScore<T>) super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RankScore{");
        sb.append("fair=").append(fair);
        sb.append(", poor=").append(poor);
        sb.append(", disable=").append(disable);
        sb.append('}');
        return sb.toString();
    }
}
