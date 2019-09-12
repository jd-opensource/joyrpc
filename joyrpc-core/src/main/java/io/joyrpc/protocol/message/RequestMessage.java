package io.joyrpc.protocol.message;

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

import io.joyrpc.cluster.distribution.FailoverPolicy;
import io.joyrpc.context.RequestContext;
import io.joyrpc.extension.Parametric;
import io.joyrpc.protocol.MsgType;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.util.SystemClock;

import java.net.InetSocketAddress;
import java.util.function.Supplier;

/**
 * @date: 8/1/2019
 */
public class RequestMessage<T> extends BaseMessage<T> implements Request {

    /**
     * 请求体信息
     */
    protected T payload;
    /**
     * 创建时间
     */
    protected transient long createTime;
    /**
     * temp Property for Request receive time
     */
    protected transient long receiveTime;
    /**
     * 原始超时时间，不是当前重试调用的超时时间
     */
    protected transient long timeout;
    /**
     * 重试策略
     */
    protected transient FailoverPolicy failoverPolicy;
    /**
     * 请求上下文
     */
    protected transient RequestContext context;

    /**
     * The Local address.
     */
    protected transient InetSocketAddress localAddress;

    /**
     * The Remote address.
     */
    protected transient InetSocketAddress remoteAddress;
    /**
     * 调用的线程
     */
    protected transient Thread thread;
    /**
     * 用于生成应答消息，便于传递请求的上下文
     */
    protected transient Supplier<ResponseMessage> responseSupplier;

    /**
     * 构造函数
     */
    public RequestMessage() {
        super(new MessageHeader());
    }

    /**
     * 构造函数
     *
     * @param header
     */
    public RequestMessage(MessageHeader header) {
        super(header);
    }

    /**
     * 构造函数
     *
     * @param header
     * @param payload
     */
    public RequestMessage(MessageHeader header, T payload) {
        super(header);
        this.payload = payload;
    }


    /**
     * 构造
     *
     * @param invocation
     * @return
     */
    public static RequestMessage<Invocation> build(final Invocation invocation) {
        RequestMessage<Invocation> request = new RequestMessage<>(new MessageHeader(MsgType.BizReq.getType()), invocation);
        request.createTime = SystemClock.now();
        return request;
    }

    /**
     * 构造
     *
     * @param invocation
     * @param channel
     * @return
     */
    public static RequestMessage<Invocation> build(final Invocation invocation, final Channel channel) {
        return build(new MessageHeader(MsgType.BizReq.getType()), invocation, channel.getLocalAddress(), channel.getRemoteAddress());
    }

    /**
     * 构造
     *
     * @param header
     * @param invocation
     * @param channel
     * @param http
     * @return
     */
    public static RequestMessage<Invocation> build(final MessageHeader header, final Invocation invocation,
                                                   final Channel channel, final Parametric http, final long receiveTime) {
        // 解析远程地址
        InetSocketAddress remoteAddress = channel.getRemoteAddress();
        String remoteIp = http == null ? null : http.getString("X-Forwarded-For", "X-Real-IP", null);
        if (remoteIp != null) {
            // 可能是vip nginx等转发后的ip，多次转发用逗号分隔
            int pos = remoteIp.indexOf(',');
            remoteIp = pos > 0 ? remoteIp.substring(0, pos).trim() : remoteIp.trim();
            if (!remoteIp.isEmpty()) {
                remoteAddress = InetSocketAddress.createUnresolved(remoteIp, channel.getRemoteAddress().getPort());
            }
        }
        RequestMessage requestMessage = build(header, invocation, channel.getLocalAddress(), remoteAddress);
        requestMessage.setReceiveTime(receiveTime);
        //隐式传参
        return requestMessage;
    }

    /**
     * 构造
     *
     * @param header
     * @param invocation
     * @param localAddress
     * @param remoteAddress
     * @return
     */
    public static RequestMessage<Invocation> build(final MessageHeader header, final Invocation invocation,
                                                   final InetSocketAddress localAddress,
                                                   final InetSocketAddress remoteAddress) {
        RequestMessage<Invocation> request = new RequestMessage<>(header, invocation);
        request.createTime = SystemClock.now();
        request.localAddress = localAddress;
        request.remoteAddress = remoteAddress;
        request.thread = Thread.currentThread();
        request.context = RequestContext.getContext();
        request.context.setLocalAddress(localAddress);
        request.context.setRemoteAddress(remoteAddress);
        return request;
    }

    @Override
    public T getPayLoad() {
        return payload;
    }

    @Override
    public void setPayLoad(T data) {
        this.payload = data;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getReceiveTime() {
        return receiveTime;
    }

    public void setReceiveTime(long receiveTime) {
        this.receiveTime = receiveTime;
    }

    public FailoverPolicy getFailoverPolicy() {
        return failoverPolicy;
    }

    public void setFailoverPolicy(FailoverPolicy failoverPolicy) {
        this.failoverPolicy = failoverPolicy;
    }

    public RequestContext getContext() {
        return context;
    }

    public void setContext(RequestContext context) {
        this.context = context;
    }

    public Thread getThread() {
        return thread;
    }

    public void setThread(Thread thread) {
        this.thread = thread;
    }

    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    public void setLocalAddress(InetSocketAddress localAddress) {
        this.localAddress = localAddress;
    }

    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(InetSocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public Supplier<ResponseMessage> getResponseSupplier() {
        return responseSupplier;
    }

    public void setResponseSupplier(Supplier<ResponseMessage> responseSupplier) {
        this.responseSupplier = responseSupplier;
    }

    /**
     * 当前请求是否超时
     *
     * @return
     */
    public boolean isTimeout() {
        return SystemClock.now() - createTime > (timeout > 0 ? timeout : header.timeout);
    }

    /**
     * 当前请求是否超时
     *
     * @param startTime
     * @return
     */
    public boolean isTimeout(final Supplier<Long> startTime) {
        return SystemClock.now() - startTime.get() > (timeout > 0 ? timeout : header.timeout);
    }

    @Override
    public boolean isRequest() {
        return true;
    }

    @Override
    public String toString() {
        return "RequestMessage{" +
                "header=" + header +
                ", payload=" + payload +
                '}';
    }
}
