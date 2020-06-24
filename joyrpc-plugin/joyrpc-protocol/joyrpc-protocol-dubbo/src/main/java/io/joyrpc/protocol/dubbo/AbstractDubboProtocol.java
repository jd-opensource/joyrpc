package io.joyrpc.protocol.dubbo;

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

import io.joyrpc.codec.serialization.Serialization;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.extension.URL;
import io.joyrpc.invoker.CallbackMethod;
import io.joyrpc.protocol.AbstractProtocol;
import io.joyrpc.protocol.MsgType;
import io.joyrpc.protocol.dubbo.codec.DubboCodec;
import io.joyrpc.protocol.dubbo.message.DubboInvocation;
import io.joyrpc.protocol.dubbo.message.DubboMessageHeader;
import io.joyrpc.protocol.dubbo.message.DubboResponsePayload;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.protocol.message.ResponseMessage;
import io.joyrpc.protocol.message.ResponsePayload;
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.message.Message;
import io.joyrpc.transport.session.Session;

import java.util.function.Function;

import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.protocol.dubbo.DubboCallback.getProducerServiceUrl;
import static io.joyrpc.protocol.dubbo.DubboCallback.toDubboInsId;
import static io.joyrpc.protocol.dubbo.DubboStatus.getStatus;
import static io.joyrpc.protocol.dubbo.message.DubboInvocation.*;
import static io.joyrpc.protocol.dubbo.serialization.hessian2.DubboHessian2Serialization.DUBBO_HESSIAN_ID;
import static io.joyrpc.util.ClassUtils.getDesc;

/**
 * Dubbo协议
 */
public abstract class AbstractDubboProtocol extends AbstractProtocol {

    /**
     * Dubbo的MagicCode
     */
    protected static final byte[] MAGIC_CODE = new byte[]{(byte) 0xDA, (byte) 0xBB};
    /**
     * 默认Dubbo版本号
     */
    public static final String DEFALUT_DUBBO_VERSION = "2.0.2";

    private static final String GENERIC_INVOKE_PARAM_TYPES_DESC = "Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;";

    private static final String GENERIC_INVOKE_METHOD = "$invoke";


    /**
     * Dubbo序列化标识
     */
    protected static final byte HESSIAN2_SERIALIZATION_ID = 2;
    protected static final byte JAVA_SERIALIZATION_ID = 3;
    protected static final byte COMPACTED_JAVA_SERIALIZATION_ID = 4;
    protected static final byte FASTJSON_SERIALIZATION_ID = 6;
    protected static final byte NATIVE_JAVA_SERIALIZATION_ID = 7;
    protected static final byte KRYO_SERIALIZATION_ID = 8;
    protected static final byte FST_SERIALIZATION_ID = 9;
    protected static final byte NATIVE_HESSIAN_SERIALIZATION_ID = 10;
    protected static final byte PROTOSTUFF_SERIALIZATION_ID = 12;
    protected static final byte AVRO_SERIALIZATION_ID = 11;
    protected static final byte GSON_SERIALIZATION_ID = 16;
    protected static final byte PROTOBUF_JSON_SERIALIZATION_ID = 21;
    protected static final byte PROTOBUF_SERIALIZATION_ID = 22;
    protected static final byte KRYO_SERIALIZATION2_ID = 25;

    /**
     * 序列化映射 dobbo -> joy
     */
    protected static final byte[] SERIALIZATIONS_TO_JOY = new byte[127];

    /**
     * 序列化映射 joy -> dobbo
     */
    protected static final byte[] SERIALIZATIONS_TO_DUBBO = new byte[127];

    static {
        SERIALIZATIONS_TO_JOY[HESSIAN2_SERIALIZATION_ID] = DUBBO_HESSIAN_ID;
        SERIALIZATIONS_TO_JOY[KRYO_SERIALIZATION_ID] = (byte) Serialization.KRYO_ID;
        SERIALIZATIONS_TO_JOY[PROTOSTUFF_SERIALIZATION_ID] = (byte) Serialization.DUBBO_PROTOSTUFF_ID;
        SERIALIZATIONS_TO_JOY[FST_SERIALIZATION_ID] = (byte) Serialization.FST_ID;
        SERIALIZATIONS_TO_JOY[JAVA_SERIALIZATION_ID] = (byte) Serialization.ADVANCE_JAVA_ID;
        SERIALIZATIONS_TO_JOY[FASTJSON_SERIALIZATION_ID] = (byte) Serialization.JSON_ID;
        SERIALIZATIONS_TO_DUBBO[DUBBO_HESSIAN_ID] = HESSIAN2_SERIALIZATION_ID;
        SERIALIZATIONS_TO_DUBBO[(byte) Serialization.KRYO_ID] = KRYO_SERIALIZATION_ID;
        SERIALIZATIONS_TO_DUBBO[(byte) Serialization.DUBBO_PROTOSTUFF_ID] = PROTOSTUFF_SERIALIZATION_ID;
        SERIALIZATIONS_TO_DUBBO[(byte) Serialization.FST_ID] = FST_SERIALIZATION_ID;
        SERIALIZATIONS_TO_DUBBO[(byte) Serialization.ADVANCE_JAVA_ID] = JAVA_SERIALIZATION_ID;
        SERIALIZATIONS_TO_DUBBO[(byte) Serialization.JSON_ID] = FASTJSON_SERIALIZATION_ID;
    }

    @Override
    protected Codec createCodec() {
        return new DubboCodec(this);
    }

    @Override
    public byte[] getMagicCode() {
        return MAGIC_CODE;
    }

