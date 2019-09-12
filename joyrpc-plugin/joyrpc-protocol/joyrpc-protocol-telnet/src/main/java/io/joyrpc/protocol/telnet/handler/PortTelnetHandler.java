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

import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ServerChannel;
import io.joyrpc.transport.telnet.TelnetResponse;
import io.joyrpc.util.network.Ipv4;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.net.InetSocketAddress;
import java.util.List;

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
        if (cmd.hasOption(HELP_SHORT)) {
            return new TelnetResponse(help());
        }
        boolean detail = true;
        if (cmd.hasOption("c")) {
            detail = false;
        }
        try {
            if (args == null || args.length == 0 || (!detail && args.length == 1)) {
                StringBuilder sb = new StringBuilder();
                List<Channel> activeChannels = getActiveChannels(channel);
                sb.append("count:").append(activeChannels.size()).append(LINE);
                for (Channel cn : activeChannels) {
                    if (detail) {
                        sb.append(Channel.toString(cn)).append(LINE);
                    }
                }
                return new TelnetResponse(sb.toString());
            } else {
                int port = detail ? Integer.parseInt(args[0]) : Integer.parseInt(args[1]);
                StringBuilder sb = new StringBuilder();
                List<Channel> activeChannels = getActiveChannels(channel);
                int count = 0;
                for (Channel cn : activeChannels) {
                    InetSocketAddress address = cn.getLocalAddress();
                    if (Ipv4.toAddress(address).endsWith(":" + port)) {
                        if (detail) {
                            sb.append(Channel.toString(cn)).append(LINE);
                        }
                        count++;
                    }
                }
                sb.insert(0, "count:" + count + LINE);
                return new TelnetResponse(sb.toString());
            }
        } catch (Exception e) {
            return new TelnetResponse("Invalid port : " + args[0]);
        }
    }

    private List<Channel> getActiveChannels(Channel channel) {
        ServerChannel serverChannel = channel.getAttribute(Channel.SERVER_CHANNEL);
        return serverChannel.getChannels();
    }
}
