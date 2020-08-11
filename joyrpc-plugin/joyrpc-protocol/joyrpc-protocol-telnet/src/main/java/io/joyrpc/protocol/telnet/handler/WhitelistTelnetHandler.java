package io.joyrpc.protocol.telnet.handler;

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

import io.joyrpc.permission.SerializerWhiteList;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.telnet.TelnetResponse;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.util.TreeSet;

/**
 * 序列化白名单
 *
 * @date: 2019/1/22
 */
public class WhitelistTelnetHandler extends AbstractTelnetHandler {

    public WhitelistTelnetHandler() {
        options = new Options()
                .addOption(HELP_SHORT, HELP_LONG, false, "show help message for command whitelist");
    }

    @Override
    public String type() {
        return "allow";
    }

    @Override
    public String description() {
        return "Show global serialization whitelist.";
    }

    @Override
    public TelnetResponse telnet(Channel channel, String[] args) {
        if (args == null) {
            return new TelnetResponse(help());
        }
        String result = "";
        CommandLine cmd = getCommand(options, args);
        if (cmd.hasOption(HELP_SHORT)) {
            result = help();
        } else {
            StringBuilder builder = new StringBuilder(500);
            TreeSet<String> names = new TreeSet<>();
            for (Class<?> clazz : SerializerWhiteList.getGlobalWhitelist().getWhitelist()) {
                names.add(clazz.getName());
            }
            for (String name : names) {
                builder.append(name).append('\n');
            }
            result = builder.toString();
        }
        return new TelnetResponse(result);
    }
}
