package io.joyrpc.util;

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
 * 服务状态
 */
public enum Status {
    /**
     * 关闭
     */
    CLOSED {
        @Override
        public boolean isClose() {
            return true;
        }
    },
    /**
     * 打开中
     */
    OPENING,
    /**
     * 打开
     */
    OPENED,
    /**
     * 关闭中
     */
    CLOSING {
        @Override
        public boolean isClose() {
            return true;
        }
    };

    /**
     * 是否关闭
     *
     * @return 关闭标识
     */
    public boolean isClose() {
        return false;
    }

    /**
     * 是否在打开
     * @return 打开标识
     */
    public boolean isOpen() {
        return !isClose();
    }

}