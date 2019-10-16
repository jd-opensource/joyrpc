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
import io.joyrpc.util.Daemon;
import io.joyrpc.util.Shutdown;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 服务端会话管理器
 */
public class ServerSessionManager {

    /**
     * 会话检测周期
     */
    protected long interval = 10000;

    /**
     * 对应服务端的ServerTransport
     */
    protected Set<ServerChannel> channels = new CopyOnWriteArraySet<>();

    /**
     * 服务端会话管理器
     */
    protected ServerSessionManager() {
        Daemon daemon = new Daemon("session-checker",
                () -> channels.forEach(sch -> sch.getChannels().forEach(ch -> ch.getSessionManager().clearExpires())),
                interval, () -> !Shutdown.isShutdown());
        // 启动会话管理器线程
        daemon.start();
    }

    /**
     * 添加通道
     *
     * @param channel
     */
    public void add(final ServerChannel channel) {
        if (channel != null) {
            channels.add(channel);
        }
    }

    /**
     * 移除通道
     *
     * @param channel
     */
    public void remove(final ServerChannel channel) {
        if (channel != null) {
            channels.remove(channel);
        }
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
