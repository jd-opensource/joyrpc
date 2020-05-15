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
        super(null, null, false, false, null, true);
    }

    public OverloadException(String message) {
        super(message, null, false, false, null, true);
    }

    public OverloadException(String message, int tps, boolean isServer) {
        super(message, null, false, false, null, true);
        this.tps = tps;
        this.isServer = isServer;
    }

    public OverloadException(String message, String errorCode, int tps, boolean isServer) {
        //不输出堆栈，减少限流造成的CPU过高
        super(message, null, false, false, errorCode, true);
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
