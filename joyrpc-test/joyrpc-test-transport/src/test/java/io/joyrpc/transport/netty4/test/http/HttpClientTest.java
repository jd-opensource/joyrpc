package io.joyrpc.transport.netty4.test.http;

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

import io.joyrpc.transport.http.*;
import io.joyrpc.transport.http.jdk.JdkHttpClient;

/**
 * @date: 2019/4/10
 */
public class HttpClientTest {

    public static void main(String[] args) throws Exception {
        HttpClient httpClient = new JdkHttpClient();
        HttpHeaders headers = new DefaultHttpHeaders();
        headers.set("login", System.currentTimeMillis());
        HttpRequest request = new HttpRequest("http://www.baidu.com", HttpMethod.GET);
        HttpResponse response = httpClient.execute(request);
        System.out.println(new String(response.content()));

    }
}
