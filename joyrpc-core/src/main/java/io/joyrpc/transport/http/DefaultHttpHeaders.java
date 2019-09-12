package io.joyrpc.transport.http;

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

import java.util.HashMap;
import java.util.Map;

/**
 * @date: 2019/2/14
 */
public class DefaultHttpHeaders implements HttpHeaders {

    private Map<CharSequence, Object> params = new HashMap<>();

    @Override
    public Object get(CharSequence name) {
        return params.get(name);
    }

    @Override
    public Map<CharSequence, Object> getAll() {
        return params;
    }

    @Override
    public Object set(CharSequence name, Object value) {
        return params.put(name, value);
    }

    @Override
    public boolean add(CharSequence name, Object value) {
        return params.putIfAbsent(name, value) == null;
    }

    @Override
    public Object remove(String name) {
        return params.remove(name);
    }
}
