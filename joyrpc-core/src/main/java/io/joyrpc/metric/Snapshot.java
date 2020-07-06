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
 * 快照
 */
public interface Snapshot<T extends Metric> {

    /**
     * 创建指标快照
     */
    void snapshot();

    /**
     * 获取最近周期创建的指标快照
     *
     * @return 指标快照
     */
    T getSnapshot();

    /**
     * 判断是否过期
     *
     * @return 过期标识
     */
    boolean isExpired();

    /**
     * 设置上次快照时间，单位毫秒
     *
     * @param timeMillis 上次快照时间，单位毫秒
     */
    void setLastSnapshotTime(long timeMillis);

}
