package io.joyrpc.cluster.discovery.naming.http;

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

import io.joyrpc.cluster.discovery.backup.Backup;
import io.joyrpc.cluster.discovery.naming.AbstractRegistar;
import io.joyrpc.extension.URL;

import java.util.concurrent.ExecutorService;

/**
 * 基于HTTP的目录服务
 */
public class HttpRegistrar extends AbstractRegistar {

    public HttpRegistrar(String name, URL url, HttpProvider provider) {
        super(name, url, provider, 0, null, null);
    }

    public HttpRegistrar(String name, URL url, HttpProvider provider, long expireTime, Backup backup, ExecutorService executorService) {
        super(name, url, provider, expireTime, backup, executorService);
    }
}
