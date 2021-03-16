package io.joyrpc.protocol.handler;

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

import io.joyrpc.Result;
import io.joyrpc.codec.compression.Compression;
import io.joyrpc.constants.ExceptionCode;
import io.joyrpc.context.RequestContext;
import io.joyrpc.context.injection.RespInjection;
import io.joyrpc.context.injection.Transmit;
import io.joyrpc.context.injection.Transmits;
import io.joyrpc.exception.*;
import io.joyrpc.invoker.Exporter;
import io.joyrpc.invoker.ServiceManager;
import io.joyrpc.protocol.MsgType;
import io.joyrpc.protocol.ServerProtocol;
import io.joyrpc.protocol.message.*;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelContext;
import io.joyrpc.transport.session.Session;
import io.joyrpc.transport.session.Session.ServerSession;
import io.joyrpc.util.GenericMethod;
import io.joyrpc.util.GenericType;
import io.joyrpc.util.network.Ipv4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static io.joyrpc.Plugin.RESPONSE_INJECTION;
import static io.joyrpc.Plugin.TRANSMIT;
import static io.joyrpc.constants.ExceptionCode.PROVIDER_TASK_SESSION_EXPIRED;
import static io.joyrpc.util.StringUtils.isEmpty;

/**
 * 业务请求处理
 */
public class BizReceiver extends AbstractReceiver {

    private final static Logger logger = LoggerFactory.getLogger(BizReceiver.class);

    /**
     * 透传反向清理
     */
    protected Transmit transmit = new Transmits(TRANSMIT.reverse());
    /**
     * 应答注入
     */
    protected Iterable<RespInjection> injections = RESPONSE_INJECTION.extensions();

    @SuppressWarnings("unchecked")
    @Override
    public void handle(final ChannelContext context, final Message message) throws HandlerException {
        if (!(message instanceof RequestMessage)) {
            return;
        }
        BizReq bizReq = new BizReq(context, (RequestMessage<Invocation>) message, transmit, injections);
        if (!bizReq.discard()) {
            try {
                //恢复调用信息和上下文
                bizReq.restore();
                //调用
                bizReq.invoke();
            } catch (ClassNotFoundException e) {
                bizReq.fail(e.getMessage());
            } catch (LafException e) {
                bizReq.fail(e);
            } catch (Throwable e) {
                bizReq.fail(e.getMessage(), e);
            } finally {
                //清理上下文
                bizReq.exit();
            }
        }
    }

    @Override
    public Integer type() {
        return (int) MsgType.BizReq.getType();
    }

    /**
     * 请求对象
     */
    protected static class BizReq {
        /**
         * 请求
         */
        protected RequestMessage<Invocation> request;
        /**
         * 会话
         */
        protected ServerSession session;
        /**
         * 调用对象
         */
        protected Invocation invocation;
        /**
         * 上下文
         */
        protected ChannelContext context;
        /**
         * 通道
         */
        protected Channel channel;
        /**
         * 透传反向清理
         */
        protected Transmit transmit;
        /**
         * 应答注入
         */
        protected Iterable<RespInjection> injections;
        /**
         * Exporter
         */
        protected Exporter exporter;

        public BizReq(ChannelContext context,
                      RequestMessage<Invocation> request,
                      Transmit transmit,
                      Iterable<RespInjection> injections) {
            this.request = request;
            this.session = (ServerSession) request.getSession();
            this.invocation = request.getPayLoad();
            this.context = context;
            this.channel = context.getChannel();
            this.transmit = transmit;
            this.injections = injections;
        }

        public RequestMessage<Invocation> getRequest() {
            return request;
        }

        public Channel getChannel() {
            return channel;
        }

        public Exporter getExporter() {
            return exporter;
        }

        public void setExporter(Exporter exporter) {
            this.exporter = exporter;
        }

