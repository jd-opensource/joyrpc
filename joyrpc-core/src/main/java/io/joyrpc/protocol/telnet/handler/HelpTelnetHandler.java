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
import io.joyrpc.transport.telnet.TelnetHandler;
import io.joyrpc.transport.telnet.TelnetResponse;

import static io.joyrpc.Plugin.TELNET_HANDLER;

/**
 * @date: 2019/1/22
 */
public class HelpTelnetHandler implements TelnetHandler {

    @Override
    public String type() {
        return "help";
    }

    @Override
    public String help() {
        return "Usage:\thelp [cmd]" + LINE + "show all commands help!";
    }

    @Override
    public String description() {
        return "Show all commands help.";
    }

    @Override
    public TelnetResponse telnet(final Channel channel, final String[] args) {
        StringBuilder builder = new StringBuilder();
        if (args != null && args.length > 0) {
            TelnetHandler handler = TELNET_HANDLER.get(args[0]);
            if (handler != null) {
                builder.append(handler.help());
            } else {
                builder.append("Not found command : " + args[0]);
            }
        } else {
            builder.append("The supported command include:");
            TELNET_HANDLER.extensions().forEach(h -> builder.append(LINE).append(h.type()).append('\t').append(h.shortDescription()));
        }
        return new TelnetResponse(builder.toString());
    }


}
