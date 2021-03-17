package io.joyrpc.apm.health;

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
 * 健康状态
 */
public enum HealthState {
    /**
     * Healthy health state.
     */
    HEALTHY((byte) 0, "healthy"),
    /**
     * Exhausted health state.
     */
    EXHAUSTED((byte) 1, "exhausted"),
    /**
     * Dead health state.
     */
    DEAD((byte) 2, "dead");

    private final byte status;
    private final String description;

    HealthState(byte status, String description) {
        this.status = status;
        this.description = description;
    }

    /**
     * Value of node heartbeat result . health state.
     *
     * @param b the b
     * @return the node heartbeat result . health state
     */
    public static HealthState valueOf(byte b) {
        switch (b) {
            case (byte) 0:
                return HEALTHY;
            case (byte) 1:
                return EXHAUSTED;
            default:
                return DEAD;
        }
    }
}
