package io.joyrpc.codec.serialization.fastjson2;

import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.reader.ObjectReaderProvider;

import java.lang.reflect.Type;

/**
 * 安全对象读取器提供者
 */
public class SecurityObjectReaderProvider extends ObjectReaderProvider {

    /**
     * 对象读取器黑白名单
     */
    protected final ObjectReaderBlackWhiteList whiteList;

    public SecurityObjectReaderProvider(ObjectReaderBlackWhiteList whiteList) {
        this.whiteList = whiteList;
    }

    @Override
    public ObjectReader getObjectReader(final Type objectType, final boolean fieldBased) {
        // 缓存，避免每次都过安全名单
        ObjectReader objectReader = whiteList.getObjectReader(objectType, fieldBased);
        if (objectReader != null) {
            return objectReader;
        }
        return super.getObjectReader(objectType, fieldBased);
    }


}
