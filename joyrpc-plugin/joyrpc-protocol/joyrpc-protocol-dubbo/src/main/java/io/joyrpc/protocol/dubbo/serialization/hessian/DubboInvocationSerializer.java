package io.joyrpc.protocol.dubbo.serialization.hessian;

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

import io.joyrpc.com.caucho.hessian.io.AbstractHessianOutput;
import io.joyrpc.com.caucho.hessian.io.AutowiredObjectSerializer;
import io.joyrpc.protocol.dubbo.message.DubboInvocation;

import java.io.IOException;

import static io.joyrpc.protocol.dubbo.DubboAbstractProtocol.DEFALUT_DUBBO_VERSION;

/**
 * DubboInvocation序列化
 */
public class DubboInvocationSerializer implements AutowiredObjectSerializer {

    @Override
    public Class<?> getType() {
        return DubboInvocation.class;
    }

    @Override
    public void writeObject(Object obj, AbstractHessianOutput out) throws IOException {
        if (!(obj instanceof DubboInvocation)) {
            throw new IOException("Write dubbo invocation data failed, because invocation class is error,  invocation class is "
                    + obj.getClass());
        }
        DubboInvocation invocation = (DubboInvocation) obj;
        //写dubboversion
        out.writeString(DEFALUT_DUBBO_VERSION);
        //写接口名
        out.writeString(invocation.getClassName());
        //写服务版本
        out.writeString(invocation.getVersion());
        //写方法名
        out.writeString(invocation.getMethodName());
        //写参数描述
        out.writeString(invocation.getParameterTypesDesc());
        //写参数
        Object[] args = invocation.getArgs();
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                //TODO callback处理
                out.writeObject(args[i]);
            }
        }
        //写attachments
        out.writeObject(invocation.getAttachments());
    }
}
