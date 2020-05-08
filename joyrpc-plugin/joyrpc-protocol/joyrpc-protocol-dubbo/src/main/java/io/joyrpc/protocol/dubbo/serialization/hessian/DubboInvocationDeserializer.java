package io.joyrpc.protocol.dubbo.serialization.hessian;

import io.joyrpc.com.caucho.hessian.io.AbstractHessianInput;
import io.joyrpc.com.caucho.hessian.io.AutowiredObjectDeserializer;
import io.joyrpc.protocol.dubbo.message.DubboInvocation;
import io.joyrpc.util.ClassUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;

import static io.joyrpc.protocol.dubbo.message.DubboInvocation.DUBBO_GROUP_KEY;
import static io.joyrpc.protocol.dubbo.message.DubboInvocation.DUBBO_VERSION_KEY;

public class DubboInvocationDeserializer implements AutowiredObjectDeserializer {

    @Override
    public Class<?> getType() {
        return DubboInvocation.class;
    }

    @Override
    public boolean isReadResolve() {
        return false;
    }

    @Override
    public Object readObject(AbstractHessianInput in) throws IOException {

        String dubboVersion = in.readString();
        String className = in.readString();
        String version = in.readString();
        String methodName = in.readString();
        String desc = in.readString();

        Object[] args;
        Class<?>[] pts;
        Method method;

        try {
            method = ClassUtils.getPublicMethod(className, methodName);
            pts = method.getParameterTypes();
            if (pts.length == 0) {
                args = new Object[0];
            } else {
                args = new Object[pts.length];
                for (int i = 0; i < args.length; i++) {
                    args[i] = in.readObject(pts[i]);
                }
            }
        } catch (Exception e) {
            throw new IOException("Read dubbo invocation data failed.", e);
        }

        //获取传参信息
        Map<String, Object> attachments = (Map<String, Object>) in.readObject(Map.class);
        //设置 dubboVersion
        attachments.put(DUBBO_VERSION_KEY, dubboVersion);
        //获取别名
        String alias = (String) attachments.get(DUBBO_GROUP_KEY);
        //创建DubboInvocation对象
        DubboInvocation invocation = new DubboInvocation();
        invocation.setClassName(className);
        invocation.setMethodName(methodName);
        invocation.setAlias(alias);
        invocation.setAttachments(attachments);
        invocation.setMethod(method);
        invocation.setArgs(args);
        invocation.setVersion(version);
        invocation.setArgsType(pts);
        invocation.setParameterTypesDesc(desc);
        return invocation;
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
