package io.joyrpc.transport.http.jdk;

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
import io.joyrpc.transport.http.*;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import static io.joyrpc.transport.http.HttpHeaders.Values.GZIP;
import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.net.HttpURLConnection.HTTP_OK;

/**
 * @date: 2019/4/10
 */
@Extension(value = "jdk", provider = "java")
public class JdkHttpClient implements HttpClient {

    protected static final int BUFFER_SIZE = 1024;

    @Override
    public HttpResponse execute(HttpRequest request) throws IOException {
        int status;
        HttpURLConnection conn = null;
        InputStream inputStream = null;
        byte[] content = null;
        try {
            java.net.URL url = new java.net.URL(request.getUri());
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(request.getHttpMethod().name());
            conn.setConnectTimeout(request.getConnectTimeout());
            conn.setReadTimeout(request.getSocketTimeout());
            HttpHeaders headers = request.headers();
            if (headers != null) {
                for (Map.Entry<CharSequence, Object> entry : headers.getAll().entrySet()) {
                    conn.setRequestProperty(entry.getKey().toString(), String.valueOf(entry.getValue()));
                }
            }
            conn.connect();

            status = conn.getResponseCode();
            if (status == HTTP_NOT_MODIFIED) {
                return new HttpResponse(status, prarseHeader(conn));
            }
            if (status == HTTP_OK) {
                inputStream = conn.getInputStream();
                //压缩处理
                String encoding = conn.getContentEncoding();
                if (encoding != null && !encoding.isEmpty()) {
                    if (GZIP.equals(encoding)) {
                        inputStream = new GZIPInputStream(inputStream);
                    } else if (HttpRequest.DEFLATE.equals(encoding)) {
                        inputStream = new InflaterInputStream(inputStream);
                    }
                }
            } else {
                inputStream = conn.getErrorStream();
            }
            //防止没有错误流
            if (inputStream != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFFER_SIZE);
                content = new byte[BUFFER_SIZE];
                int size;
                while ((size = inputStream.read(content)) != -1) {
                    if (size > 0) {
                        baos.write(content, 0, size);
                    }
                }
                content = baos.toByteArray();
            }
        } finally {
            close(inputStream);
            close(conn);
        }
        return new HttpResponse(status, prarseHeader(conn), content);
    }

    /**
     * 解析响应头
     *
     * @param conn
     * @return
     */
    protected HttpHeaders prarseHeader(final HttpURLConnection conn) {
        HttpHeaders httpHeaders = new DefaultHttpHeaders();
        Map<String, List<String>> headerFields = conn.getHeaderFields();
        if (headerFields != null && !headerFields.isEmpty()) {
            headerFields.forEach((k, v) -> {
                if (k != null && !k.isEmpty() && v != null && !v.isEmpty()) {
                    httpHeaders.set(k, v.get(0));
                }
            });
        }

        return httpHeaders;
    }

    /**
     * 关闭
     *
     * @param closeable
     */
    protected void close(final Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {

            }
        }
    }

    /**
     * 关闭链接
     *
     * @param connection
     */
    protected void close(final HttpURLConnection connection) {
        if (connection != null) {
            connection.disconnect();
        }
    }
}
