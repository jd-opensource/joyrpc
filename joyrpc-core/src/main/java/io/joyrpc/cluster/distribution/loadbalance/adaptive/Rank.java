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

/**
 * 评分
 *
 */
//TODO 得分区间需要进行大量数据验证调整
public enum Rank {

    /**
     * 好
     */
    Good("Good", 100, 85),
    /**
     * 中
     */
    Fair("Fair", 84, 60),
    /**
     * 差
     */
    Poor("Poor", 59, 1),
    /**
     * 不可用
     */
    Disabled("Disabled", 0, 0);

    private String name;
    private int max;
    private int min;

    Rank(String name, int max, int min) {
        this.name = name;
        this.max = max;
        this.min = min;
    }

    public String getName() {
        return name;
    }

    public int getMax() {
        return max;
    }

    public int getMin() {
        return min;
    }

    public static Rank valueOf(int score) {
        if (score >= Good.min) {
            return Good;
        } else if (score >= Fair.min) {
            return Fair;
        } else if (score >= Poor.min) {
            return Poor;
        } else {
            return Disabled;
        }
    }

}