        /**
         * 恢复上下文及补充信息
         *
         * @throws ClassNotFoundException 类没有找到异常
         */
        public void restore() throws ClassNotFoundException {
            request.setContext(RequestContext.getContext());
            //从会话恢复接口和别名
            if (session != null) {
                if (isEmpty(invocation.getClassName())) {
                    invocation.setClassName(session.getInterfaceName());
                }
                if (isEmpty(invocation.getAlias())) {
                    invocation.setAlias(session.getAlias());
                }
                request.setLocalAddress(session.getLocalAddress());
                request.setRemoteAddress(session.getRemoteAddress());
                request.setTransport(session.getTransport());
                exporter = (Exporter) session.getProvider();
            }
            if (request.getLocalAddress() == null) {
                request.setLocalAddress(channel.getLocalAddress());
            }
            if (request.getRemoteAddress() == null) {
                request.setRemoteAddress(channel.getRemoteAddress());
            }
            if (request.getTransport() == null) {
                request.setTransport(channel.getAttribute(Channel.CHANNEL_TRANSPORT));
            }

            String className = invocation.getClassName();
            if (isEmpty(className)) {
                //session 为空，类名也为空，可能是session超时并被清理
                throw new SessionException(error(" may be the session has expired", PROVIDER_TASK_SESSION_EXPIRED));
            }
            //检查接口ID，兼容老版本
            checkInterfaceId(invocation, className);
            //直接使用会话上的Exporter，加快性能
            if (exporter == null) {
                exporter = ServiceManager.getExporter(invocation.getClassName(), invocation.getAlias(), channel.getLocalAddress().getPort());
                if (exporter == null) {
                    //当多个接口使用同一的服务名称S进行注册，A接口注册后，B接口还没有准备好，客户端拿到服务S的地址，调用B的请求到达，B还没有就绪
                    //把关机异常改成拒绝异常，客户端可以重试
                    throw new RejectException(error(" exporter is not found"));
                }
            }
            //构建请求
            exporter.setup(request);
            //对应服务端协议，设置认证信息
            if (exporter.getAuthentication() != null) {
                ServerProtocol protocol = null;
                if (session != null) {
                    protocol = session.getProtocol();
                }
                if (protocol == null) {
                    protocol = channel.getAttribute(Channel.PROTOCOL);
                }
                if (protocol != null) {
                    request.setAuthenticated(protocol::authenticate);
                }
            }
        }

        /**
         * 获取错误信息
         *
         * @param cause 异常
         * @return 异常
         */
        protected String error(final String cause) {
            return error(cause, ExceptionCode.PROVIDER_TASK_FAIL);
        }

        /**
         * 获取错误信息
         *
         * @param cause 异常信息
         * @param code  异常代码
         * @return 异常
         */
        protected String error(final String cause, final String code) {
            Invocation invocation = request.getPayLoad();
            return String.format(ExceptionCode.format(code == null ? ExceptionCode.PROVIDER_TASK_FAIL : code)
                            + "Error occurs while processing request %s/%s/%s from channel %s->%s, caused by: %s",
                    invocation.getClassName(),
                    invocation.getMethodName(),
                    invocation.getAlias(),
                    Ipv4.toAddress(channel.getRemoteAddress()),
                    Ipv4.toAddress(channel.getLocalAddress()),
                    cause);
        }

        /**
         * 检查接口ID，兼容老版本
         *
         * @param invocation 调用信息
         * @param className  类
         * @throws ClassNotFoundException 类没有找到
         */
        protected void checkInterfaceId(final Invocation invocation, String className) throws ClassNotFoundException {
            if (Character.isDigit(className.charAt(0))) {
                //处理接口ID，兼容老版本调用
                try {
                    className = ServiceManager.getClassName(Long.parseLong(className));
                    if (className == null) {
                        throw new ClassNotFoundException("class is not found by interfaceId " + invocation.getClassName());
                    }
                    invocation.setClassName(className);
                    invocation.setClazz(null);
                } catch (NumberFormatException e) {
                    throw new ClassNotFoundException("class is not found by interfaceId " + invocation.getClassName());
                }
            }
        }

        /**
         * 调用
         */
        public void invoke() {
            //执行调用，包括过滤器链
            exporter.invoke(request).whenComplete(this::onComplete);
        }

