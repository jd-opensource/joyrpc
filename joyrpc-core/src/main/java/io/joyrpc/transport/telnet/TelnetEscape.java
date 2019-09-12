package io.joyrpc.transport.telnet;

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
 * Telnet 转义符号
 * <p>
 * ESC[nX:清除光标右边n个字符，光标不动。<br/>
 * ESC[K或ESC[OK;清除光标右边全部字符，光标不动。<br/>
 * ESC[1K:清除光标左边全部字符，光标不动。<br/>
 * ESC[2K:清除整行，光标不动。<br/>
 * ESC[J或ESC[OJ:清除光标右下屏所有字符，光标不动。<br/>
 * ESC[1J:清除光标左上屏所有字符，光标不动。<br/>
 * ESC[2J或ESCc:清屏，光标移到左上角。<br/>
 * ESC[nM:删除光标之下n行，剩下行往上移，光标不动。<br/>
 * ESC[nP:删除光标右边n个字符，剩下部分左移，光标不动。<br/>
 * ESC[n@:在当前光标处插入n个字符。<br/>
 * ESC[nL:在当前光标下插入n行。<br/>
 * ESC[nA:光标上移n行。<br/>
 * ESC[nB:光标下移n行。<br/>
 * ESC[nC:光标右移n个字符。<br/>
 * ESC[nD:光标左移n个字符。<br/>
 * ESC[n;mH :光标定位到第n行m列(类似代码ESC[n;mf)。<br/>
 * <p>
 *
 * @date: 2019/4/26
 */
public class TelnetEscape {


    public static final String CR = "\r\n";

    // 文件结束符
    public static final byte NVT_EOF = -20;
    // 挂起当前进程
    public static final byte NVT_SUSP = -19;
    // 中止进程
    public static final byte NVT_ABORT = -18;
    // 记录结束符
    public static final byte NVT_EOR = -17;
    // 子选项结束
    public static final byte NVT_SE = -16;
    // 空操作
    public static final byte NVT_NOP = -15;
    // 数据标记
    public static final byte NVT_DM = -14;
    // 终止符
    public static final byte NVT_BRK = -13;
    // 终止进程
    public static final byte NVT_IP = -12;
    // 终止输出
    public static final byte NVT_AO = -11;
    // 请求应答
    public static final byte NVT_AYT = -10;
    // 请求应答
    public static final byte NVT_EC = -9;
    // 擦除一行
    public static final byte NVT_EL = -8;
    // 继续
    public static final byte NVT_GA = -7;
    // 子选项开始
    public static final byte NVT_SB = -6;
    // 选项协商
    public static final byte NVT_WILL = -5;
    // 选项协商
    public static final byte NVT_WONT = -4;
    // 选项协商
    public static final byte NVT_DO = -3;
    // 选项协商
    public static final byte NVT_DONT = -2;
    // 命令开始符号
    public static final byte NVT_IAC = -1;
    // 回应
    public static final byte OPT_ECHO = 1;
    // 禁止继续
    public static final byte OPT_STOP = 3;
    // 状态
    public static final byte OPT_STATUS = 5;
    // 时钟标识
    public static final byte OPT_TIME = 6;
    // 终端类型
    public static final byte OPT_TERMINAL_TYPE = 24;
    // 窗口大小
    public static final byte OPT_WINDOW_SIZE = 31;
    // 终端速率
    public static final byte OPT_TERMINAL_RATIO = 32;
    // 远端流量控制
    public static final byte OPT_FLOW_CONTROL = 33;
    // 行模式
    public static final byte OPT_LINE = 34;
    // 环境变量
    public static final byte OPT_ENV = 36;

    /**
     * ESC[nX:清除光标右边n个字符，光标不动。
     *
     * @param number 字符数量
     * @return 转义字符串
     */
    public static final String deleteRight(final int number) {
        return deleteRight(number, new StringBuilder(20)).toString();
    }

    /**
     * ESC[nX:清除光标右边n个字符，光标不动。
     *
     * @param number  字符数量
     * @param builder 字符串构造器
     * @return 转义字符串
     */
    public static final StringBuilder deleteRight(final int number, final StringBuilder builder) {
        if (number < 0) {
            throw new IllegalArgumentException("number must be greater than or equals 0");
        }
        if (builder == null) {
            return builder;
        }
        return builder.append((char) 0x1B).append((char) 0x5B).append(number).append((char) 0x58);
    }

    /**
     * ESC[nP:删除光标右边n个字符，剩下部分左移，光标不动。
     *
     * @param number 字符数量
     * @return 转义字符串
     */
    public static final String deleteRightAndShift(final int number) {
        return deleteRightAndShift(number, new StringBuilder(20)).toString();
    }

    /**
     * ESC[nP:删除光标右边n个字符，剩下部分左移，光标不动。
     *
     * @param number  字符数量
     * @param builder 字符串构造器
     * @return 转义字符串
     */
    public static final StringBuilder deleteRightAndShift(final int number, final StringBuilder builder) {
        if (number < 0) {
            throw new IllegalArgumentException("number must be greater than or equals 0");
        }
        if (builder == null) {
            return builder;
        }
        return builder.append((char) 0x1B).append((char) 0x5B).append(number).append((char) 0x52);
    }

    /**
     * ESC[K或ESC[OK;清除光标右边全部字符，光标不动。
     *
     * @return 转义字符串
     */
    public static final String deleteRight() {
        return deleteRight(new StringBuilder(20)).toString();
    }

    /**
     * ESC[K或ESC[OK;清除光标右边全部字符，光标不动。
     *
     * @param builder 字符串构造器
     * @return 转义字符串
     */
    public static final StringBuilder deleteRight(final StringBuilder builder) {
        if (builder == null) {
            return builder;
        }
        return builder.append((char) 0x1B).append((char) 0x5B).append((char) 0x4B);
    }

    /**
     * ESC[1K:清除光标左边全部字符，光标不动。
     *
     * @return 转义字符串
     */
    public static final String deleteLeft() {
        return deleteLeft(new StringBuilder(20)).toString();
    }

    /**
     * ESC[1K:清除光标左边全部字符，光标不动。
     *
     * @param builder 字符串构造器
     * @return 转义字符串
     */
    public static final StringBuilder deleteLeft(final StringBuilder builder) {
        if (builder == null) {
            return builder;
        }
        return builder.append((char) 0x1B).append((char) 0x5B).append((char) 0x31).append((char) 0x4B);
    }

    /**
     * ESC[2K:清除整行，光标不动。
     *
     * @return 转义字符串
     */
    public static final String deleteLine() {
        return deleteLine(new StringBuilder(20)).toString();
    }

    /**
     * ESC[2K:清除整行，光标不动。
     *
     * @param builder 字符串构造器
     * @return 转义字符串
     */
    public static final StringBuilder deleteLine(final StringBuilder builder) {
        if (builder == null) {
            return builder;
        }
        return builder.append((char) 0x1B).append((char) 0x5B).append((char) 0x32).append((char) 0x4B);
    }

    /**
     * ESC[J或ESC[OJ:清除光标右下屏所有字符，光标不动。
     *
     * @return 转义字符串
     */
    public static final String deleteLowerRight() {
        return deleteLowerRight(new StringBuilder(20)).toString();
    }

    /**
     * ESC[J或ESC[OJ:清除光标右下屏所有字符，光标不动。
     *
     * @param builder 字符串构造器
     * @return 转义字符串
     */
    public static final StringBuilder deleteLowerRight(final StringBuilder builder) {
        if (builder == null) {
            return builder;
        }
        return builder.append((char) 0x1B).append((char) 0x5B).append((char) 0x4A);
    }

    /**
     * ESC[1J:清除光标左上屏所有字符，光标不动。
     *
     * @return 转义字符串
     */
    public static final String deleteUpperLeft() {
        return deleteUpperLeft(new StringBuilder(20)).toString();
    }

    /**
     * ESC[1J:清除光标左上屏所有字符，光标不动。
     *
     * @param builder 字符串构造器
     * @return 转义字符串
     */
    public static final StringBuilder deleteUpperLeft(final StringBuilder builder) {
        if (builder == null) {
            return builder;
        }
        return builder.append((char) 0x1B).append((char) 0x5B).append((char) 0x31).append((char) 0x4A);
    }

    /**
     * ESC[2J或ESCc:清屏，光标移到左上角。
     *
     * @return 转义字符串
     */
    public static final String clear() {
        return clear(new StringBuilder(20)).toString();
    }

    /**
     * ESC[2J或ESCc:清屏，光标移到左上角。
     *
     * @param builder 字符串构造器
     * @return 转义字符串
     */
    public static final StringBuilder clear(final StringBuilder builder) {
        if (builder == null) {
            return builder;
        }
        return builder.append((char) 0x1B).append((char) 0x5B).append((char) 0x48).append((char) 0x1B)
                .append((char) 0x5B).append((char) 0x4A);
    }

    /**
     * ESC[nM:删除光标之下n行，剩下行往上移，光标不动。
     *
     * @param number 字符数量
     * @return 转义字符串
     */
    public static final String deleteLine(final int number) {
        return deleteLine(number, new StringBuilder(20)).toString();
    }

    /**
     * ESC[nM:删除光标之下n行，剩下行往上移，光标不动。
     *
     * @param number  字符数量
     * @param builder 字符串构造器
     * @return 转义字符串
     */
    public static final StringBuilder deleteLine(final int number, final StringBuilder builder) {
        if (number < 0) {
            throw new IllegalArgumentException("number must be greater than or equals 0");
        } else if (builder == null) {
            return builder;
        }
        return builder.append((char) 0x1B).append((char) 0x5B).append(number).append((char) 0x4D);
    }

    /**
     * ESC[n@:在当前光标处插入n个字符。
     *
     * @param number 字符数量
     * @return 转义字符串
     */
    public static final String insertChar(final int number) {
        return insertChar(number, new StringBuilder(20)).toString();
    }

    /**
     * ESC[n@:在当前光标处插入n个字符。
     *
     * @param number  字符数量
     * @param builder 字符串构造器
     * @return 转义字符串
     */
    public static final StringBuilder insertChar(final int number, final StringBuilder builder) {
        if (number < 0) {
            throw new IllegalArgumentException("number must be greater than or equals 0");
        } else if (builder == null) {
            return builder;
        }
        return builder.append((char) 0x1B).append((char) 0x5B).append(number).append((char) 0x40);
    }

    /**
     * ESC[nL:在当前光标下插入n行。
     *
     * @param number 字符数量
     * @return 转义字符串
     */
    public static final String insertLine(final int number) {
        return insertLine(number, new StringBuilder(20)).toString();
    }

    /**
     * ESC[nL:在当前光标下插入n行。
     *
     * @param number  字符数量
     * @param builder 字符串构造器
     * @return 转义字符串
     */
    public static final StringBuilder insertLine(final int number, final StringBuilder builder) {
        if (number < 0) {
            throw new IllegalArgumentException("number must be greater than or equals 0");
        } else if (builder == null) {
            return builder;
        }
        return builder.append((char) 0x1B).append((char) 0x5B).append(number).append((char) 0x4C);
    }

    /**
     * ESC[nA:光标上移n行。
     *
     * @param number 字符数量
     * @return 转义字符串
     */
    public static final String moveUp(final int number) {
        return moveUp(number, new StringBuilder(20)).toString();
    }

    /**
     * ESC[nA:光标上移n行。
     *
     * @param number  字符数量
     * @param builder 字符串构造器
     * @return 转义字符串
     */
    public static final StringBuilder moveUp(final int number, final StringBuilder builder) {
        if (number < 0) {
            throw new IllegalArgumentException("number must be greater than or equals 0");
        } else if (builder == null) {
            return builder;
        }
        return builder.append((char) 0x1B).append((char) 0x5B).append(number).append((char) 0x41);
    }

    /**
     * ESC[nB:光标下移n行。
     *
     * @param number 字符数量
     * @return 转义字符串
     */
    public static final String moveDown(final int number) {
        return moveDown(number, new StringBuilder(20)).toString();
    }

    /**
     * ESC[nB:光标下移n行。
     *
     * @param number  字符数量
     * @param builder 字符串构造器
     * @return 转义字符串
     */
    public static final StringBuilder moveDown(final int number, final StringBuilder builder) {
        if (number < 0) {
            throw new IllegalArgumentException("number must be greater than or equals 0");
        } else if (builder == null) {
            return builder;
        }
        return builder.append((char) 0x1B).append((char) 0x5B).append(number).append((char) 0x42);
    }

    /**
     * ESC[nC:光标右移n个字符。
     *
     * @param number 字符数量
     * @return 转义字符串
     */
    public static final String moveRight(final int number) {
        return moveRight(number, new StringBuilder(20)).toString();
    }

    /**
     * ESC[nC:光标右移n个字符。
     *
     * @param number  字符数量
     * @param builder 字符串构造器
     * @return 转义字符串
     */
    public static final StringBuilder moveRight(final int number, final StringBuilder builder) {
        if (number < 0) {
            throw new IllegalArgumentException("number must be greater than or equals 0");
        }
        if (builder == null) {
            return builder;
        }
        return builder.append((char) 0x1B).append((char) 0x5B).append(number).append((char) 0x43);
    }

    /**
     * ESC[nD:光标左移n个字符。
     *
     * @param number 字符数量
     * @return 转义字符串
     */
    public static final String moveLeft(final int number) {
        return moveLeft(number, new StringBuilder(20)).toString();
    }

    /**
     * ESC[nD:光标左移n个字符。
     *
     * @param number  字符数量
     * @param builder 字符串构造器
     * @return 转义字符串
     */
    public static final StringBuilder moveLeft(final int number, final StringBuilder builder) {
        if (number < 0) {
            throw new IllegalArgumentException("number must be greater than or equals 0");
        }
        if (builder == null) {
            return builder;
        }
        return builder.append((char) 0x1B).append((char) 0x5B).append(number).append((char) 0x44);
    }

    /**
     * ESC[n;mH :光标定位到第n行m列(类似代码ESC[n;mf)。
     *
     * @param line   行
     * @param column 列
     * @return 转义字符串
     */
    public static final String move(final int line, final int column) {
        return move(line, column, new StringBuilder(20)).toString();
    }

    /**
     * ESC[n;mH :光标定位到第n行m列(类似代码ESC[n;mf)。
     *
     * @param line    行
     * @param column  列
     * @param builder 字符串构造器
     * @return 转义字符串
     */
    public static final StringBuilder move(final int line, final int column, final StringBuilder builder) {
        if (line < 0) {
            throw new IllegalArgumentException("line must be greater than or equals 0");
        } else if (column < 0) {
            throw new IllegalArgumentException("column must be greater than or equals 0");
        } else if (builder == null) {
            return builder;
        }
        return builder.append((char) 0x1B).append((char) 0x5B).append(line).append((char) 0x3B).append(column)
                .append((char) 0x48);
    }

}
