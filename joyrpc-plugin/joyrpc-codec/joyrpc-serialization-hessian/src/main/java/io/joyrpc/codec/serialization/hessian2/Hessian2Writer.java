package io.joyrpc.codec.serialization.hessian2;

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

import io.joyrpc.codec.serialization.ObjectWriter;
import io.joyrpc.com.caucho.hessian.io.Hessian2Output;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * HessianLite 输出
 */
public class Hessian2Writer implements ObjectWriter {

    protected final Hessian2Output hessian2Output;

    public Hessian2Writer(Hessian2Output hessian2Output) {
        this.hessian2Output = hessian2Output;
    }

    @Override
    public void writeObject(final Object obj) throws IOException {
        hessian2Output.writeObject(obj);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        hessian2Output.writeBytes(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        hessian2Output.flush();
    }

    @Override
    public void close() throws IOException {
        hessian2Output.close();
    }

    @Override
    public void writeBoolean(final boolean v) throws IOException {
        hessian2Output.writeBoolean(v);
    }

    @Override
    public void writeByte(final int v) throws IOException {
        hessian2Output.writeInt(v);
    }

    @Override
    public void writeShort(final int v) throws IOException {
        hessian2Output.writeInt(v);
    }

    @Override
    public void writeChar(final int v) throws IOException {
        hessian2Output.writeInt(v);
    }

    @Override
    public void writeInt(final int v) throws IOException {
        hessian2Output.writeInt(v);
    }

    @Override
    public void writeLong(final long v) throws IOException {
        hessian2Output.writeLong(v);
    }

    @Override
    public void writeFloat(final float v) throws IOException {
        hessian2Output.writeDouble(v);
    }

    @Override
    public void writeDouble(final double v) throws IOException {
        hessian2Output.writeDouble(v);
    }

    @Override
    public void writeString(final String value, final Charset charset, final boolean zeroNull, final boolean shortLength) throws IOException {
        hessian2Output.writeString(value);
    }
}
