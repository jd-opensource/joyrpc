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

import java.lang.reflect.Field;

import static io.joyrpc.util.Memory.UNSAFE;

/**
 * 整数状态
 */
public class StateInt implements StateTransition {

    /**
     * 已关闭
     */
    protected static final int CLOSED = 0;
    /**
     * 关闭中
     */
    protected static final int CLOSING = 1;
    /**
     * 导出中
     */
    protected static final int EXPORTING = 2;
    /**
     * 已导出
     */
    protected static final int EXPORTED = 3;
    /**
     * 打开中
     */
    protected static final int OPENING = 4;
    /**
     * 已打开
     */
    protected static final int OPENED = 5;

    protected static long valueOffset;

    protected volatile int status = CLOSED;

    static {
        try {
            Field valueField = StateInt.class.getDeclaredField("status");
            valueOffset = UNSAFE.objectFieldOffset(valueField);
        } catch (NoSuchFieldException e) {
        }
    }

    @Override
    public boolean isOpening() {
        return status == OPENING;
    }

    @Override
    public boolean isOpened() {
        return status == OPENED;
    }

    @Override
    public boolean isClosing() {
        return status == CLOSING;
    }

    @Override
    public boolean isClosed() {
        return status == CLOSED;
    }

    @Override
    public boolean isClose() {
        return status <= CLOSING;
    }

    @Override
    public boolean isOpen() {
        return status >= OPENING;
    }

    @Override
    public int tryOpening() {
        return UNSAFE.compareAndSwapInt(this, valueOffset, CLOSED, OPENING) ? SUCCESS : FAILED;
    }

    @Override
    public int tryOpened() {
        return UNSAFE.compareAndSwapInt(this, valueOffset, OPENING, OPENED) ? SUCCESS : FAILED;
    }

    @Override
    public int tryClosing() {
        while (true) {
            switch (status) {
                case OPENING:
                    if (UNSAFE.compareAndSwapInt(this, valueOffset, OPENING, CLOSING)) {
                        return SUCCESS_OPENING_TO_CLOSING;
                    }
                    break;
                case OPENED:
                    if (UNSAFE.compareAndSwapInt(this, valueOffset, OPENED, CLOSING)) {
                        return SUCCESS_OPENED_TO_CLOSING;
                    }
                    break;
                default:
                    return FAILED;
            }
        }
    }

    @Override
    public int tryClosed() {
        return UNSAFE.compareAndSwapInt(this, valueOffset, CLOSING, CLOSED) ? SUCCESS : FAILED;
    }

    @Override
    public void toClosed() {
        status = CLOSED;
    }

    /**
     * 状态转换
     *
     * @param from 开始状态
     * @param to   结束状态
     * @return 成功标识
     */
    public int translate(int from, int to) {
        return UNSAFE.compareAndSwapInt(this, valueOffset, from, to) ? SUCCESS : FAILED;
    }

    @Override
    public String name() {
        switch (status) {
            case CLOSED:
                return "CLOSED";
            case CLOSING:
                return "CLOSING";
            case OPENING:
                return "OPENING";
            case OPENED:
                return "OPENED";
        }
        return "UNKNOWN";
    }

    /**
     * 扩展状态，增加了导出中和已经导出状态
     */
    public static class ExStateInt extends StateInt implements ExStateTransition {

        @Override
        public int tryExporting() {
            return UNSAFE.compareAndSwapInt(this, valueOffset, CLOSED, EXPORTING) ? SUCCESS : FAILED;
        }

        @Override
        public int tryExported() {
            return UNSAFE.compareAndSwapInt(this, valueOffset, EXPORTING, EXPORTED) ? SUCCESS : FAILED;
        }

        @Override
        public int tryOpening() {
            return UNSAFE.compareAndSwapInt(this, valueOffset, EXPORTED, OPENING) ? SUCCESS : FAILED;
        }

        @Override
        public int tryClosing() {
            while (true) {
                switch (status) {
                    case EXPORTING:
                        if (UNSAFE.compareAndSwapInt(this, valueOffset, EXPORTING, CLOSING)) {
                            return SUCCESS_EXPORTING_TO_CLOSING;
                        }
                        break;
                    case EXPORTED:
                        if (UNSAFE.compareAndSwapInt(this, valueOffset, EXPORTED, CLOSING)) {
                            return SUCCESS_EXPORTED_TO_CLOSING;
                        }
                        break;
                    case OPENING:
                        if (UNSAFE.compareAndSwapInt(this, valueOffset, OPENING, CLOSING)) {
                            return SUCCESS_OPENING_TO_CLOSING;
                        }
                        break;
                    case OPENED:
                        if (UNSAFE.compareAndSwapInt(this, valueOffset, OPENED, CLOSING)) {
                            return SUCCESS_OPENED_TO_CLOSING;
                        }
                        break;
                    default:
                        return FAILED;
                }
            }
        }

        @Override
        public boolean isExporting() {
            return status == EXPORTING;
        }

        @Override
        public boolean isExported() {
            return status == EXPORTED;
        }

        @Override
        public boolean isExport() {
            return status == EXPORTING || status == EXPORTED;
        }
    }
}
