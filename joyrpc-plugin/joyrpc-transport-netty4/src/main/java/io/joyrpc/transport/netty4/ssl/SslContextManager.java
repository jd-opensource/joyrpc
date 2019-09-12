package io.joyrpc.transport.netty4.ssl;

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

import io.joyrpc.exception.SslException;
import io.joyrpc.extension.URL;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.util.ClassUtils.getCurrentClassLoader;
import static io.joyrpc.util.StringUtils.*;
import static io.netty.handler.ssl.ClientAuth.NONE;

/**
 * @date: 2019/8/13
 */
public class SslContextManager {

    /**
     * server端ssl上下文
     */
    private static final Map<String, SslContext> SERVER_SSL_CONTEXTS = new ConcurrentHashMap<>();
    /**
     * client端ssl上下文
     */
    private static final Map<String, SslContext> CLIENT_SSL_CONTEXTS = new ConcurrentHashMap<>();

    /**
     * 获取server端SslContext对象
     *
     * @param url
     * @return
     */
    public static SslContext getServerSslContext(URL url) throws SslException {
        if (!url.getBoolean(SSL_ENABLE)) {
            return null;
        }
        String pkPath = url.getString(SSL_PK_PATH);
        String caPath = url.getString(SSL_CA_PATH);
        //是否需要客户端认证
        ClientAuth clientAuth = ClientAuth.valueOf(url.getString(SSL_CLIENT_AUTH));
        //验证秘钥文件库和信任库path
        if (isEmpty(pkPath)) {
            throw new SslException("pkPath must not be empty.");
        }
        if (isEmpty(caPath) && clientAuth != NONE) {
            throw new SslException("caPath must not be empty.");
        }
        String key = pkPath + "#" + caPath;
        return SERVER_SSL_CONTEXTS.computeIfAbsent(key, k -> {
            try {
                //密钥管理器
                KeyManagerFactory kmf = getKeyManagerFactory(url);
                //信任库
                TrustManagerFactory tf = getTrustManagerFactory(url);
                //允许的协议版本
                String protocolsStr = url.getString(SSL_PROTOCOLS);
                String[] protocols = isEmpty(protocolsStr) ? null : split(protocolsStr, SEMICOLON_COMMA_WHITESPACE);
                //创建SslContenxt对象
                return SslContextBuilder.forServer(kmf).trustManager(tf).protocols(protocols).clientAuth(clientAuth).build();
            } catch (Throwable e) {
                throw new SslException("Failed to initialize the server-side SSLContext", e);
            }
        });
    }

    /**
     * 获取client端SslContext对象
     *
     * @param url
     * @return
     */
    public static SslContext getClientSslContext(URL url) throws SslException {
        if (!url.getBoolean(SSL_ENABLE)) {
            return null;
        }
        String pkPath = url.getString(SSL_PK_PATH);
        String caPath = url.getString(SSL_CA_PATH);
        //验证秘钥文件库和信任库path
        if (isEmpty(caPath)) {
            throw new SslException("caPath must not be empty.");
        }
        String key = pkPath + "#" + caPath;
        return CLIENT_SSL_CONTEXTS.computeIfAbsent(key, k -> {
            try {
                //密钥管理器
                KeyManagerFactory kmf = getKeyManagerFactory(url);
                //信任库
                TrustManagerFactory tf = getTrustManagerFactory(url);
                //允许的协议版本
                String protocolsStr = url.getString(SSL_PROTOCOLS);
                String[] protocols = isEmpty(protocolsStr) ? null : split(protocolsStr, SEMICOLON_COMMA_WHITESPACE);
                //创建SslContenxt对象
                return SslContextBuilder.forClient().keyManager(kmf).trustManager(tf).protocols(protocols).build();
            } catch (Throwable e) {
                throw new SslException("Failed to connect " + url.toString(false, false) +
                        ". caused by:failed to initialize the client-side SSLContext", e);
            }
        });
    }

    /**
     * 选择证书证明自己的身份
     *
     * @param url
     * @param url
     * @return
     * @throws Exception
     */
    protected static KeyManagerFactory getKeyManagerFactory(final URL url) throws Exception {
        String pkPath = url.getString(SSL_PK_PATH);
        if (isEmpty(pkPath)) {
            return null;
        }
        String password = url.getString(SSL_PASSWORD);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(url.getString(SSL_CERTIFICATE));
        KeyStore tks = getKeyStore(pkPath, password, url.getString(SSL_KEYSTORE));
        kmf.init(tks, password.toCharArray());
        return kmf;
    }

    /**
     * 决定是否信任对方的证书
     *
     * @param url
     * @return
     * @throws Exception
     */
    protected static TrustManagerFactory getTrustManagerFactory(final URL url) throws Exception {
        String caPath = url.getString(SSL_CA_PATH);
        if (isEmpty(caPath)) {
            return null;
        }
        TrustManagerFactory tf = TrustManagerFactory.getInstance(url.getString(SSL_CERTIFICATE));
        KeyStore tks = getKeyStore(caPath, url.getString(SSL_PASSWORD), url.getString(SSL_KEYSTORE));
        tf.init(tks);
        return tf;
    }

    /**
     * 获取KeyStore
     *
     * @param password
     * @param path
     * @param keyStore
     * @return
     * @throws Exception
     */
    protected static KeyStore getKeyStore(final String path, final String password, final String keyStore) throws Exception {
        KeyStore tks = KeyStore.getInstance(keyStore);
        File file = new File(path);
        InputStream in = file.exists() ? new FileInputStream(path) : getCurrentClassLoader().getResourceAsStream(path);
        if (in == null) {
            throw new FileNotFoundException("file is not found. " + path);
        }
        try {
            tks.load(in, password.toCharArray());
        } finally {
            in.close();
        }
        return tks;
    }

}
