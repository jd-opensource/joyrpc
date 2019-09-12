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

import io.joyrpc.cluster.Shard;
import io.joyrpc.cluster.discovery.naming.ClusterProvider;
import io.joyrpc.exception.ProtocolException;
import io.joyrpc.exception.TransportException;
import io.joyrpc.extension.URL;
import io.joyrpc.transport.http.HttpClient;
import io.joyrpc.transport.http.HttpRequest;
import io.joyrpc.transport.http.HttpResponse;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import static io.joyrpc.Plugin.HTTP_CLIENT;

/**
 * 基于Http请求的目录服务
 */
public abstract class HttpProvider implements ClusterProvider {

    @Override
    public List<Shard> apply(final URL endpoint, final URL cluster) {
        HttpRequest request = createRequest(endpoint, cluster);
        HttpClient client = HTTP_CLIENT.get();
        try {
            HttpResponse response = client.execute(request);
            if (response.getStatus() != 200) {
                throw new IOException("http status " + response.getStatus());
            }
            return parse(response);
        } catch (MalformedURLException e) {
            throw new ProtocolException(String.format("Error occurs while %s %s", request.getHttpMethod(), request.getUri()), e);
        } catch (IOException e) {
            throw new TransportException(String.format("Error occurs while %s %s", request.getHttpMethod(), request.getUri()), e);
        }
    }

    /**
     * 构造请求URL
     *
     * @param endpoint
     * @param cluster
     * @return
     */
    protected abstract HttpRequest createRequest(final URL endpoint, final URL cluster);

    /**
     * 解析应答
     *
     * @param response
     * @return
     */
    protected abstract List<Shard> parse(final HttpResponse response);
}
