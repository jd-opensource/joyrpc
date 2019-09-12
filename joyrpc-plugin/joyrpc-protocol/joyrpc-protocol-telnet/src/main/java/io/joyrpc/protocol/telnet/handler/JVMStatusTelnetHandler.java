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
import io.joyrpc.transport.telnet.TelnetResponse;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * @date: 2019/1/22
 */
public class JVMStatusTelnetHandler extends AbstractTelnetHandler {

    @Override
    public String type() {
        return "jvm";
    }

    @Override
    public String description() {
        return "Usage:\tjvm" + LINE + "Display current JVM's status. " + LINE;
    }

    @Override
    public String shortDescription() {
        return "Display current JVM's status. ";
    }

    @Override
    public TelnetResponse telnet(Channel channel, String[] args) {
        if (args != null && args.length != 0) {
            return new TelnetResponse(help());
        }
        StringBuilder sb = new StringBuilder(1024);
        //内存使用情况
        MemoryMXBean mmxb = ManagementFactory.getMemoryMXBean();
        long max = mmxb.getHeapMemoryUsage().getMax();
        long used = mmxb.getHeapMemoryUsage().getUsed();
        long init = mmxb.getHeapMemoryUsage().getInit();
        long commit = mmxb.getHeapMemoryUsage().getCommitted();
        sb.append("********Memory status******************").append(LINE);
        sb.append("Max JVM Heap Memory:").append(max / 1024 / 1024).append("M").append(LINE)
                .append("Used Heap Memory:").append(used / 1024 / 1024).append("M").append(LINE)
                .append("Init Heap Memory:").append(init / 1024 / 1024).append("M").append(LINE)
                .append("Commited Heap Memory:").append(commit / 1024 / 1024).append("M").append(LINE);

        sb.append("********Thread status********************").append(LINE);
        //线程数
        ThreadMXBean txmb = ManagementFactory.getThreadMXBean();
        sb.append("Peak thread count:").append(txmb.getPeakThreadCount()).append(LINE)
                .append("Thread count:").append(txmb.getThreadCount()).append(LINE);

        sb.append("********Runtime status******************").append(LINE);
        //启动入口参数
        RuntimeMXBean rmxb = ManagementFactory.getRuntimeMXBean();
        sb.append("InputArguments:[");
        for (String ia : rmxb.getInputArguments()) {
            sb.append(ia).append(",");
        }
        sb.deleteCharAt(sb.length() - 1).append("]").append(LINE);

        DateTimeFormatter dtf = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime localDateTime = Instant.ofEpochMilli(rmxb.getStartTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
        sb.append("JVM start time:").append(dtf.format(localDateTime)).append(LINE);

        return new TelnetResponse(sb.toString());
    }

}
