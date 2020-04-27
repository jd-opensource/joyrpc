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

import java.util.function.Function;

/**
 * 编解码异常
 */
public class CodecException extends ProtocolException {
    /**
     * 异常转换
     */
    public static Function<ClassNotFoundException, CodecException> CONVERTER = (e) -> new CodecException(e);

    private static final long serialVersionUID = -6892107995287057300L;

    public CodecException() {
        super(null, null, false, false, null, false);
    }

    public CodecException(String message) {
        super(message, null, false, false, null, false);
    }

    public CodecException(String message, String errorCode) {
        super(message, null, false, false, errorCode, false);
    }

    public CodecException(String message, Throwable cause) {
        super(message, cause, false, false, null, false);
    }

    public CodecException(String message, Throwable cause, String errorCode) {
        super(message, cause, false, false, errorCode, false);
    }

    public CodecException(Throwable cause) {
        super(cause == null ? null : cause.toString(), cause, false, false, null, false);
    }

    public CodecException(Throwable cause, String errorCode) {
        super(cause == null ? null : cause.toString(), cause, false, false, errorCode, false);
    }
}
