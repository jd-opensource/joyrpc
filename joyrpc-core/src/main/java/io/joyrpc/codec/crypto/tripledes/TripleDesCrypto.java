package io.joyrpc.codec.crypto.tripledes;

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

import io.joyrpc.codec.crypto.AbstractCipherCrypto;
import io.joyrpc.extension.Extension;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.spec.KeySpec;

/**
 * 3DES编码
 */
@Extension("DESede")
public class TripleDesCrypto extends AbstractCipherCrypto {

    public TripleDesCrypto() {
        super("DESede", "DESede/ECB/PKCS5Padding", 168);
    }

    @Override
    protected Key encryptKey(final byte[] key, final int blockSize) throws GeneralSecurityException {
        //实例化Des密钥
        KeySpec dks = new DESedeKeySpec(key);
        //实例化密钥工厂
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(name);
        //生成密钥
        return keyFactory.generateSecret(dks);
    }

}
