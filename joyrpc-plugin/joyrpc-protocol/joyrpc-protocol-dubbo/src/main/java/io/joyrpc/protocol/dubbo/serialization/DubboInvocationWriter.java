package io.joyrpc.protocol.dubbo.serialization;

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

import io.joyrpc.codec.serialization.ObjectWriter;
import io.joyrpc.protocol.dubbo.message.DubboInvocation;

import java.io.IOException;

import static io.joyrpc.protocol.dubbo.AbstractDubboProtocol.DEFALUT_DUBBO_VERSION;

/**
 * DubboInvocation序列化
 */
public class DubboInvocationWriter {
    /**
     * 写对象
     */
    protected ObjectWriter writer;

    public DubboInvocationWriter(ObjectWriter writer) {
        this.writer = writer;
    }

    /**
     * 写调用
     *
     * @param invocation 调度
     * @throws IOException
     */
    public void write(final DubboInvocation invocation) throws IOException {
        //心跳响应，直接写null
        if (invocation.isHeartbeat()) {
            writer.writeNull();
            return;
        }
        //写dubboversion
        writer.writeString(DEFALUT_DUBBO_VERSION);
        //写接口名
        writer.writeString(invocation.getClassName());
        //写服务版本
        writer.writeString(invocation.getVersion());
        //写方法名
        writer.writeString(invocation.getMethodName());
        //写参数描述
        writer.writeString(invocation.getParameterTypesDesc());
        //写参数
        Object[] args = invocation.getArgs();
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                //TODO callback处理
                writer.writeObject(args[i]);
            }
        }
        //写attachments
        writer.writeObject(invocation.getAttachments());
    }

}
