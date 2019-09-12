package io.joyrpc.codec.crypto.hmac;

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

import io.joyrpc.codec.crypto.Signature;
import io.joyrpc.extension.Extension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

/**
 * HMAC加密
 */
@Extension("HMAC")
public class HmacSignature implements Signature {

    protected String hmacType;

    public HmacSignature() {
        this("HmacMD5");
    }

    public HmacSignature(String hmacType) {
        this.hmacType = hmacType;
    }

    @Override
    public byte[] encrypt(final byte[] source, final byte[] key) throws GeneralSecurityException {
        //根据给定的字节数组构造一个密钥,第二参数指定一个密钥算法的名称
        SecretKeySpec signinKey = new SecretKeySpec(key, hmacType);
        //生成一个指定 Mac 算法 的 Mac 对象
        Mac mac = Mac.getInstance(hmacType);
        //用给定密钥初始化 Mac 对象
        mac.init(signinKey);
        //完成 Mac 操作
        return mac.doFinal(source);
    }
}
