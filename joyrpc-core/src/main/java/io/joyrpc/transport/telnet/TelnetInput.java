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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @date: 2019/4/26
 */
public class TelnetInput {

    public static final byte[] BYTES_WIN_CTRL_C = new byte[]{3};
    public static final byte[] BYTES_LINUX_CTRL_C = new byte[]{-1, -12, -1, -3, 6};
    public static final byte[] BYTES_LINUX_PAUSE = new byte[]{-1, -19, -1, -3, 6};
    public static final byte[] BYTES_CTRL_ENTER = new byte[]{'\r', '\n'};
    public static final byte[] BYTES_ENTER = new byte[]{'\n'};
    public static final byte[] BYTES_UP = new byte[]{27, 91, 65};
    public static final byte[] BYTES_DOWN = new byte[]{27, 91, 66};
    public static final byte[] BYTES_BACKSPACE = new byte[]{'\b'};

    public static final String INPUT = "input";
    public static final String CMD_HISTORY = "history";
    public static final char CMD_INDEX = '!';

    // 执行命令历史
    protected LinkedList<History> histories = new LinkedList<History>();
    // 命令计数器
    protected AtomicLong counter = new AtomicLong(0);
    // 翻阅命令索引
    protected int bookmark = -1;
    // 提醒符号
    protected String prompt = ">";
    // 最大历史条数
    protected int maxHistorySize = 100;
    // 不使用Line模式，缓存当前输入的命令
    protected StringBuilder input = new StringBuilder(100);

    public TelnetInput(String prompt, int maxHistorySize) {
        this.prompt = prompt;
        this.maxHistorySize = maxHistorySize;
    }

    /**
     * 追加输入
     *
     * @param input 输入字符串
     * @return 输入
     */
    public TelnetInput append(final String input) {
        if (input != null) {
            this.input.append(input);
        }
        return this;
    }

    /**
     * 删除最后一个字符
     *
     * @return 最后一个字符
     */
    public char deleteLast() {
        if (input.length() < 0) {
            return 0;
        }
        int pos = input.length() - 1;
        char last = input.charAt(pos);
        input.delete(pos, input.length());
        return last;
    }

    /**
     * 清空输入缓冲器
     */
    public void delete() {
        if (input.length() > 0) {
            input.delete(0, input.length());
        }
    }

    /**
     * 判断输入缓冲器是否为空
     *
     * @return 输入缓冲器为空标示
     */
    public boolean isEmpty() {
        return input.length() == 0;
    }

    public String getInput() {
        return input.toString();
    }

    public List<History> getHistories() {
        return histories;
    }

    /**
     * 添加历史命令
     *
     * @param command 命令
     */
    public void addHistory(final String command) {
        if (command != null && !command.isEmpty() && maxHistorySize > 0) {
            History last = histories.peekLast();
            if (last == null || !last.command.equals(command)) {
                //完整的命令
                histories.offer(new History(counter.incrementAndGet(), command));
                if (histories.size() > maxHistorySize) {
                    histories.removeFirst();
                }
            }

            bookmark = -1;
        }
    }

    /**
     * 获取翻阅命令
     *
     * @param backward 向后翻阅标示
     */
    public String roll(final boolean backward) {
        if (histories.isEmpty()) {
            return null;
        }
        if (backward) {
            if (bookmark == -1) {
                bookmark = histories.size() - 1;
            } else {
                bookmark = bookmark - 1 >= 0 ? bookmark - 1 : histories.size() - 1;
            }
        } else {
            if (bookmark == -1) {
                bookmark = 0;
            } else {
                bookmark = bookmark + 1 >= histories.size() ? 0 : bookmark + 1;
            }
        }
        History history = histories.get(bookmark);
        if (history != null) {
            int len = prompt.length() + input.length();

            input.delete(0, input.length());
            input.append(history.command);
            StringBuilder sb = new StringBuilder();
            // 清除当前行
            TelnetEscape.deleteLine(sb);
            // 光标往左移动
            TelnetEscape.moveLeft(len, sb);
            // 重新输出提示符和命令
            sb.append(prompt).append(history.command);
            return sb.toString();
        }
        return null;
    }

    /**
     * 判断全角字符
     *
     * @param ch 字符
     * @return 是否全角
     */
    public static boolean isDoubleByteChar(final char ch) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(ch);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {
            return true;
        }
        return false;
    }

    /**
     * 是否是退出命令
     *
     * @param message 消息体
     * @return 退出命令标示
     */
    public static boolean isExit(final byte[] message) {
        //退出操作
        if (endsWith(message, BYTES_WIN_CTRL_C)) {
            return true;
        } else if (endsWith(message, BYTES_LINUX_CTRL_C)) {
            return true;
        } else if (endsWith(message, BYTES_LINUX_PAUSE)) {
            return true;
        }
        return false;
    }

    /**
     * 是否是回车结尾
     *
     * @param message 消息体
     * @return 回车键结尾标示
     */
    public static boolean isEnter(final byte[] message) {
        if (endsWith(message, BYTES_CTRL_ENTER)) {
            return true;
        } else if (endsWith(message, BYTES_ENTER)) {
            return true;
        }
        return false;
    }

    /**
     * 是否是退格键
     *
     * @param message 消息体
     * @return 退格键标示
     */
    public static boolean isBackspace(final byte[] message) {
        return equals(message, BYTES_BACKSPACE);
    }

    /**
     * 是否是向上键
     *
     * @param message 消息体
     * @return 向上键标示
     */
    public static boolean isUp(final byte[] message) {
        return equals(message, BYTES_UP);
    }

    /**
     * 是否是向上键
     *
     * @param message 消息体
     * @return 向上键标示
     */
    public static boolean isDown(final byte[] message) {
        return equals(message, BYTES_DOWN);
    }

    /**
     * 判断是否相等
     *
     * @param src    消息体
     * @param target 结尾命令
     * @return 是否某字符串结尾
     */
    public static boolean equals(final byte[] src, final byte[] target) {
        return Arrays.equals(src, target);
    }

    /**
     * 判断某字符结尾
     *
     * @param message 消息体
     * @param command 结尾命令
     * @return 是否某字符串结尾
     */
    public static boolean endsWith(final byte[] message, final byte[] command) {
        if (message.length < command.length) {
            return false;
        }
        int offset = message.length - command.length;
        for (int i = command.length - 1; i >= 0; i--) {
            if (message[offset + i] != command[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取指定序号的历史命令
     *
     * @param command 指定序号
     * @return 命令
     */
    public String getHistory(final String command) {
        if (command == null || command.length() < 2) {
            return null;
        }
        if (command.charAt(0) != '!') {
            return null;
        }
        // 序号
        try {
            long id = Long.parseLong(command.substring(1));
            for (History history : histories) {
                if (history.getId() == id) {
                    return history.command;
                }
            }
        } catch (NumberFormatException e) {
        }
        return null;
    }

    /**
     * 历史
     */
    public static class History {
        // ID
        protected long id;
        // 命令
        protected String command;

        public History(long id, String command) {
            this.id = id;
            this.command = command;
        }

        public long getId() {
            return id;
        }

        public String getCommand() {
            return command;
        }
    }
}
