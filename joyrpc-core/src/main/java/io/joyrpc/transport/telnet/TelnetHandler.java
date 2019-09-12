/**
 *
 */
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

import io.joyrpc.exception.ParserException;
import io.joyrpc.extension.Type;
import io.joyrpc.transport.channel.Channel;

/**
 * @date: 2019/1/22
 */
public interface TelnetHandler extends Type<String> {

    String LINE = "\r\n";

    String HELP_SHORT = "h";
    String HELP_LONG = "help";
    String HELP_CMD = "-h";

    /**
     * telnet 命令名称
     * @return String
     */
    @Override
    String type();

    /**
     * 执行telnet命令
     * @param channel   channel
     * @param args  命令参数
     * @return TelnetResponse
     * @throws ParserException
     */
    TelnetResponse telnet(Channel channel, String[] args) throws ParserException;

    /**
     * 帮助信息
     * @return
     */
    String help();

    /**
     * 描述信息
     * @return
     */
    String description();

    /**
     * 简洁描述
     * @return
     */
    default String shortDescription() {
        return description();
    }

    /**
     * 是否要换行
     * @return
     */
    default boolean newLine() {
        return true;
    }

}
