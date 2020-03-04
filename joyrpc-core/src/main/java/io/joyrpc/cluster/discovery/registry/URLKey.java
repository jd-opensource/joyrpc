package io.joyrpc.cluster.discovery.registry;

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

import io.joyrpc.extension.URL;

/**
 * URL和Key信息
 */
public class URLKey {
    /**
     * URL
     */
    protected final URL url;
    /**
     * Key
     */
    protected final String key;

    /**
     * 构造函数
     *
     * @param url
     * @param key
     */
    public URLKey(URL url, String key) {
        this.url = url;
        this.key = key;
    }

    public URL getUrl() {
        return url;
    }

    public String getKey() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        URLKey key1 = (URLKey) o;

        return key.equals(key1.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }
}