    @Override
    protected MessageConverter createInConverter() {
        return new MessageConverter() {

            @Override
            public Function<Byte, Byte> serialization() {
                return s -> SERIALIZATIONS_TO_JOY[s];
            }
        };
    }

    @Override
    protected MessageConverter createOutConverter() {
        return new MessageConverter() {

            @Override
            public Function<Byte, Byte> serialization() {
                return s -> SERIALIZATIONS_TO_DUBBO[s];
            }

            @Override
            public Function<Object, Object> message() {
                return obj -> {
                    Message message = (Message) obj;
                    MsgType type = MsgType.valueOf(message.getMsgType());
                    switch (type) {
                        case BizReq:
                        case CallbackReq:
                        case HbReq:
                            return outputRequest((RequestMessage<Invocation>) message, type);
                        case BizResp:
                        case CallbackResp:
                        case HbResp:
                            return outputResponse((ResponseMessage) message);
                        default:
                            return message;
                    }
                };
            }
        };
    }

    /**
     * 输出请求
     *
     * @param message
     */
    protected Object outputRequest(final RequestMessage<Invocation> message, MsgType type) {
        Invocation payLoad = message.getPayLoad();
        if (payLoad != null && message.getMsgType() != MsgType.HbReq.getType()) {
            boolean generic = payLoad.isGeneric();
            DubboInvocation invocation = new DubboInvocation();
            invocation.setClassName(payLoad.getClassName());
            invocation.setMethodName(generic ? GENERIC_INVOKE_METHOD : payLoad.getMethodName());
            invocation.setAlias(payLoad.getAlias());
            String parameterTypesDesc = generic ? GENERIC_INVOKE_PARAM_TYPES_DESC :
                    message.getOption() != null ? message.getOption().getDescription() : getDesc(payLoad.getMethod().getParameterTypes());
            invocation.setParameterTypesDesc(parameterTypesDesc);
            invocation.setArgs(payLoad.getArgs());
            Session session = message.getSession();
            if (session != null && session.getAttributes() != null) {
                String serviceVersion = session.getOrDefault(SERVICE_VERSION_OPTION.getName(), "");
                invocation.setVersion(serviceVersion);
            }
            invocation.addAttachment(DUBBO_PATH_KEY, payLoad.getClassName());
            invocation.addAttachment(DUBBO_INTERFACE_KEY, payLoad.getClassName());
            invocation.addAttachment(DUBBO_GROUP_KEY, payLoad.getAlias());
            invocation.addAttachment(DUBBO_SERVICE_VERSION_KEY, invocation.getVersion());
            invocation.addAttachment(DUBBO_TIMEOUT_KEY, String.valueOf(message.getTimeout()));
            String appName = GlobalContext.getString(KEY_APPNAME);
            if (appName != null && !appName.isEmpty()) {
                invocation.addAttachment(DUBBO_APPLICATION_KEY, appName);
            }
            if (generic) {
                invocation.addAttachment(DUBBO_GENERIC_KEY, "true");
            }

            switch (type) {
                case BizReq: {
                    CallbackMethod callbackMethod = message.getOption() == null ? null : message.getOption().getCallback();
                    if (callbackMethod != null) {
                        int paramIndex = callbackMethod.getIndex();
                        Object callbackInsId = message.getHeader().getAttribute(HEAD_CALLBACK_INSID);
                        if (callbackInsId != null) {
                            invocation.addAttachment(DUBBO_CALLBACK_ARG_PRE + paramIndex, toDubboInsId(callbackInsId));
                        }
                    }
                    break;
                }

                case CallbackReq: {
                    Object callbackInsId = message.getHeader().getAttribute(HEAD_CALLBACK_INSID);
                    URL url = getProducerServiceUrl(callbackInsId);
                    if (url != null) {
                        invocation.computeIfAbsent(DUBBO_GROUP_KEY, o -> url.getString(ALIAS_OPTION));
                        invocation.computeIfAbsent(DUBBO_SERVICE_VERSION_KEY, o -> url.getString(SERVICE_VERSION_OPTION));
                    }
                    invocation.addAttachment(DUBBO_CALLBACK_INSID_KEY, callbackInsId);
                    invocation.addAttachment(DUBBO_IS_CALLBACK_INVOKE_KEY, true);
                    break;
                }

            }

            message.setPayLoad(invocation);
        }
        return message;
    }

    /**
     * 输出应答
     *
     * @param message
     */
    protected Object outputResponse(final ResponseMessage message) {
        //心跳，payload置空
        if (message.getMsgType() == MsgType.HbResp.getType()) {
            message.setPayLoad(new DubboResponsePayload(true));
            return message;
        }
        //转换payload
        Object payload = message.getPayLoad();
        ResponsePayload responsePayload = payload instanceof ResponsePayload ? (ResponsePayload) message.getPayLoad() : null;
        //获取dubbo版本，设置status
        String dubboVersion = DEFALUT_DUBBO_VERSION;
        if (message.getHeader() instanceof DubboMessageHeader) {
            DubboMessageHeader header = ((DubboMessageHeader) message.getHeader());
            //获取dubbo版本
            dubboVersion = header.getDubboVersion();
            //设置status
            byte status = getStatus(responsePayload == null ? null : responsePayload.getException());
            header.setStatus(status);
        }
        //创建dubbo payload
        DubboResponsePayload dubboPayload = responsePayload == null
                ? new DubboResponsePayload(dubboVersion)
                : new DubboResponsePayload(responsePayload.getResponse(), responsePayload.getException(),
                responsePayload.getReturnType(), dubboVersion);
        message.setPayLoad(dubboPayload);

        return message;
    }

}
