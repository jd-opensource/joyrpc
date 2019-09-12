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

import io.joyrpc.constants.Version;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.telnet.TelnetResponse;

/**
 * @date: 2019/1/22
 */
public class VersionTelnetHandler extends AbstractTelnetHandler {

    @Override
    public String type() {
        return "version";
    }

    @Override
    public String description() {
        return "Usage:\tversion" + LINE + "Show version of current RPC.";
    }

    @Override
    public String shortDescription() {
        return "Show version of current RPC.";
    }

    @Override
    public TelnetResponse telnet(Channel channel, String[] args) {
        return new TelnetResponse("{\"BuildVersion\":\"" + Version.BUILD_VERSION + "\"," +
                "\"buildVersion\":\"" + Version.BUILD_VERSION_TIME + "\"}");
    }

    @Override
    public boolean newLine() {
        return false;
    }
}
