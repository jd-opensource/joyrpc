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
 * @date: 2019/5/15
 */
public class SessionManager {

    protected Map<Integer, SessionMeta> sessionMeats = new ConcurrentHashMap<>();

    protected boolean isServer;

    public SessionManager() {
        this(false);
    }

    public SessionManager(boolean isServer) {
        this.isServer = isServer;
    }

    public Session get(int sessionId) {
        SessionMeta meta = sessionMeats.get(sessionId);
        return meta == null ? null : meta.session;
    }

    public Session putIfAbsent(int sessionId, Session session) {
        if (session == null) {
            return null;
        }
        SessionMeta meta = sessionMeats.putIfAbsent(sessionId, new SessionMeta(session));
        return meta == null ? null : meta.session;
    }

    public Session put(int sessionId, Session session) {
        if (session == null) {
            return null;
        }
        SessionMeta meta = sessionMeats.put(sessionId, new SessionMeta(session));
        return meta == null ? null : meta.session;
    }

    public Session remove(int sessionId) {
        SessionMeta meta = sessionMeats.remove(sessionId);
        return meta == null ? null : meta.session;
    }

    public boolean sessionbeat(int sessionId) {
        if (!isServer) {
            return false;
        }
        SessionMeta meta = sessionMeats.get(sessionId);
        if (meta == null) {
            return false;
        }
        meta.sessionbeat();
        return true;
    }

    public void clearExpires() {
        if (!isServer) {
            return;
        }
        sessionMeats.entrySet().removeIf(entry -> entry.getValue().isExpire());
    }

    /**
     * 会话元数据
     */
    protected static class SessionMeta {
        //会话
        protected Session session;

        //上次访问时间
        protected long lastTime;

        protected SessionMeta(Session session) {
            this.session = session;
            this.lastTime = SystemClock.now();
        }

        protected Session getSession() {
            return session;
        }

        protected void sessionbeat() {
            this.lastTime = SystemClock.now();
        }

        protected boolean isExpire() {
            return SystemClock.now() - lastTime > session.getTimeout();
        }
    }
}
