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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.joyrpc.Plugin.SERIALIZATION;
import static io.joyrpc.constants.Constants.*;

/**
 * Dubbo客户端协议
 */
@Extension("dubbo")
public class DubboClientProtocol extends AbstractDubboProtocol implements ClientProtocol {

    /**
     * 默认序列化参数
     */
    protected static final URLOption<String> SERIALIZATION_OPTION = new URLOption<>("serialization", "hessian@dubbo");
    /**
     * 序列化列表，优先级排序
     */
    protected static final List<String> SERIALIZATIONS = Arrays.asList("hessian@dubbo", "fst", "protostuff@dubbo", "kryo", "java@advance");

    protected static final Map<String, String> SERIALIZATION_MAPPING = new HashMap<>();

    static {
        SERIALIZATION_MAPPING.put("hessian", "hessian@dubbo");
        SERIALIZATION_MAPPING.put("hessian@dubbo", "hessian@dubbo");
        SERIALIZATION_MAPPING.put("fst", "fst");
        SERIALIZATION_MAPPING.put("protostuff", "protostuff@dubbo");
        SERIALIZATION_MAPPING.put("protostuff@dubbo", "protostuff@dubbo");
        SERIALIZATION_MAPPING.put("kryo", "kryo");
        SERIALIZATION_MAPPING.put("java", "java@advance");
        SERIALIZATION_MAPPING.put("java@advance", "java@advance");
    }

    @Override
    public Message negotiate(final URL clusterUrl, final Client client) {
        //拿到服务端的版本
        NegotiationResponse response = new NegotiationResponse();
        //设置可用的序列化插件
        List<String> availables = SERIALIZATION.available(SERIALIZATIONS);
        response.setSerializations(availables);
        //设置优先序列化方式
        String serialization = SERIALIZATION_MAPPING.get(clusterUrl.getString(SERIALIZATION_OPTION));
        if (serialization == null || !availables.contains(serialization)) {
            serialization = availables.get(0);
        }
        response.setSerialization(serialization);
        //添加扩展属性信息
        response.addAttribute(CONFIG_KEY_INTERFACE, clusterUrl.getPath());
        response.addAttribute(ALIAS_OPTION.getName(), clusterUrl.getString(ALIAS_OPTION));
        response.addAttribute(SERVICE_VERSION_OPTION.getName(), clusterUrl.getString(SERVICE_VERSION_OPTION));
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
