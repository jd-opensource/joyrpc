package io.joyrpc.codec.serialization.fastjson2;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.reader.ObjectReader;
import io.joyrpc.permission.SerializerBlackList;
import io.joyrpc.permission.SerializerBlackWhiteList;
import io.joyrpc.permission.SerializerWhiteList;
import io.joyrpc.util.Resource;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 缓存了对象读取器的黑白名单，避免每次序列化都要判断黑白名单，名单变化的时候清空缓存
 */
public class ObjectReaderBlackWhiteList extends SerializerBlackWhiteList {

    protected volatile ObjectReaderCache cache = new ObjectReaderCache();

    public ObjectReaderBlackWhiteList(String... blackListFiles) {
        super(blackListFiles);
    }

    public ObjectReaderBlackWhiteList(Resource.Definition[] blackListFiles) {
        super(blackListFiles);
    }

    public ObjectReaderBlackWhiteList(SerializerBlackList blackList, SerializerWhiteList whiteList) {
        super(blackList, whiteList);
    }

    @Override
    public void updateBlack(Collection<String> targets) {
        cache = new ObjectReaderCache();
        super.updateBlack(targets);
    }

    @Override
    public void updateWhite(Collection<String> targets) {
        cache = new ObjectReaderCache();
        super.updateWhite(targets);
    }

    /**
     * 获取对象读取器
     *
     * @param objectType 类型
     * @param fieldBased 基于字段
     * @return 对象读取器
     */
    public ObjectReader getObjectReader(final Type objectType, final boolean fieldBased) {
        ObjectReader reader = cache.getObjectReader(objectType, fieldBased);
        if (reader != null) {
            return reader;
        }
        if (objectType != null && objectType instanceof Class && !isValid((Class<?>) objectType)) {
            throw new JSONException("Failed to decode class " + objectType + " by json serialization, it is not passed through blackWhiteList.");
        }
        return null;
    }

    /**
     * 对象读取器缓存
     */
    protected static class ObjectReaderCache {
        protected final ConcurrentMap<Type, ObjectReader> cache = new ConcurrentHashMap<>();
        protected final ConcurrentMap<Type, ObjectReader> cacheFieldBased = new ConcurrentHashMap<>();

        public ObjectReader getObjectReader(final Type objectType, final boolean fieldBased) {
            Type type = objectType == null ? Object.class : objectType;
            return fieldBased ? cacheFieldBased.get(type) : cache.get(type);
        }

    }
}
