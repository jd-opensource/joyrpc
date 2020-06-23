package io.joyrpc.protocol.dubbo;

import io.joyrpc.Invoker;
import io.joyrpc.invoker.ServiceManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DubboCallbackInsIdManager {

    protected static final Map<String, String> callbackInsIds = new ConcurrentHashMap<>();

    public static String toDubbo(Object callbackInsId) {
        Invoker invoker = ServiceManager.getConsumerCallback().getInvoker(callbackInsId.toString());
        if (invoker != null) {
            String dubboId = String.valueOf(System.identityHashCode(invoker));
            callbackInsIds.put(dubboId, callbackInsId.toString());
            return dubboId;
        }
        return "0";
    }

    public static String toJoy(Object callbackInsId) {
        return callbackInsIds.get(callbackInsId.toString());
    }
}
