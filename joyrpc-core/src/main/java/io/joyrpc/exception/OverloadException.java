package io.joyrpc.exception;

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

/**
 * 过载异常
 */
public class OverloadException extends RejectException {

    private static final long serialVersionUID = 8092542592823750863L;

    //期望降到的目标TPS
    protected int tps;

    protected boolean isServer;

    public OverloadException() {
    }

    public OverloadException(String message) {
        super(message);
    }

    public OverloadException(String message, String errorCode) {
        super(message, errorCode);
    }

    public OverloadException(String message, Throwable cause) {
        super(message, cause);
    }

    public OverloadException(String message, Throwable cause, String errorCode) {
        super(message, cause, errorCode);
    }

    public OverloadException(Throwable cause) {
        super(cause);
    }

    public OverloadException(Throwable cause, String errorCode) {
        super(cause, errorCode);
    }

    public OverloadException(int tps, boolean isServer) {
        this.tps = tps;
        this.isServer = isServer;
    }

    public OverloadException(String message, int tps, boolean isServer) {
        super(message);
        this.tps = tps;
        this.isServer = isServer;
    }

    public OverloadException(String message, String errorCode, int tps, boolean isServer) {
        super(message, errorCode);
        this.tps = tps;
        this.isServer = isServer;
    }

    public OverloadException(String message, Throwable cause, int tps, boolean isServer) {
        super(message, cause);
        this.tps = tps;
        this.isServer = isServer;
    }

    public OverloadException(String message, Throwable cause, String errorCode, int tps, boolean isServer) {
        super(message, cause, errorCode);
        this.tps = tps;
        this.isServer = isServer;
    }

    public OverloadException(Throwable cause, int tps, boolean isServer) {
        super(cause);
        this.tps = tps;
        this.isServer = isServer;
    }

    public OverloadException(Throwable cause, String errorCode, int tps, boolean isServer) {
        super(cause, errorCode);
        this.tps = tps;
        this.isServer = isServer;
    }

    public int getTps() {
        return tps;
    }

    public void setTps(int tps) {
        this.tps = tps;
    }

    public boolean isServer() {
        return isServer;
    }

    public void setServer(boolean server) {
        isServer = server;
    }
}
