package io.joyrpc.apm.metric;

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
 * 上一个周期的性能数据
 *
 * @date 2019年2月19日 下午2:29:11
 */
public interface TPSnapshot extends Metric {

    /**
     * 请求数
     *
     * @return
     */
    long getRequests();

    /**
     * 成功的请求数
     *
     * @return
     */
    long getSuccesses();

    /**
     * 失败的请求数
     *
     * @return
     */
    long getFailures();

    /**
     * 成功率
     *
     * @return
     */
    double getAvailability();

    /**
     * 记录数，一次请求可能多条数据
     *
     * @return
     */
    long getRecords();

    /**
     * 数据大小
     *
     * @return
     */
    long getDataSize();

    /**
     * 成功请求花费的实际
     *
     * @return
     */
    int getElapsedTime();

    /**
     * 最大时间
     *
     * @return
     */
    int getMax();

    /**
     * 最小时间
     *
     * @return
     */
    int getMin();

    /**
     * 平均时间
     *
     * @return
     */
    int getAvg();

    /**
     * TP40
     *
     * @return
     */
    int getTp30();

    /**
     * TP50
     *
     * @return
     */
    int getTp50();

    /**
     * TP90
     *
     * @return
     */
    int getTp90();

    /**
     * TP99
     *
     * @return
     */
    int getTp99();

    /**
     * TP999
     *
     * @return
     */
    int getTp999();

}
