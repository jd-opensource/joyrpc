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

import io.joyrpc.util.MilliPeriod;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能窗口
 *
 * @date 2019年2月19日 下午2:21:42
 */
public interface TPWindow extends Metric, Snapshot<TPMetric> {

    /**
     * 请求成功
     *
     * @param timeMillis 耗费的时间，单位毫秒
     */
    void success(int timeMillis);

    /**
     * 成功请求一次
     *
     * @param timeMillis 耗费的时间，单位毫秒
     * @param records    记录数
     * @param dataSize   数据大小
     */
    void success(int timeMillis, int records, long dataSize);

    /**
     * 请求失败
     */
    void failure();

    /**
     * 重置请求失败数
     */
    void resetSuccessiveFailures();

    /**
     * 判断当前是否有记录的请求
     */
    boolean hasRequest();

    /**
     * 并发请求数
     *
     * @return 并发请求数
     */
    AtomicLong actives();

    /**
     * 待分发流量
     *
     * @return 待分发流量
     */
    AtomicLong distribution();

    /**
     * 窗口时间，单位毫秒
     *
     * @return 窗口时间，单位毫秒
     */
    long getWindowTime();

    /**
     * 获取熔断时间
     *
     * @return
     */
    MilliPeriod getBrokenPeriod();

    /**
     * 熔断
     *
     * @param duration   熔断时间（毫秒）
     * @param decubation 恢复期（毫秒）
     */
    void broken(long duration, long decubation);

    /**
     * 虚弱，进入恢复阶段，在指定时间短里面平滑恢复权重
     *
     * @param period   原子并发
     * @param duration 虚弱时间
     */
    void weak(MilliPeriod period, long duration);

    /**
     * 获取虚弱时间
     *
     * @return 虚弱时间
     */
    MilliPeriod getWeakPeriod();


}
