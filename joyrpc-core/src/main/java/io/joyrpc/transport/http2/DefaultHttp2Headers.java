package io.joyrpc.transport.http2;

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

import io.joyrpc.transport.http.DefaultHttpHeaders;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

/**
 * 默认Http2头
 */
public class DefaultHttp2Headers extends DefaultHttpHeaders implements Http2Headers {

    public DefaultHttp2Headers() {
    }

    public DefaultHttp2Headers(final Iterable<Map.Entry<CharSequence, CharSequence>> headers) {
        if (headers != null) {
            headers.forEach(t -> {
                try {
                    params.put(t.getKey().toString(), URLDecoder.decode(t.getValue().toString(), "UTF-8"));
                } catch (UnsupportedEncodingException ignored) {
                }
            });
        }
    }
}
