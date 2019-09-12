package io.joyrpc.transport.message;

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

import io.joyrpc.transport.session.Session;

/**
 * @date: 2019/1/7
 */
public interface Message<H extends Header, T> {

    H getHeader();

    void setHeader(H header);

    T getPayLoad();

    void setPayLoad(T data);

    default int getMsgId() {
        H header = getHeader();
        return header == null ? -1 : header.getMsgId();
    }

    default void setMsgId(int id) {
        getHeader().setMsgId(id);
    }

    default int getMsgType() {
        H header = getHeader();
        return header == null ? -1 : header.getMsgType();
    }

    boolean isRequest();

    default int getSessionId() {
        H header = getHeader();
        return header == null ? -1 : header.getSessionId();
    }

    default void setSessionId(int sessionId) {
        getHeader().setSessionId(sessionId);
    }

    default Session getSession() {
        H header = getHeader();
        return header == null ? null : header.getSession();
    }

    default void setSession(Session session) {
        H header = getHeader();
        if (header != null) {
            header.setSession(session);
        }
    }

}