        /**
         * 调用完成
         *
         * @param result    结果
         * @param throwable 异常
         */
        @SuppressWarnings("unchecked")
        protected void onComplete(final Result result, final Throwable throwable) {
            Invocation invocation = request.getPayLoad();
            if (throwable != null) {
                if (!(throwable instanceof ShutdownExecption)) {
                    logger.error(error(throwable.getMessage()));
                }
                fail(throwable);
            } else {
                //构造响应Msg
                ResponseMessage<ResponsePayload> response = createResponseMessage();
                GenericMethod genericMethod = invocation == null ? null : invocation.getGenericMethod();
                GenericType returnType = genericMethod == null ? null : genericMethod.getReturnType();
                Type type = returnType == null ? null : returnType.getGenericType();
                RequestContext context = result.getContext();
                if (context.isAsync() && !result.isException()) {
                    //异步
                    CompletableFuture<Object> future = (CompletableFuture<Object>) result.getValue();
                    future.whenComplete((obj, th) -> {
                        response.setPayLoad(new ResponsePayload(obj, th, type));
                        transmit.onServerComplete(request, th != null ? new Result(request.getContext(), th) : new Result(request.getContext(), obj));
                        acknowledge(this.context, request, response, BizReceiver.logger);
                    });
                } else {
                    //同步调用
                    response.setPayLoad(new ResponsePayload(result.getValue(), result.getException(), type));
                    transmit.onServerComplete(request, result);
                    acknowledge(this.context, request, response, BizReceiver.logger);
                }
            }
        }

        /**
         * 创建应答消息
         *
         * @return 应答消息
         */
        protected ResponseMessage<ResponsePayload> createResponseMessage() {
            return createResponseMessage(null);
        }

        /**
         * 创建应答消息
         *
         * @param compressType 压缩类型
         * @return 应答消息
         */
        protected ResponseMessage<ResponsePayload> createResponseMessage(final Byte compressType) {
            MessageHeader header = request.getHeader();
            Session session = request.getSession();
            Supplier<ResponseMessage<ResponsePayload>> supplier = request.getResponseSupplier();
            ResponseMessage<ResponsePayload> response = supplier != null ? supplier.get() :
                    new ResponseMessage<>(header.response(MsgType.BizResp.getType(),
                            compressType != null ? compressType :
                                    (session == null ? Compression.NONE : session.getCompressionType())));
            return response;
        }

        /**
         * 执行结束退出，清理上下文
         */
        public void exit() {
            transmit.onServerReturn(request);
        }

        /**
         * 处理失败
         *
         * @param message 异常消息
         */
        public void fail(String message) {
            fail(new RpcException(error(message)));
        }

        /**
         * 处理失败
         *
         * @param message 异常消息
         * @param cause   原因
         */
        public void fail(String message, Throwable cause) {
            fail(new RpcException(error(message), cause));
        }

        /**
         * 处理失败
         *
         * @param e 异常
         */
        public void fail(Throwable e) {
            //服务端结束
            transmit.onServerComplete(request, new Result(request.getContext(), e));
            //构建异常应答消息，不压缩
            ResponseMessage<ResponsePayload> response = createResponseMessage(Compression.NONE);
            response.setPayLoad(new ResponsePayload(e));
            //注入异常信息
            for (RespInjection injection : injections) {
                injection.inject(request, response, exporter);
            }
            acknowledge(context, request, response, BizReceiver.logger);
        }

        /**
         * 判断是否要丢弃消息
         *
         * @return 丢弃标识
         */
        public boolean discard() {
            if (request.isTimeout(request::getReceiveTime)) {
                //客户端已经超时的请求
                logger.warn(String.format("%sDiscard request caused by timeout after receive the msg. at %s : %s",
                        ExceptionCode.format(ExceptionCode.PROVIDER_DISCARD_TIMEOUT_MESSAGE),
                        Channel.toString(channel), request.getHeader()));
                return true;
            } else if (!channel.isWritable()) {
                //channel不可写，丢弃消息
                logger.error(String.format("Discard request caused by channel is not writable when client is sending too fast. at %s : %s",
                        Channel.toString(channel), request.getHeader()));
                return true;
            }
            return false;
        }

    }
}
