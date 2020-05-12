package io.joyrpc.protocol.dubbo.serialization.protostuff;

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

import io.joyrpc.codec.serialization.ObjectReader;
import io.joyrpc.util.ClassUtils;
import io.protostuff.GraphIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Dubbo protostuff 反序列化
 */
public class DubboProtostuffReader implements ObjectReader {

    /**
     * Schema
     */
    protected Schema schema;
    /**
     * 输入流
     */
    protected DataInputStream dis;

    public DubboProtostuffReader(Schema schema, InputStream inputStream) {
        this.schema = schema;
        this.dis = new DataInputStream(inputStream);
    }

    @Override
    public Object readObject() throws IOException {
        int classNameLength = dis.readInt();
        int bytesLength = dis.readInt();
        if (classNameLength < 0 || bytesLength < 0) {
            throw new IOException();
        }
        byte[] classNameBytes = new byte[classNameLength];
        dis.readFully(classNameBytes, 0, classNameLength);
        byte[] bytes = new byte[bytesLength];
        dis.readFully(bytes, 0, bytesLength);

        Object result;
        try {
            Schema schema = RuntimeSchema.getSchema(ClassUtils.forName(new String(classNameBytes)));
            result = schema.newMessage();
            GraphIOUtil.mergeFrom(bytes, result, schema);
        } catch (ClassNotFoundException e) {
            throw new IOException(e.getMessage(), e);
        }

        return result;
    }

    @Override
    public int read() throws IOException {
        return dis.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return dis.read(b, off, len);
    }

    @Override
    public int available() throws IOException {
        return dis.available();
    }

    @Override
    public void close() throws IOException {
        dis.close();
    }

    @Override
    public boolean readBoolean() throws IOException {
        return dis.readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        return dis.readByte();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return dis.readUnsignedByte();
    }

    @Override
    public short readShort() throws IOException {
        return dis.readShort();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return dis.readUnsignedShort();
    }

    @Override
    public char readChar() throws IOException {
        return dis.readChar();
    }

    @Override
    public int readInt() throws IOException {
        return dis.readInt();
    }

    @Override
    public long readLong() throws IOException {
        return dis.readLong();
    }

    @Override
    public float readFloat() throws IOException {
        return dis.readFloat();
    }

    @Override
    public double readDouble() throws IOException {
        return dis.readDouble();
    }

    @Override
    public String readLine() throws IOException {
        return readString(StandardCharsets.UTF_8, false);
    }

    @Override
    public String readUTF() throws IOException {
        return readString(StandardCharsets.UTF_8, false);
    }

    @Override
    public String readString() throws IOException {
        return readString(StandardCharsets.UTF_8, false);
    }

    @Override
    public String readString(Charset charset) throws IOException {
        return readString(charset, false);
    }
}
