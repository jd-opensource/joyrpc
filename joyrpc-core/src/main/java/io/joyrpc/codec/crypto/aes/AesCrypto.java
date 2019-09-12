package io.joyrpc.codec.crypto.aes;

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

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.Key;

/**
 * AES/ECB/PKCS5Padding加解密
 */
@Extension("AES")
public class AesCrypto extends AbstractCipherCrypto {

    public AesCrypto() {
        super("AES", "AES/CBC/PKCS5Padding", 128);
    }

    @Override
    public byte[] encrypt(final byte[] source, final byte[] key) throws GeneralSecurityException {
        //实例化
        Cipher cp = Cipher.getInstance(cipher);
        //初始化，设置为加密模式
        cp.init(Cipher.ENCRYPT_MODE, encryptKey(key, cp.getBlockSize()), new IvParameterSpec(key));
        return cp.doFinal(source);
    }

    @Override
    public byte[] decrypt(final byte[] source, final byte[] key) throws GeneralSecurityException {
        //实例化
        Cipher cp = Cipher.getInstance(cipher);
        //初始化，设置为解密模式
        cp.init(Cipher.DECRYPT_MODE, decryptKey(key, cp.getBlockSize()), new IvParameterSpec(key));
        //执行操作
        return cp.doFinal(source);
    }

    @Override
    protected Key encryptKey(final byte[] key, final int blockSize) throws GeneralSecurityException {
        byte[] data = key;
        int length = key.length;
        if (length % blockSize != 0) {
            length = length + (blockSize - (length % blockSize));
            data = new byte[length];
            System.arraycopy(key, 0, data, 0, key.length);

        }
        return new SecretKeySpec(data, name);
    }
}
