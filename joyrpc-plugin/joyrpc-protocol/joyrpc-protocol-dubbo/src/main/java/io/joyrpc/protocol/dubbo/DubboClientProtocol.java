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

import io.joyrpc.extension.Extension;
import io.joyrpc.extension.URL;
import io.joyrpc.extension.URLOption;
import io.joyrpc.extension.condition.ConditionalOnClass;
import io.joyrpc.protocol.ClientProtocol;
import io.joyrpc.protocol.MsgType;
import io.joyrpc.protocol.dubbo.message.DubboInvocation;
import io.joyrpc.protocol.message.MessageHeader;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.protocol.message.ResponseMessage;
import io.joyrpc.protocol.message.negotiation.NegotiationResponse;
import io.joyrpc.transport.Client;
import io.joyrpc.transport.message.Message;

import java.util.Arrays;
import java.util.List;

import static io.joyrpc.Plugin.SERIALIZATION;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.constants.Constants.ALIAS_OPTION;

/**
 * Dubbo客户端协议
 */
@Extension("dubbo")
@ConditionalOnClass("org.apache.dubbo.rpc.Protocol")
public class DubboClientProtocol extends DubboAbstractProtocol implements ClientProtocol {

    /**
     * 默认序列化参数
     */
    protected static final URLOption<String> SERIALIZATION_OPTION = new URLOption<>("serialization", "hessian");

    /**
     * 序列化列表，优先级排序
     */
    protected static final List<String> SERIALIZATIONS = Arrays.asList("hessian", "fst", "protobuf", "kryo");

    @Override
    public Message negotiate(final URL clusterUrl, final Client client) {
        //拿到服务端的版本
        NegotiationResponse response = new NegotiationResponse();
        //设置可用的序列化插件
        response.setSerializations(SERIALIZATION.available(SERIALIZATIONS));
        //设置优先序列化方式
        response.setSerialization(clusterUrl.getString(SERIALIZATION_OPTION));
        //添加扩展属性信息
        response.addAttribute(CONFIG_KEY_INTERFACE, clusterUrl.getPath());
        response.addAttribute(ALIAS_OPTION.getName(), clusterUrl.getString(ALIAS_OPTION));
        // 构造协商响应消息
        return new ResponseMessage<>(new MessageHeader(MsgType.NegotiationResp.getType()), response);
    }

    @Override
    public Message sessionbeat(URL clusterUrl, Client client) {
        return null;
    }

    @Override
    public Message heartbeat(URL clusterUrl, Client client) {
        return new RequestMessage(new MessageHeader(MsgType.HbReq.getType()), new DubboInvocation(true));
    }
}
