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

import io.joyrpc.config.ConsumerConfig;
import io.joyrpc.constants.Constants;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.invoker.Exporter;
import io.joyrpc.invoker.InvokerManager;
import io.joyrpc.invoker.Refer;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.telnet.TelnetResponse;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.joyrpc.Plugin.JSON;

/**
 * @date: 2019/1/22
 */
public class ConfigTelnetHandler extends AbstractTelnetHandler {

    /**
     * slf4j Logger for this class
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ConfigTelnetHandler.class);

    public ConfigTelnetHandler() {
        options = new Options()
                .addOption(HELP_SHORT, HELP_LONG, false, "show help message for command config")
                .addOption(Option.builder("p")
                        .hasArg()
                        .numberOfArgs(2)
                        .argName("interface,alias")
                        .desc("Show provider configs")
                        .build())
                .addOption(Option.builder("c")
                        .hasArg()
                        .numberOfArgs(2)
                        .argName("interface,alias")
                        .desc("Show consumer configs")
                        .build())
                .addOption("r", false, "Show global context")
                .addOption("s", false, "Show all interface settings")
                .addOption("g", false, "Show global settings")
                .addOption("i", false, "Show interface id mapping");
        //.addOption("a", false, "Show all above");
    }

    @Override
    public String type() {
        return "conf";
    }

    @Override
    public String description() {
        return "Show config details.";
    }

    @Override
    public TelnetResponse telnet(Channel channel, String[] args) {
        if (args == null) {
            return new TelnetResponse(help());
        }
        String respMessage = "";
        CommandLine cmd = getCommand(options, args);
        if (cmd.hasOption("p")) {
            String[] ifaceAndAlias = cmd.getOptionValues("p");
            Exporter exporter = InvokerManager.getFirstExporter(ifaceAndAlias[0], ifaceAndAlias[1]);
            if (exporter != null) {
                respMessage = JSON.get().toJSONString(exporter.getConfig());
            }
        } else if (cmd.hasOption("c")) {
            String[] ifaceAndAlias = cmd.getOptionValues("c");
            List<Refer> refers = InvokerManager.getRefers();
            for (Refer refer : refers) {
                ConsumerConfig cc = refer.getConfig();
                if ((refer.getInterfaceName().equals(ifaceAndAlias[0])
                        || cc.getInterfaceClazz().equals(ifaceAndAlias[0]))
                        && cc.getAlias().equals(ifaceAndAlias[1])) {
                    respMessage = JSON.get().toJSONString(cc);
                    break;
                }
            }
        } else if (cmd.hasOption("g")) {
            respMessage = JSON.get().toJSONString(GlobalContext.getInterfaceConfig(Constants.GLOBAL_SETTING));
        } else if (cmd.hasOption("r")) {
            respMessage = JSON.get().toJSONString(GlobalContext.getContext());
        } else if (cmd.hasOption("s")) {
            respMessage = JSON.get().toJSONString(GlobalContext.getInterfaceConfigs());
        } else if (cmd.hasOption("i")) {
            Map<String, String> ifaceIds = new HashMap<>();
            InvokerManager.getInterfaceIds().forEach((k, v) -> ifaceIds.put(v, String.valueOf(k)));
            respMessage = JSON.get().toJSONString(ifaceIds);
        } else if (cmd.hasOption("h")) {
            respMessage = help();
        } else {
            respMessage = help();
        }
        return new TelnetResponse(respMessage);
    }
}
