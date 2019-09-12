package io.joyrpc.codec.crypto;

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

import javax.crypto.Cipher;
import java.security.GeneralSecurityException;
import java.security.Key;

/**
 * 加解密
 */
public abstract class AbstractCipherCrypto implements Crypto {

    // 名称
    protected String name;
    // 算法
    protected String cipher;
    // 密匙位长度
    protected int length;

    public AbstractCipherCrypto(final String name, final String cipher, final int length) {
        this.name = name;
        this.cipher = cipher;
        this.length = length;
    }

    @Override
    public byte[] encrypt(final byte[] source, final byte[] key) throws GeneralSecurityException {
        //实例化
        Cipher cp = Cipher.getInstance(cipher);
        //初始化，设置为加密模式
        cp.init(Cipher.ENCRYPT_MODE, encryptKey(key, cp.getBlockSize()));
        return cipher(source, cp, true);

    }

    @Override
    public byte[] decrypt(final byte[] source, final byte[] key) throws GeneralSecurityException {
        //实例化
        Cipher cp = Cipher.getInstance(cipher);
        //初始化，设置为解密模式
        cp.init(Cipher.DECRYPT_MODE, decryptKey(key, cp.getBlockSize()));
        //执行操作
        return cipher(source, cp, false);
    }

    /**
     * 加解密
     *
     * @param source  数据源
     * @param cipher  加解密对象
     * @param encrypt 加密标识
     * @return
     * @throws GeneralSecurityException
     */
    protected byte[] cipher(final byte[] source, final Cipher cipher, final boolean encrypt)
            throws GeneralSecurityException {
        //执行操作
        return cipher.doFinal(source);
    }

    /**
     * 创建加密key
     *
     * @param key
     * @param blockSize
     * @return
     * @throws GeneralSecurityException
     */
    protected abstract Key encryptKey(final byte[] key, final int blockSize) throws GeneralSecurityException;

    /**
     * 创建解密key
     *
     * @param key
     * @param blockSize
     * @return
     * @throws GeneralSecurityException
     */
    protected Key decryptKey(final byte[] key, final int blockSize) throws GeneralSecurityException {
        return encryptKey(key, blockSize);
    }

}
