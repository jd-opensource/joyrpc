package io.joyrpc.codec.digester;

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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MessageDigest摘要
 */
public class MessageDigester implements Digester {

    protected String algorithm;

    public MessageDigester(String algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public byte[] digest(final byte[] source) throws NoSuchAlgorithmException {
        // 获得MD5摘要算法的 MessageDigest 对象
        MessageDigest md = MessageDigest.getInstance(algorithm);
        md.reset();
        // 使用指定的字节更新摘要
        md.update(source);
        // 获得密文
        return md.digest();
    }

}
