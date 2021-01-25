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

import io.joyrpc.event.Event;

/**
 * 状态事件
 */
public class StateEvent implements Event {

    /**
     * 开始输出
     */
    public static final int START_EXPORT = 10;
    /**
     * 输出成功
     */
    public static final int SUCCESS_EXPORT = 11;
    /**
     * 输出失败
     */
    public static final int FAIL_EXPORT = 12;
    /**
     * 输出失状态异常
     */
    public static final int FAIL_EXPORT_ILLEGAL_STATE = 13;
    /**
     * 开始打开
     */
    public static final int START_OPEN = 30;
    /**
     * 打开成功
     */
    public static final int SUCCESS_OPEN = 31;
    /**
     * 打开失败
     */
    public static final int FAIL_OPEN = 32;
    /**
     * 打开状态异常
     */
    public static final int FAIL_OPEN_ILLEGAL_STATE = 33;
    /**
     * 开始关闭
     */
    public static final int START_CLOSE = 40;
    /**
     * 关闭成功
     */
    public static final int SUCCESS_CLOSE = 41;

    /**
     * 事件类型
     */
    protected final int type;
    /**
     * 异常
     */
    protected final Throwable throwable;

    public StateEvent(int type) {
        this(type, null);
    }

    public StateEvent(int type, Throwable exception) {
        this.type = type;
        this.throwable = exception;
    }

    public int getType() {
        return type;
    }

    public Throwable getThrowable() {
        return throwable;
    }
}
