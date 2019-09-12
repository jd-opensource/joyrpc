package io.joyrpc.transport.channel;

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

import io.joyrpc.extension.Extension;
import io.joyrpc.extension.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @date: 2019/2/21
 */
@Extension(value = "unshared", singleton = true)
public class UnsharedChannelManagerFactory implements ChannelManagerFactory {

    private static final Logger logger = LoggerFactory.getLogger(UnsharedChannelManagerFactory.class);

    private Map<String, UnsharedChannelManager> managers = new ConcurrentHashMap<>();

    @Override
    public ChannelManager getChannelManager(URL url) {
        return managers.computeIfAbsent(
                url.toString(false, false),
                o -> new UnsharedChannelManager(url)
        );
    }
}
