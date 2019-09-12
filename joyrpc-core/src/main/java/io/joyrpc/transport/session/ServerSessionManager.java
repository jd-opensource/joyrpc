package io.joyrpc.transport.session;

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


import io.joyrpc.transport.channel.ServerChannel;
import io.joyrpc.util.Shutdown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 服务端会话管理器
 */
public class ServerSessionManager {

    private final static Logger logger = LoggerFactory.getLogger(ServerSessionManager.class);

    /**
     * 会话检测周期
     */
    protected final long checkScheduleTime = 10000;

    protected Set<ServerChannel> serverChannels = new HashSet<>();

    private ServerSessionManager() {
        // 启动会话管理器线程
        Thread thread = new Thread(() -> {
            while (!Shutdown.isShutdown()) {
                try {
                    Thread.sleep(checkScheduleTime);
                    if (!Shutdown.isShutdown()) {
                        serverChannels.forEach(sch -> sch.getChannels().forEach(ch -> ch.getSessionManager().clearExpires()));
                    }
                } catch (InterruptedException e) {
                    logger.info("session checker is interrupted");
                    break;
                }
            }
        }, "session-checker");
        thread.setDaemon(true);
        thread.start();
    }

    public void add(ServerChannel serverChannel) {
        serverChannels.add(serverChannel);
    }

    public void remove(ServerChannel serverChannel) {
        serverChannels.remove(serverChannel);
    }

    /**
     * 获取单例
     *
     * @return
     */
    public static ServerSessionManager getInstance() {
        //延迟加载
        return ServerSessionManagerLazy.INSTANCE;
    }

    /**
     * 延迟加载会话
     */
    protected static class ServerSessionManagerLazy {

        protected static ServerSessionManager INSTANCE = new ServerSessionManager();
    }
}
