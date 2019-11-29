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

import io.joyrpc.util.SystemClock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话管理器
 *
 * @date: 2019/5/15
 */
public class SessionManager {
    /**
     * 会话
     */
    protected Map<Integer, Session> sessions = new ConcurrentHashMap<>();
    /**
     * 是否服务端会话
     */
    protected boolean server;

    /**
     * 构造函数
     *
     * @param server
     */
    public SessionManager(boolean server) {
        this.server = server;
    }

    /**
     * 获取会话
     *
     * @param sessionId
     * @return
     */
    public Session get(final int sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * 添加会话
     *
     * @param sessionId
     * @param session
     * @return
     */
    public Session putIfAbsent(final int sessionId, final Session session) {
        if (session == null) {
            return null;
        }
        session.setLastTime(SystemClock.now());
        return sessions.putIfAbsent(sessionId, session);
    }

    /**
     * 修改会话
     *
     * @param sessionId
     * @param session
     * @return
     */
    public Session put(int sessionId, Session session) {
        if (session == null) {
            return null;
        }
        session.setLastTime(SystemClock.now());
        return sessions.put(sessionId, session);
    }

    /**
     * 移除会话
     *
     * @param sessionId
     * @return
     */
    public Session remove(int sessionId) {
        return sessions.remove(sessionId);
    }

    /**
     * 心跳
     *
     * @param sessionId
     * @return
     */
    public boolean beat(final int sessionId) {
        if (!server) {
            return false;
        }
        Session meta = sessions.get(sessionId);
        if (meta == null) {
            return false;
        }
        meta.setLastTime(SystemClock.now());
        return true;
    }

    /**
     * 驱逐过期的
     */
    public void evict() {
        if (!server) {
            return;
        }
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpire());
    }
}
