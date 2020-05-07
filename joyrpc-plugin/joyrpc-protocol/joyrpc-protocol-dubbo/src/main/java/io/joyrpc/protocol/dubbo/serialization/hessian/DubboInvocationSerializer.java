package io.joyrpc.protocol.dubbo.serialization.hessian;

import io.joyrpc.com.caucho.hessian.io.AbstractHessianOutput;
import io.joyrpc.com.caucho.hessian.io.AutowiredObjectSerializer;
import io.joyrpc.protocol.dubbo.message.DubboInvocation;

import java.io.IOException;

public class DubboInvocationSerializer implements AutowiredObjectSerializer {

    @Override
    public Class<?> getType() {
        return DubboInvocation.class;
    }

    @Override
    public void writeObject(Object obj, AbstractHessianOutput out) throws IOException {

    }
}
