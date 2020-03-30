package io.joyrpc.constants;

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


/**
 * enum for head key number
 */
public enum HeadKey {

    /**
     * 请求超时时间
     */
    timeout((byte) 1, Integer.class),
    /**
     * 回调函数对应的实例id
     */
    callbackInsId((byte) 5, String.class),
    /**
     * 客户端的版本
     */
    version((byte) 7, Short.class),
    /**
     * 请求的语言（针对跨语言 1c++ 2lua）
     */
    srcLanguage((byte) 8, Byte.class),
    /**
     * 返回结果（针对跨语言 0成功 1失败）
     */
    responseCode((byte) 9, Byte.class),
    /**
     * 检查消费者调用的提供者是否正常
     */
    checkProvider((byte) 10, String.class),
    /**
     * 兼容老版本的安全认证，检查是否认证成功（1成功，则反之）
     */
    checkAuth((byte) 11, String.class),
    /**
     * 兼容老版本的网关请求
     */
    generic((byte) 12, Byte.class);
    /**
     * 值
     */
    private byte key;
    /**
     * 类型
     */
    private Class type;

    /**
     * 构造函数
     *
     * @param b
     * @param clazz
     */
    HeadKey(byte b, Class clazz) {
        this.key = b;
        this.type = clazz;
    }

    public byte getKey() {
        return key;
    }

    public Class getType() {
        return type;
    }

    public static HeadKey valueOf(final byte num) {
        switch (num) {
            case 1:
                return timeout;
            case 5:
                return callbackInsId;
            case 7:
                return version;
            case 8:
                return srcLanguage;
            case 9:
                return responseCode;
            case 10:
                return checkProvider;
            case 11:
                return checkAuth;
            case 12:
                return generic;
            default:
                throw new IllegalArgumentException("Unknown head key value: " + num);
        }
    }

}
