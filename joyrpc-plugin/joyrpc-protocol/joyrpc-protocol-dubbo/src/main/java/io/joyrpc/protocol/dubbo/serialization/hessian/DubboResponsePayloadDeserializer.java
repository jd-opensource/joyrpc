package io.joyrpc.protocol.dubbo.serialization.hessian;

import io.joyrpc.com.caucho.hessian.io.AbstractHessianInput;
import io.joyrpc.com.caucho.hessian.io.AutowiredObjectDeserializer;

import java.io.IOException;

public class DubboResponsePayloadDeserializer implements AutowiredObjectDeserializer {

    @Override
    public Class<?> getType() {
        return null;
    }

    @Override
    public boolean isReadResolve() {
        return false;
    }

    @Override
    public Object readObject(AbstractHessianInput in) throws IOException {
        return null;
    }

    @Override
    public Object readList(AbstractHessianInput in, int length) throws IOException {
        return null;
    }

    @Override
    public Object readLengthList(AbstractHessianInput in, int length) throws IOException {
        return null;
    }

    @Override
    public Object readMap(AbstractHessianInput in) throws IOException {
        return null;
    }

    @Override
    public Object[] createFields(int len) {
        return new Object[0];
    }

    @Override
    public Object createField(String name) {
        return null;
    }

    @Override
    public Object readObject(AbstractHessianInput in, Object[] fields) throws IOException {
        return null;
    }

    @Override
    public Object readObject(AbstractHessianInput in, String[] fieldNames) throws IOException {
        return null;
    }
}
