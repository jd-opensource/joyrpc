package io.joyrpc.protocol.dubbo.message;

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

import io.joyrpc.codec.serialization.Codec;
import io.joyrpc.codec.serialization.ObjectReader;
import io.joyrpc.codec.serialization.ObjectWriter;
import io.joyrpc.constants.Constants;
import io.joyrpc.protocol.dubbo.DubboStatus;
import io.joyrpc.protocol.message.Invocation;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static io.joyrpc.protocol.dubbo.AbstractDubboProtocol.DEFALUT_DUBBO_VERSION;
import static io.joyrpc.util.ClassUtils.getPublicMethod;

/**
 * Dubbo调用
 */
public class DubboInvocation extends Invocation implements Codec {

    protected static final Map<Object, Object> EMPTY_ATTACHMENTS = new HashMap<>(0);
    public static String DUBBO_VERSION_KEY = "dubbo";
    public static String DUBBO_GROUP_KEY = "group";
    public static String DUBBO_PATH_KEY = "path";
    public static String DUBBO_INTERFACE_KEY = "path";
    public static String DUBBO_APPLICATION_KEY = "remote.application";
    public static String DUBBO_SERVICE_VERSION_KEY = "version";
    public static String DUBBO_TIMEOUT_KEY = "timeout";
    public static String DUBBO_GENERIC_KEY = "generic";
    public static String DUBBO_CALLBACK_ARG_PRE = "sys_callback_arg-";
    public static String DUBBO_CALLBACK_INSID_KEY = "callback.service.instid";
    public static String DUBBO_IS_CALLBACK_INVOKE_KEY = "_isCallBackServiceInvoke";
    protected static Set<String> GENERIC_METHODS = new HashSet<>();

    private String parameterTypesDesc;

    private Map<Object, Object> attributes = new HashMap<>();

    private transient String version = "";

    /**
     * 心跳标识
     */
    protected transient boolean heartbeat = false;

    static {
        GENERIC_METHODS.add("$invoke");
        GENERIC_METHODS.add("$invokeAsync");
        GENERIC_METHODS.add("$async");
    }

    public DubboInvocation() {
    }

    public DubboInvocation(boolean heartbeat) {
        this.heartbeat = heartbeat;
    }

    public String getParameterTypesDesc() {
        return parameterTypesDesc;
    }

    public void setParameterTypesDesc(String parameterTypesDesc) {
        this.parameterTypesDesc = parameterTypesDesc;
    }

    public void setAttachments(Map<String, Object> attachments) {
        this.attachments = attachments;
    }

    public Map<Object, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<Object, Object> attributes) {
        this.attributes = attributes;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isHeartbeat() {
        return heartbeat;
    }

    public void setHeartbeat(boolean heartbeat) {
        this.heartbeat = heartbeat;
    }

    @Override
    public boolean isGeneric() {
        if (generic == null) {
            Object attr = attachments == null ? null : attachments.get(DUBBO_GENERIC_KEY);
            if (attr instanceof String) {
                generic = Boolean.parseBoolean((String) attr);
            } else {
                generic = attr == null ? Boolean.FALSE : Boolean.TRUE.equals(attr);
            }
        }
        return generic;
    }

    /**
     * 读取调用
     *
     * @throws IOException
     */
    public void decode(final ObjectReader reader) throws IOException {
        String dubboVersion = reader.readUTF();
        String className = reader.readUTF();
        String version = reader.readUTF();
        String methodName = reader.readUTF();
        String desc = reader.readUTF();

        Object[] args;
        Class<?>[] pts;
        Method method = null;

        try {
            if (!GENERIC_METHODS.contains(methodName)) {
                method = getPublicMethod(className, methodName);
                pts = method.getParameterTypes();
                args = new Object[pts.length];
                if (args.length > 0) {
                    for (int i = 0; i < args.length; i++) {
                        args[i] = reader.readObject(pts[i]);
                    }
                }
            } else {
                args = new Object[3];
                args[0] = reader.readString();
                args[1] = reader.readObject(String[].class);
                args[2] = reader.readObject(Object[].class);
                methodName = (String) args[0];
                try {
                    method = getPublicMethod(className, methodName);
                } catch (Exception e) {
                    throw new IOException("Read dubbo invocation data failed.", e);
                }
                pts = method.getParameterTypes();
            }
        } catch (Exception e) {
            throw new IOException("Read dubbo invocation data failed.", e);
        }

        //获取传参信息
        Map<String, Object> attachments = (Map<String, Object>) reader.readObject(Map.class);
        attachments = attachments == null ? new HashMap<>() : attachments;
        //设置 dubboVersion
        attachments.put(DUBBO_VERSION_KEY, dubboVersion);
        //获取别名
        String alias = (String) attachments.get(DUBBO_GROUP_KEY);
        String appName = (String) attachments.get(DUBBO_APPLICATION_KEY);
        if (appName != null) {
            attachments.put(Constants.HIDDEN_KEY_APPNAME, appName);
        }
        //创建DubboInvocation对象
        this.className = className;
        this.alias = alias;
        this.attachments = attachments;
        this.args = args;
        this.version = version;
        this.parameterTypesDesc = desc;
        this.setArgsType(pts);
        this.methodName = methodName;
        this.method = method;
    }

    /**
     * 写调用
     *
     * @param writer 写
     * @throws IOException
     */
    public void encode(final ObjectWriter writer) throws IOException {
        //心跳响应，直接写null
        if (isHeartbeat()) {
            writer.writeObject(DubboStatus.HEARTBEAT_EVENT);
            return;
        }
        //写dubboversion
        writer.writeUTF(DEFALUT_DUBBO_VERSION);
        //写接口名
        writer.writeUTF(className);
        //写服务版本
        writer.writeUTF(version);
        //写方法名
        writer.writeUTF(methodName);
        //写参数描述
        writer.writeUTF(parameterTypesDesc);
        //写参数
        Object[] args = getArgs();
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                //TODO callback处理
                writer.writeObject(args[i]);
            }
        }
        //写attachments
        writer.writeObject(attachments == null ? EMPTY_ATTACHMENTS : attachments);
    }

}
