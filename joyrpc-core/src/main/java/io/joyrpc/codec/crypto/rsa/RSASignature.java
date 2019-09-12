package io.joyrpc.codec.crypto.rsa;

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

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * RSA加密
 */
@Extension("RSA")
public class RSASignature implements Signature {

    protected String keyAlgorithm;

    protected String signatureAlgorithm;

    public RSASignature() {
        this("MD5withRSA");
    }

    public RSASignature(String signatureAlgorithm) {
        this.keyAlgorithm = "RSA";
        this.signatureAlgorithm = signatureAlgorithm;
    }

    @Override
    public byte[] encrypt(byte[] source, byte[] key) throws GeneralSecurityException {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(key);
        KeyFactory keyFactory = KeyFactory.getInstance(keyAlgorithm);
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

        java.security.Signature signature = java.security.Signature.getInstance(signatureAlgorithm);
        signature.initSign(privateKey);
        signature.update(source);
        return signature.sign();
    }

    @Override
    public boolean verify(byte[] source, byte[] key, byte[] sign) throws GeneralSecurityException {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(key);
        KeyFactory keyFactory = KeyFactory.getInstance(keyAlgorithm);
        PublicKey publicK = keyFactory.generatePublic(keySpec);
        java.security.Signature signature = java.security.Signature.getInstance(signatureAlgorithm);
        signature.initVerify(publicK);
        signature.update(source);
        return signature.verify(sign);
    }
}
