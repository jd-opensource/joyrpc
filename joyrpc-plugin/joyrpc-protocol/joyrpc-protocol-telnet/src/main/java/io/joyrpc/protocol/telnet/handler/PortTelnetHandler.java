/**
 *
 */
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

import io.joyrpc.invoker.InvokerManager;
import io.joyrpc.transport.Server;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ServerChannel;
import io.joyrpc.transport.telnet.TelnetResponse;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.util.List;

import static io.joyrpc.invoker.InvokerManager.getServer;
import static io.joyrpc.invoker.InvokerManager.getServers;

/**
 * @date: 2019/1/22
 */
public class PortTelnetHandler extends AbstractTelnetHandler {

    public PortTelnetHandler() {
        options = new Options()
                .addOption("c", false, "show concise information")
                .addOption(HELP_SHORT, HELP_LONG, false, "show help message for command ps");
    }

    @Override
    public String type() {
        return "ps";
    }

    @Override
    public String description() {
        return "Show server ports, if has port, show established connect with port, else show listening port.";
    }

    @Override
    public String shortDescription() {
        return "Show server ports.";
    }

    @Override
    public String help() {
        return super.help("ps [port]", options);
    }

    @Override
    public TelnetResponse telnet(Channel channel, String[] args) {
        CommandLine cmd = getCommand(options, args);
        boolean detail = !cmd.hasOption("c");
        if (cmd.hasOption(HELP_SHORT)) {
            return new TelnetResponse(help());
        } else if (cmd.getArgList().isEmpty()) {
            StringBuilder builder = new StringBuilder(1024);
            int size = 0;
            for (Server server : getServers()) {
                size += showConnection(server.getServerChannel(), detail, builder);
            }
            builder.append("count:").append(size).append(LINE);
            return new TelnetResponse(builder.toString());
        } else {
            String portArg = cmd.getArgList().get(0);
            try {
                int port = Integer.parseInt(portArg);
                Server server = getServer(port);
                if (server == null) {
                    return new TelnetResponse("Invalid port " + portArg);
                }
                return new TelnetResponse(showConnection(server.getServerChannel(), detail));
            } catch (NumberFormatException e) {
                return new TelnetResponse("Invalid port " + portArg);
            }
        }

    }

    /**
     * 显示连接信息
     * @param serverChannel 服务
     * @param detail 是否显示明细
     * @return 连接信息
     */
    protected String showConnection(final ServerChannel serverChannel, final boolean detail) {
        StringBuilder builder = new StringBuilder(detail ? 1024 : 100);
        int size = showConnection(serverChannel, detail, builder);
        builder.append("count:").append(size).append(LINE);
        return builder.toString();
    }

    /**
     * 显示连接信息
     * @param serverChannel 服务
     * @param detail 是否显示明细
     * @param builder 缓冲区
     * @return 连接信息
     */
    protected int showConnection(final ServerChannel serverChannel, final boolean detail, final StringBuilder builder) {
        List<Channel> channels = serverChannel.getChannels();
        if (detail) {
            for (Channel cn : channels) {
                builder.append(Channel.toString(cn)).append(LINE);
            }
        }
        return channels.size();
    }
}
