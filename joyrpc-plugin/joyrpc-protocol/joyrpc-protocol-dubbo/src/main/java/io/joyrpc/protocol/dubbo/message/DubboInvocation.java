package io.joyrpc.protocol.dubbo.message;

import io.joyrpc.protocol.message.Invocation;

import java.util.HashMap;
import java.util.Map;

public class DubboInvocation extends Invocation {

    public static String DUBBO_VERSION_KEY = "dubbo";
    public static String DUBBO_GROUP_KEY = "group";

    private String targetServiceUniqueName;

    private String serviceName;

    private String parameterTypesDesc;

    private String[] compatibleParamSignatures;

    private Map<Object, Object> attributes = new HashMap<>();

    private transient String version = "0.0.0";

    public String getTargetServiceUniqueName() {
        if (targetServiceUniqueName == null && className != null) {
            targetServiceUniqueName = alias == null ? className + "/" + alias + ":" + version : className + ":" + version;
        }
        return targetServiceUniqueName;
    }

    public void setTargetServiceUniqueName(String targetServiceUniqueName) {
        this.targetServiceUniqueName = targetServiceUniqueName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getParameterTypesDesc() {
        return parameterTypesDesc;
    }

    public void setParameterTypesDesc(String parameterTypesDesc) {
        this.parameterTypesDesc = parameterTypesDesc;
    }

    public String[] getCompatibleParamSignatures() {
        return compatibleParamSignatures;
    }

    public void setCompatibleParamSignatures(String[] compatibleParamSignatures) {
        this.compatibleParamSignatures = compatibleParamSignatures;
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

}
