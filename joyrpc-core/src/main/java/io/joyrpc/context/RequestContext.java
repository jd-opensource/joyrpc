package io.joyrpc.context;

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


import io.joyrpc.constants.Constants;
import io.joyrpc.extension.MapParametric;
import io.joyrpc.extension.Parametric;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

import static io.joyrpc.constants.Constants.HIDDEN_KEY_SESSION;
import static io.joyrpc.constants.Constants.HIDE_KEY_PREFIX;

/**
 * Title: 请求上下文，用于隐式传参 <br>
 * <p/>
 * Description: 注意：RequestContext是一个临时状态记录器，当接收到RPC请求，或发起RPC请求时，RequestContext的状态都会变化。<br>
 * 比如：A调B，B再调C，则B机器上，在B调C之前，RequestContext记录的是A调B的信息，在B调C之后，RequestContext记录的是B调C的信息。<br>
 * <p/>
 *
 * @date: 26/2/2019
 */
public class RequestContext {

    /**
     * The constant LOCAL.
     */
    // todo InheritableThreadLocal 有问题
    protected static final ThreadLocal<RequestContext> LOCAL = new ThreadLocal() {
        @Override
        protected RequestContext initialValue() {
            return new RequestContext();
        }
    };

    public static final Predicate<String> NONE_INTERNAL_KEY = (o) -> o == null || o.charAt(0) != Constants.INTERNAL_KEY_PREFIX;

    public static final Predicate<String> INTERNAL_KEY = NONE_INTERNAL_KEY.negate();

    public static final Predicate<String> NONE_HIDDEN_KEY = (o) -> o == null || o.charAt(0) != Constants.HIDE_KEY_PREFIX;

    public static final Predicate<String> HIDDEN_KEY = NONE_HIDDEN_KEY.negate();

    public static final Predicate<String> VALID_KEY = (o) -> {
        if (o == null) {
            return false;
        }
        char ch = o.charAt(0);
        return ch != Constants.HIDE_KEY_PREFIX && ch != Constants.INTERNAL_KEY_PREFIX;
    };

    public static final Function<String, String> INTERNAL_TO_HIDDEN = (k) -> HIDE_KEY_PREFIX + k.substring(1);

    /**
     * The Local address.
     */
    protected InetSocketAddress localAddress;

    /**
     * The Remote address.
     */
    protected InetSocketAddress remoteAddress;

    /**
     * The Provider side.
     */
    protected boolean provider;

    /**
     * 异步调用
     */
    protected boolean async;

    /**
     * 当前组
     */
    protected String alias;

    /**
     * 异步调用的future
     */
    protected CompletableFuture<?> future;

    /**
     * The Attachments.
     */
    protected final Map<String, Object> attachments = new HashMap<>();
    /**
     * 会话
     */
    protected Map<String, Object> session;

    /**
     * 构造函数
     */
    public RequestContext() {
    }

    /**
     * 构造函数
     *
     * @param session
     */
    public RequestContext(Map<String, Object> session) {
        this.session = session;
    }

    /**
     * Is provider side.
     *
     * @return the boolean
     */
    public boolean isProvider() {
        return provider;
    }

    /**
     * Is consumer side.
     *
     * @return the boolean
     */
    public boolean isConsumer() {
        return !provider;
    }

    /**
     * Gets alias.
     *
     * @return the alias
     */
    public String getAlias() {
        return alias;
    }

    /**
     * Sets alias.
     *
     * @param alias the alias
     * @return RpcContext
     */
    public RequestContext setAlias(String alias) {
        this.alias = alias;
        return this;
    }

    /**
     * Sets provider side.
     *
     * @param provider the is provider side
     * @return the provider side
     */
    public RequestContext setProvider(final boolean provider) {
        this.provider = provider;
        return this;
    }

    /**
     * 是否异步调用
     *
     * @return
     */
    public boolean isAsync() {
        return async;
    }

    /**
     * 设置异步调用
     *
     * @param async
     * @return
     */
    public RequestContext setAsync(final boolean async) {
        this.async = async;
        return this;
    }

    /**
     * 获取异步future
     *
     * @param <T>
     * @return
     */
    public <T> CompletableFuture<T> getFuture() {
        return (CompletableFuture<T>) future;
    }

    /**
     * 设置异步future
     *
     * @param future
     * @return
     */
    public RequestContext setFuture(CompletableFuture<?> future) {
        this.future = future;
        return this;
    }

    /**
     * set local address.
     *
     * @param address the address
     * @return context local address
     */
    public RequestContext setLocalAddress(final InetSocketAddress address) {
        this.localAddress = address;
        return this;
    }

    /**
     * 本地地址InetSocketAddress
     *
     * @return local address
     */
    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    /**
     * set remote address.
     *
     * @param address the address
     * @return context remote address
     */
    public RequestContext setRemoteAddress(final InetSocketAddress address) {
        this.remoteAddress = address;
        return this;
    }

