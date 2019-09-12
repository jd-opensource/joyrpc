package io.joyrpc.transport.heartbeat;

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

import io.joyrpc.transport.message.Message;

import java.util.function.Supplier;

/**
 * 心跳策略
 */
public interface HeartbeatStrategy {

    /**
     * 创建一条心跳信息的 supplier
     *
     * @return
     */
    Supplier<Message> getHeartbeat();

    /**
     * 心跳超时时间
     *
     * @return
     */
    default int getTimeout() {
        return 5000;
    }

    /**
     * 心跳定时间隔（TIMING）/ 触发心跳的空闲间隔 (IDLE)
     *
     * @return
     */
    default int getInterval() {
        return 10000;
    }

    /**
     * 心跳模式 定时/空闲
     *
     * @return
     */
    default HeartbeatMode getHeartbeatMode() {
        return HeartbeatMode.TIMING;
    }

    /**
     * 心跳模式
     */
    enum HeartbeatMode {
        /**
         * 定时
         */
        TIMING,
        /**
         * 空闲
         */
        IDLE
    }

}
