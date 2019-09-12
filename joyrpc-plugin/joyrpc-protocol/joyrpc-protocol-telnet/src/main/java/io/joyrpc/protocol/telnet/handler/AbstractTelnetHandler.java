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

import io.joyrpc.exception.ParserException;
import io.joyrpc.transport.telnet.TelnetHandler;
import org.apache.commons.cli.*;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @date: 2019/4/25
 */
public abstract class AbstractTelnetHandler implements TelnetHandler {

    protected static final String SUDO_ATTRIBUTE = "sudo";
    protected static final String SUDO_CRYPTO_TYPE = "DESede";

    protected Options options;

    @Override
    public String help() {
        if (options != null) {
            return help(type(), options);
        } else {
            return description();
        }
    }

    /**
     * 帮助
     *
     * @param cmdSyntax
     * @param options
     * @return
     */
    protected String help(String cmdSyntax, Options options) {
        HelpFormatter formatter = new HelpFormatter();
        StringWriter sw = new StringWriter();
        formatter.printHelp(new PrintWriter(sw), formatter.getWidth(), cmdSyntax, description(), options,
                formatter.getLeftPadding(), formatter.getDescPadding(), null, false);
        return sw.toString();
    }

    /**
     * 解析命令
     *
     * @param options
     * @param args
     * @return
     * @throws ParseException
     */
    protected CommandLine getCommand(Options options, String[] args) throws ParserException {
        CommandLineParser parser = new DefaultParser();
        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            throw new ParserException(e);
        }
    }

}