    /**
     * 远程地址InetSocketAddress
     *
     * @return remote address
     */
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * get attachment.
     *
     * @param key the key
     * @return attachment attachment
     */
    public <T> T getAttachment(final String key) {
        return key == null ? null : (T) attachments.get(key);
    }

    /**
     * 设置扩展属性
     *
     * @param key   the key
     * @param value the value
     * @return context attachment
     */
    public RequestContext setAttachment(final String key, final Object value) {
        return setAttachment(key, value, NONE_INTERNAL_KEY);
    }

    /**
     * 设置扩展属性
     *
     * @param key       the key
     * @param value     the value
     * @param predicate 判断key合法性
     * @return context attachment
     */
    public RequestContext setAttachment(final String key, final Object value, final Predicate<String> predicate) {
        if (key == null) {
            return this;
        } else if (predicate != null && !predicate.test(key)) {
            throw new IllegalArgumentException("key is illegal, the key is " + key);
        } else if (value == null) {
            attachments.remove(key);
        } else {
            attachments.put(key, value);
        }
        return this;
    }

    /**
     * remove attachment.
     *
     * @param key the key
     * @return context rpc context
     */
    public RequestContext removeAttachment(final String key) {
        attachments.remove(key);
        return this;
    }

    /**
     * get attachments.
     *
     * @return attachments attachments
     */
    public Map<String, Object> getAttachments() {
        return attachments;
    }

    /**
     * 设置扩展属性，过滤掉无效的属性
     *
     * @param attachments the attachments
     * @return context attachments
     */
    public <T extends Object> RequestContext setAttachments(final Map<String, T> attachments) {
        return setAttachments(attachments, VALID_KEY);
    }

    /**
     * 设置扩展属性，过滤掉无效的属性
     *
     * @param attachments the attachments
     * @param predicate   验证key合法性
     * @return context attachments
     */
    public <T extends Object> RequestContext setAttachments(final Map<String, T> attachments, final Predicate<String> predicate) {
        if (attachments != null && !attachments.isEmpty()) {
            Map<String, Object> session = (Map<String, Object>) attachments.remove(HIDDEN_KEY_SESSION);
            if (session != null) {
                setSession(session);
            }
            String key;
            for (Map.Entry<String, T> entry : attachments.entrySet()) {
                key = entry.getKey();
                if (predicate == null || predicate.test(key)) {
                    this.attachments.put(key, entry.getValue());
                }
            }
        }
        return this;
    }

    /**
     * 把扩展属性作为参数
     *
     * @return
     */
    public Parametric asParametric() {
        return new MapParametric(attachments);
    }

    /**
     * 设置Session的属性值<br>
     * 注：Session是一种特殊的 隐式传参，客户端线程不会主动删除，需要用户自己写代码清理
     *
     * @param key   属性
     * @param value 值
     * @return 本对象
     * @see #getSession
     * @see #clearSession()
     */
    public RequestContext setSession(final String key, final Object value) {
        if (session == null) {
            session = new HashMap<>();
        }
        if (key != null) {
            if (value == null) {
                session.remove(key);
            } else {
                session.put(key, value);
            }
        }
        return this;
    }

    /**
     * 查询Session的属性值<br>
     * 注：Session是一种特殊的 隐式传参，客户端不会主动删除，需要用户自己写代码清理
     *
     * @param key 属性
     * @return 值
     * @see #setSession
     * @see #clearSession()
     */
    public Object getSession(final String key) {
        return session == null ? null : session.get(key);
    }

    /**
     * 设置session<br>
     * 注：Session是一种特殊的 隐式传参，客户端不会主动删除，需要用户自己写代码清理
     *
     * @param session session属性map
     * @return 本对象
     */
    public RequestContext setSession(final Map<String, Object> session) {
        this.session = session;
        return this;
    }

    /**
     * 得到session<br>
     * 注：Session是一种特殊的 隐式传参，客户端不会主动删除，需要用户自己写代码清理
     *
     * @return session属性map
     */
    public Map<String, Object> getSession() {
        return session;
    }

    /**
     * 删除session
     *
     * @return 本对象
     */
    public RequestContext clearSession() {
        session = null;
        return this;
    }

    /**
     * 清除扩展属性.
     */
    public RequestContext clearAttachments() {
        provider = false;
        remoteAddress = null;
        localAddress = null;
        future = null;
        attachments.clear();
        return this;
    }

    /**
     * 获取上下文.
     *
     * @return context context
     */
    public static RequestContext getContext() {
        return LOCAL.get();
    }

    /**
     * 删除上下文.
     */
    public static void remove() {
        LOCAL.remove();
    }

    /**
     * 设置上下文
     *
     * @param context
     */
    public static void restore(final RequestContext context) {
        LOCAL.set(context);
    }

}
