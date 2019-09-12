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

import io.joyrpc.util.StringUtils;

/**
 * telent 请求对象
 *
 * @date: 2019/4/28
 */
public class TelnetRequest {

    /**
     * 请求命令
     */
    protected String cmd;
    /**
     * 请求参数
     */
    protected String[] args;
    /**
     * 提醒符号
     */
    protected String prompt;

    public TelnetRequest(final String cmd, final String[] args, String prompt) {
        this.cmd = cmd.trim();
        this.args = args == null ? new String[0] : args;
        this.prompt = prompt;
    }

    /**
     * 解析
     *
     * @param message
     * @param prompt
     * @return
     */
    public static TelnetRequest parse(final String message, final String prompt) {
        String[] parts = StringUtils.split(message, o -> Character.isWhitespace(o));
        if (parts == null || parts.length == 0) {
            return null;
        }
        String cmd = parts[0];
        String[] args;
        if (parts.length > 1) {
            args = new String[parts.length - 1];
            System.arraycopy(parts, 1, args, 0, parts.length - 1);
        } else {
            args = new String[0];
        }
        return new TelnetRequest(cmd, args, prompt);
    }

    public String getCmd() {
        return cmd;
    }

    public String[] getArgs() {
        return args;
    }

    public String getPrompt() {
        return prompt;
    }
}
