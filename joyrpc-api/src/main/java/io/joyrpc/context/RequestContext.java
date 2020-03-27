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


import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

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
     * 内部使用的key前缀，防止和自定义key冲突
     */
    protected static final char INTERNAL_KEY_PREFIX = '_';
    /**
     * 隐藏的key前缀，隐藏的key只能在filter里拿到，在RpcContext里拿不到，不过可以设置
     */
    protected static final char HIDE_KEY_PREFIX = '.';
    /**
     * 隐藏属性的key：session
     */
    protected static final String HIDDEN_KEY_SESSION = HIDE_KEY_PREFIX + "session";

    public static final String INTERNAL_KEY_TRACE = INTERNAL_KEY_PREFIX + "trace";

    /**
     * The constant LOCAL.
     */
    // todo InheritableThreadLocal 有问题
    protected static final ThreadLocal<RequestContext> LOCAL = ThreadLocal.withInitial(() -> new RequestContext());

    public static final Predicate<String> NONE_INTERNAL_KEY = (o) -> o == null || o.charAt(0) != INTERNAL_KEY_PREFIX;

    public static final Predicate<String> ALL = (o) -> true;

    public static final Predicate<String> INTERNAL_KEY = NONE_INTERNAL_KEY.negate();

    public static final Predicate<String> NONE_HIDDEN_KEY = (o) -> o == null || o.charAt(0) != HIDE_KEY_PREFIX;

    public static final Predicate<String> HIDDEN_KEY = NONE_HIDDEN_KEY.negate();

    public static final Predicate<String> VALID_KEY = (o) -> {
        if (o == null) {
            return false;
        }
        char ch = o.charAt(0);
        return ch != HIDE_KEY_PREFIX && ch != INTERNAL_KEY_PREFIX;
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
     * 所有参数的合计
     */
    protected Map<String, Object> attachments;
    /**
     * 一起调用生效，自动清理
     */
    protected Map<String, Object> requests;
    /**
     * 在本地JVM持续调用生效，需要手动清理
     */
    protected Map<String, Object> sessions;
    /**
     * 调用链生效，需要手动清理
     */
    protected Map<String, Object> traces;
    /**
     * 服务端收到的请求，会在调用链处理完毕后清理掉
     */
    protected Map<String, Object> callers;
    /**
     * 类型
     */
    protected ContentType type;
    /**
     * 是否脏，大部分情况是写入参数并不读取，可以延迟进行合并
     */
    protected volatile boolean dirty;

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
     * @param async 异步标识
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
        if (key == null) {
            return null;
        }
        //延迟合并
        if (dirty) {
            compute();
            dirty = false;
        }
        return attachments == null ? null : (T) attachments.get(key);
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
    protected RequestContext setAttachment(final String key, final Object value, final Predicate<String> predicate) {
        if (key == null) {
            return this;
        } else if (value == null) {
            removeAttachment(key);
        } else if (predicate != null && !predicate.test(key)) {
            throw new IllegalArgumentException("key is illegal, the key is " + key);
        } else {
            if (requests == null) {
                requests = new HashMap<>();
            }
            requests.put(key, value);
            dirty = true;
        }
        return this;
    }

    /**
     * 设置扩展属性，过滤掉无效的属性
     *
     * @param attachments the attachments
     * @return context attachments
     */
    public <T extends Object> RequestContext setAttachments(final Map<String, T> attachments) {
        return setAttachments(attachments, NONE_INTERNAL_KEY);
    }

    /**
     * 设置扩展属性，过滤掉无效的属性
     *
     * @param context   上下文
     * @param predicate 验证key合法性
     * @return 上下文
     */
    protected <T extends Object> RequestContext setAttachments(final Map<String, T> context, final Predicate<String> predicate) {
        if (context != null && !context.isEmpty()) {
            String key;
            for (Map.Entry<String, T> entry : context.entrySet()) {
                key = entry.getKey();
                if (predicate == null || predicate.test(key)) {
                    if (requests == null) {
                        requests = new HashMap<>();
                    }
                    requests.put(key, entry.getValue());
                }
            }
            dirty = true;
        }
        return this;
    }

    /**
     * remove attachment.
     *
     * @param key the key
     * @return 原有的值
     */
    public Object removeAttachment(final String key) {
        Object result = null;
        if (requests != null && key != null) {
            result = requests.remove(key);
            if (result != null) {
                dirty = true;
            }
        }
        return result;
    }

    /**
     * get attachments.
     *
     * @return attachments attachments
     */
    public Map<String, Object> getAttachments() {
        if (dirty) {
            compute();
            dirty = false;
        }
        return attachments == null ? null : Collections.unmodifiableMap(attachments);
    }

    public Map<String, Object> getRequests() {
        return requests == null ? null : Collections.unmodifiableMap(requests);
    }

    /**
     * 得到会话参数<br>
     *
     * @return session属性map
     */
    public Map<String, Object> getSessions() {
        return sessions == null ? null : Collections.unmodifiableMap(sessions);
    }

    /**
     * 设置会话参数
     *
     * @param session session属性map
     * @return 本对象
     */
    public RequestContext setSessions(final Map<String, Object> session) {
        return setSessions(session, NONE_INTERNAL_KEY);
    }

    /**
     * 设置会话参数
     *
     * @param context   上下文
     * @param predicate 验证key合法性
     * @return 上下文
     */
    protected RequestContext setSessions(final Map<String, Object> context, final Predicate<String> predicate) {
        if (context != null && !context.isEmpty()) {
            String key;
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                key = entry.getKey();
                if (predicate == null || predicate.test(key)) {
                    if (sessions == null) {
                        sessions = new HashMap<>();
                    }
                    sessions.put(key, entry.getValue());
                }
            }
            dirty = true;
        }
        return this;
    }

    /**
     * 设置会话参数
     *
     * @param key   属性
     * @param value 值
     * @return 本对象
     */
    public RequestContext setSession(final String key, final Object value) {
        return setSession(key, value, NONE_INTERNAL_KEY);
    }

    /**
     * 设置会话参数
     *
     * @param key       the key
     * @param value     the value
     * @param predicate 判断key合法性
     * @return 本对象
     */
    protected RequestContext setSession(final String key, final Object value, final Predicate<String> predicate) {
        if (key == null) {
            return this;
        } else if (value == null) {
            removeSession(key);
        } else if (predicate != null && !predicate.test(key)) {
            throw new IllegalArgumentException("key is illegal, the key is " + key);
        } else {
            if (sessions == null) {
                sessions = new HashMap<>();
            }
            sessions.put(key, value);
            dirty = true;
        }
        return this;
    }

    /**
     * 查询Session的属性值<br>
     * 注：Session是一种特殊的 隐式传参，客户端不会主动删除，需要用户自己写代码清理
     *
     * @param key 属性
     * @return 值
     * @see #setSessions
     * @see #clearSession()
     */
    public Object getSession(final String key) {
        return sessions == null || key == null ? null : sessions.get(key);
    }

    /**
     * 删除会话上下文
     *
     * @param key 属性
     * @return 原有的值
     */
    public Object removeSession(final String key) {
        Object result = null;
        if (key != null && sessions != null) {
            result = sessions.remove(key);
            if (result != null) {
                dirty = true;
            }
        }
        return result;
    }

    /**
     * 获取调用链参数
     *
     * @return session属性map
     */
    public Map<String, Object> getTraces() {
        return traces == null ? null : Collections.unmodifiableMap(traces);
    }

    /**
     * 获取调用者参数
     *
     * @return
     */
    public Map<String, Object> getCallers() {
        return callers == null ? null : Collections.unmodifiableMap(callers);
    }

    /**
     * 设置调用链参数
     *
     * @param traces session属性map
     * @return 本对象
     */
    protected RequestContext setTraces(final Map<String, Object> traces) {
        this.traces = traces;
        dirty = true;
        return this;
    }

    /**
     * 设置调用链跟踪属性
     *
     * @param key   属性
     * @param value 值
     * @return 本对象
     */
    public RequestContext setTrace(final String key, final Object value) {
        return setTrace(key, value, NONE_INTERNAL_KEY);
    }

    /**
     * 设置调用链跟踪属性
     *
     * @param key       the key
     * @param value     the value
     * @param predicate 判断key合法性
     * @return 本对象
     */
    protected RequestContext setTrace(final String key, final Object value, final Predicate<String> predicate) {
        if (key == null) {
            return this;
        } else if (value == null) {
            removeTrace(key);
        } else if (predicate != null && !predicate.test(key)) {
            throw new IllegalArgumentException("key is illegal, the key is " + key);
        } else {
            if (traces == null) {
                traces = new HashMap<>();
            }
            traces.put(key, value);
            dirty = true;
        }
        return this;
    }

    /**
     * 删除调用链参数
     *
     * @param key 属性
     * @return 原有的值
     */
    public Object removeTrace(final String key) {
        Object result = null;
        if (key != null && traces != null) {
            result = traces.remove(key);
            if (result != null) {
                dirty = true;
            }
        }
        return result;
    }

    /**
     * 设置调用者
     *
     * @param callers 调用者参数
     * @return 本对象
     */
    protected RequestContext setCallers(final Map<String, Object> callers) {
        this.callers = callers;
        dirty = true;
        return this;
    }

    /**
     * 清理全部参数
     *
     * @return
     */
    protected RequestContext clear() {
        provider = false;
        remoteAddress = null;
        localAddress = null;
        future = null;
        requests = null;
        sessions = null;
        traces = null;
        callers = null;
        attachments = null;
        dirty = false;
        return this;
    }

    /**
     * 清除当前调用参数
     */
    public RequestContext clearAttachments() {
        remoteAddress = null;
        localAddress = null;
        attachments = null;
        dirty = true;
        return this;
    }

    /**
     * 清理会话参数
     *
     * @return 本对象
     */
    public RequestContext clearSession() {
        sessions = null;
        dirty = true;
        return this;
    }

    /**
     * 清除调用链参数
     */
    public RequestContext clearTrace() {
        traces = null;
        dirty = true;
        return this;
    }

    /**
     * 合并
     *
     * @param target 任务
     * @param type   类型
     */
    protected void merge(final Map<String, Object> target, final ContentType type) {
        if (attachments == null) {
            if (target == null) {
                return;
            }
            attachments = target;
            this.type = type;
        } else if (target == null) {
            return;
        }
        if (this.type == ContentType.MERGE) {
            attachments.putAll(target);
        } else if (this.type != type) {
            this.type = ContentType.MERGE;
            Map<String, Object> result = new HashMap<>(attachments.size() + target.size());
            result.putAll(attachments);
            result.putAll(target);
            attachments = result;
        }
    }

    /**
     * 计算参数
     */
    protected void compute() {
        merge(callers, ContentType.ONLY_CALLER);
        merge(requests, ContentType.ONLY_REQUEST);
        merge(sessions, ContentType.ONLY_SESSION);
        merge(traces, ContentType.ONLY_TRACE);
    }

    /**
     * 上下文修改器
     */
    public static class InnerContext {

        /**
         * 上下文
         */
        protected RequestContext context;

        /**
         * 构造函数
         *
         * @param context 请求上下文
         */
        public InnerContext(RequestContext context) {
            this.context = context;
        }

        public Map<String, Object> getCaller() {
            return context.callers;
        }

        public Map<String, Object> getTraces() {
            return context.traces;
        }

        public Map<String, Object> getSessions() {
            return context.sessions;
        }

        public Map<String, Object> getRequests() {
            return context.requests;
        }

        /**
         * 设置调用者
         *
         * @param callers 调用者参数
         */
        public void setCallers(final Map<String, Object> callers) {
            context.setCallers(callers);
        }

        /**
         * 设置调用链参数
         *
         * @param traces session属性map
         */
        public void setTraces(final Map<String, Object> traces) {
            context.setTraces(traces);
        }

        /**
         * 设置会话参数
         *
         * @param sessions 会话参数
         */
        public void setSessions(final Map<String, Object> sessions) {
            context.sessions = sessions;
            context.dirty = true;
        }

        /**
         * 设置扩展参数
         *
         * @param requests 扩展参数
         */
        public void setRequests(final Map<String, Object> requests) {
            context.requests = requests;
            context.dirty = true;
        }

        /**
         * 创建新的上下文
         *
         * @return 新上下文
         */
        public RequestContext create() {
            RequestContext result = new RequestContext();
            result.sessions = context.sessions;
            result.callers = context.callers;
            result.traces = context.traces;
            result.dirty = true;
            return result;
        }

        /**
         * 清理扩展属性
         *
         * @return
         */
        public void clear() {
            context.clear();
        }
    }

    /**
     * 内容类型，按照顺序进行合并
     */
    protected enum ContentType {
        MERGE(-1),
        ONLY_CALLER(0),
        ONLY_REQUEST(1),
        ONLY_SESSION(2),
        ONLY_TRACE(3);
        /**
         * 顺序
         */
        private int order;

        ContentType(int order) {
            this.order = order;
        }

        public int getOrder() {
            return order;
        }
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
     * @param context 上下文
     */
    public static void restore(final RequestContext context) {
        LOCAL.set(context);
    }

}
