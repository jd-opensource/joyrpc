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

import io.joyrpc.codec.serialization.Json;
import io.joyrpc.invoker.InvokerManager;
import io.joyrpc.transport.Server;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.telnet.TelnetResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import static io.joyrpc.Plugin.JSON;

/**
 * @date: 2019/1/22
 */
public class BizThreadTelnetHandler extends AbstractTelnetHandler {

    public static final String CALLBACK = "callback";

    @Override
    public String type() {
        return "tp";
    }

    @Override
    public String description() {
        return "Usage:\ttp [port] [interval] [count]" + LINE
                + "Display the threadpool. " + LINE +
                "If no args, show all threadpool. " + LINE +
                "If has [port], show threadpool of port [port]. " + LINE +
                "If has [port] [interval] [count], show threadpool of port [port] time after time. " + LINE;
    }

    @Override
    public String shortDescription() {
        return "Display the threadpool statistics.";
    }

    @Override
    public TelnetResponse telnet(Channel channel, String[] args) {
        Json json = JSON.get();
        if (args == null || args.length == 0) {
            Map<String, Object> result = new HashMap<>(100);
            export(CALLBACK, InvokerManager.getCallbackThreadPool(), result);
            export(InvokerManager.getServers(), result);
            return new TelnetResponse(json.toJSONString(result));
        } else {

            int interval = 1000;
            int count = 1;
            if (args.length > 2) {
                count = Integer.parseInt(args[2]);
            }
            if (args.length > 1) {
                interval = Integer.parseInt(args[1]);
            }
            String port = args[0];
            if (interval < 100 || interval > 5000) {
                return new TelnetResponse("ERROR:interval must between 100 and 5000");
            }
            if (count < 1 || count > 60) {
                return new TelnetResponse("ERROR:count must between 1 and 60");
            }

            ThreadPoolExecutor pool = getThreadPool(port);
            if (pool != null) {
                HashMap<String, Map> map = new HashMap<>(1);
                StringBuilder builder = new StringBuilder(100);
                //循环
                for (int i = 0; i < count; i++) {
                    if (i > 0) {
                        //休息一段时间
                        try {
                            Thread.sleep(interval);
                        } catch (InterruptedException e) {
                            return new TelnetResponse(LINE);
                        }
                    }
                    if (!channel.isActive()) {
                        //通道被关闭了
                        return new TelnetResponse(LINE);
                    } else {
                        map.put(port, export(pool));
                        if (i != count - 1) {
                            //最后一个由循环外输出
                            builder.setLength(0);
                            builder.append(json.toJSONString(map)).append(LINE);
                            channel.send(builder.toString());
                        }
                    }
                }
                return new TelnetResponse(json.toJSONString(map));
            } else {
                return new TelnetResponse("ERROR:Not found threadpool:" + port);
            }

        }
    }

    /**
     * 获取线程池
     * @param name
     * @return
     */
    protected ThreadPoolExecutor getThreadPool(final String name) {
        if (Character.isDigit(name.charAt(0))) {
            Server server = InvokerManager.getServer(Integer.parseInt(name));
            return server == null ? null : server.getBizThreadPool();
        } else if (CALLBACK.equalsIgnoreCase(name)) {
            return InvokerManager.getCallbackThreadPool();
        }
        return null;
    }

    /**
     * 输出服务的线程池信息
     * @param servers
     * @param result
     */
    protected void export(final List<Server> servers, final Map<String, Object> result) {
        servers.forEach(o -> export(o, result));
    }

    /**
     * 输出服务的线程池信息
     * @param server
     * @param result
     */
    protected void export(final Server server, final Map<String, Object> result) {
        export(String.valueOf(server.getLocalAddress().getPort()), server.getBizThreadPool(), result);
    }

    /**
     * 线程池信息
     * @param name
     * @param executor
     * @param result
     */
    protected void export(final String name, final ThreadPoolExecutor executor, final Map<String, Object> result) {
        result.put(name, export(executor));
    }

    /**
     * 线程池信息
     * @param executor
     * @return
     */
    protected Map<String, Object> export(final ThreadPoolExecutor executor) {
        Map<String, Object> result = new HashMap(10);
        result.put("min", executor.getCorePoolSize());
        result.put("max", executor.getMaximumPoolSize());
        result.put("current", executor.getPoolSize());
        result.put("active", executor.getActiveCount());
        result.put("queue", executor.getQueue().size());
        return result;
    }
}
