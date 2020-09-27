package io.joyrpc.protocol;
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

import io.joyrpc.codec.compression.Compression;
import io.joyrpc.codec.serialization.Serialization;
import io.joyrpc.constants.Constants;
import io.joyrpc.exception.LafException;
import io.joyrpc.extension.Parametric;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.util.GenericClass;
import io.joyrpc.util.GenericMethod;
import io.joyrpc.util.StringUtils;

import java.lang.reflect.Method;
import java.util.function.Supplier;

import static io.joyrpc.Plugin.COMPRESSION;
import static io.joyrpc.Plugin.SERIALIZATION;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.util.ClassUtils.*;

/**
 * Http调用反序列化器
 */
public class AbstractHttpDecoder {
    /**
     * URL
     */
    protected URL url;
    /**
     * 路径
     */
    protected String[] paths;
    /**
     * 参数
     */
    protected Parametric header;
    /**
     * Body
     */
    protected byte[] body;
    /**
     * 异常提供者
     */
    protected Supplier<LafException> error;
    /**
     * 接口名称
     */
    protected transient String className;
    /**
     * 分组
     */
    protected transient String alias;
    /**
     * 接口类
     */
    protected transient Class<?> intfClass;
    /**
     * 泛化类
     */
    protected transient GenericClass genericClass;
    /**
     * 方法名称
     */
    protected transient String methodName;
    /**
     * 方法
     */
    protected transient Method method;
    /**
     * 泛化方法
     */
    protected transient GenericMethod genericMethod;

    protected AbstractHttpDecoder url(final URL url) {
        this.url = url;
        return this;
    }

    protected AbstractHttpDecoder paths(final String[] paths) {
        this.paths = paths;
        return this;
    }

    protected AbstractHttpDecoder header(final Parametric header) {
        this.header = header;
        return this;
    }

    protected AbstractHttpDecoder body(final byte[] body) {
        this.body = body;
        return this;
    }

    protected AbstractHttpDecoder error(final Supplier<LafException> error) {
        this.error = error;
        return this;
    }

    /**
     * 获取压缩
     *
     * @param headerKey 头
     * @return 压缩
     */
    protected Compression getCompression(final String headerKey) {
        //Content-Encoding:gzip
        String type = header.getString(headerKey);
        return type == null ? null : COMPRESSION.get(type);
    }

    /**
     * 获取序列化
     *
     * @param headerKey 头
     * @param def       默认序列化
     * @return 序列化
     */
    protected Serialization getSerialization(final String headerKey, final Serialization def) {
        Serialization result = null;
        //Content-Type:application/grpc+proto
        //Content-Type:application/grpc+json
        String type = header.getString(headerKey);
        if (type != null) {
            int pos = type.lastIndexOf('+');
            if (pos > 0) {
                type = type.substring(pos + 1);
                result = SERIALIZATION.get(type);
            }
        }
        return result == null ? def : result;
    }

    /**
     * 构建调用对象，存在多个调用合并请求的情况
     *
     * @return 调用对象数组
     * @throws Exception 异常
     */
    public Invocation build() throws Exception {
        parse();
        Invocation invocation = new Invocation(className, alias == null ? "" : alias, methodName, genericMethod.getTypes()).
                addAttachment(Constants.HIDDEN_KEY_TOKEN, header.getString(KEY_TOKEN)).
                addAttachment(HIDDEN_KEY_APPID, header.getString(KEY_APPID)).
                addAttachment(HIDDEN_KEY_APPNAME, header.getString(KEY_APPNAME)).
                addAttachment(HIDDEN_KEY_APPINSID, header.getString(KEY_APPINSID));
        invocation.setClazz(intfClass);
        invocation.setMethod(method);
        invocation.setGenericMethod(genericMethod);
        invocation.setGenericTypes(genericMethod.getGenericTypes());
        build(invocation);
        //隐式传参
        header.foreach((key, value) -> {
            if (!key.isEmpty() && key.charAt(0) == Constants.HIDE_KEY_PREFIX) {
                invocation.addAttachment(key, value);
            }
        });
        parseArg(invocation);
        return invocation;
    }

    /**
     * 解析参数
     *
     * @param invocation 调用
     * @throws Exception io异常
     */
    protected void parseArg(final Invocation invocation) throws Exception {
    }

    /**
     * 构建调用
     *
     * @param invocation 调用对象
     */
    protected void build(final Invocation invocation) {
    }

    /**
     * 解析类，分组和方法
     *
     * @throws Exception 异常
     */
    protected void parse() throws Exception {
        if (paths == null) {
            String path = url.getPath();
            paths = path == null ? new String[0] : StringUtils.split(path, '/');
        }
        if (paths.length > 2) {
            className = paths[0];
            alias = paths[1];
            methodName = paths[2];
        } else if (paths.length == 2) {
            className = paths[0];
            methodName = paths[1];
            alias = header.getString(ALIAS_OPTION);
        } else {
            throw error.get();
        }
        intfClass = forName(className);
        genericClass = getGenericClass(intfClass);
        parseMethod();
    }

    /**
     * 解析方法
     *
     * @throws Exception 异常
     */
    protected void parseMethod() throws Exception {
        method = getPublicMethod(intfClass, methodName);
        genericMethod = genericClass.get(method);
    }

}
