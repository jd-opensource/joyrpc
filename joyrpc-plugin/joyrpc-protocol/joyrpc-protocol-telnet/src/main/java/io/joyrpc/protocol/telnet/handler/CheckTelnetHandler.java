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
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.telnet.TelnetResponse;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @date: 2019/1/22
 */
public class CheckTelnetHandler extends AbstractTelnetHandler {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(CheckTelnetHandler.class);

    public CheckTelnetHandler() {
        options = new Options()
                .addOption(
                        Option.builder("i")
                                .longOpt("iface")
                                .hasArg()
                                .numberOfArgs(2)
                                .argName("interface-name,interface-id")
                                .desc("check for interface name and interface id")
                                .build())
                .addOption(HELP_SHORT, HELP_LONG, false, "show help message for command check");
    }

    @Override
    public String type() {
        return "check";
    }

    @Override
    public String description() {
        return "Check the specified interface id. ";
    }

    @Override
    public TelnetResponse telnet(Channel channel, String[] args) {
        String respMessage = "";
        if (args == null || args.length != 3) {
            respMessage = help();
        } else {
            CommandLine cmd = getCommand(options, args);
            if (cmd.hasOption(HELP_SHORT)) {
                respMessage = help();
            } else {
                String[] nameAndId;
                if (cmd.hasOption("i")) {
                    nameAndId = cmd.getOptionValues("i");
                } else {
                    nameAndId = new String[2];
                    System.arraycopy(cmd.getArgs(), 1, nameAndId, 0, 2);
                }
                if (nameAndId.length == 2 && nameAndId[0] != null && nameAndId[1] != null) {
                    String className = InvokerManager.getClassName(Long.valueOf(nameAndId[1]));
                    respMessage = nameAndId[0].equals(className) ? "1" : "0";
                }
            }

        }
        return new TelnetResponse(respMessage);
    }


}
