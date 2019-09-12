package io.joyrpc.metric;

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
 * 一个周期的性能指标
 *
 * @date 2019年2月19日 下午4:05:06
 */
public interface TPMetric extends Metric {

    /**
     * 当前连续失败次数
     *
     * @return
     */
    long getSuccessiveFailures();

    /**
     * 当前并发数
     *
     * @return
     */
    long getActives();

    /**
     * 待分发流量
     *
     * @return
     */
    long getDistribution();

    /**
     * 是否熔断中
     *
     * @return
     */
    boolean isBroken();

    /**
     * 上一个周期的性能数据
     *
     * @return
     */
    TPSnapshot getSnapshot();

}
