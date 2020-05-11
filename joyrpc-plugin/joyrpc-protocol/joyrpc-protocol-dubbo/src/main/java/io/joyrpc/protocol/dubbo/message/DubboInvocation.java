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

import io.joyrpc.codec.serialization.ObjectInputReader;
import io.joyrpc.codec.serialization.ObjectOutputWriter;
import io.joyrpc.protocol.dubbo.serialization.DubboInvocationReader;
import io.joyrpc.protocol.dubbo.serialization.DubboInvocationWriter;
import io.joyrpc.protocol.message.Invocation;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Dubbo调用
 */
public class DubboInvocation extends Invocation {

    public static String DUBBO_VERSION_KEY = "dubbo";
    public static String DUBBO_GROUP_KEY = "group";
    public static String DUBBO_PATH_KEY = "path";
    public static String DUBBO_INTERFACE_KEY = "path";
    public static String DUBBO_APPLICATION_KEY = "remote.application";
    public static String DUBBO_SERVICE_VERSION_KEY = "version";
    public static String DUBBO_TIMEOUT_KEY = "timeout";
    public static String DUBBO_GENERIC_KEY = "generic";

    private String parameterTypesDesc;

    private Map<Object, Object> attributes = new HashMap<>();

    private transient String version = "0.0.0";

    /**
     * 心跳标识
     */
    protected transient boolean heartbeat = false;

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
     * java序列化
     *
     * @param out
     * @throws IOException
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        new DubboInvocationWriter(new ObjectOutputWriter(out)).write(this);
    }

    /**
     * java反序列化
     *
     * @param in
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        new DubboInvocationReader(new ObjectInputReader(in)).read(this);
    }

}
