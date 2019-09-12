package io.joyrpc.codec.serialization.model;

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

import io.joyrpc.codec.serialization.Codec;
import io.joyrpc.codec.serialization.ObjectReader;
import io.joyrpc.codec.serialization.ObjectWriter;

import java.io.IOException;
import java.util.Arrays;

public class Apple implements Codec {
    protected long id;
    protected String name;
    protected byte flag;
    protected boolean good;
    protected byte[] data;

    public Apple() {
    }

    public Apple(long id, String name, byte flag, boolean good, byte[] data) {
        this.id = id;
        this.name = name;
        this.flag = flag;
        this.good = good;
        this.data = data;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte getFlag() {
        return flag;
    }

    public void setFlag(byte flag) {
        this.flag = flag;
    }

    public boolean isGood() {
        return good;
    }

    public void setGood(boolean good) {
        this.good = good;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public void encode(final ObjectWriter output) throws IOException {
        output.writeLong(id);
        output.writeString(name);
        output.writeByte((int) flag);
        output.writeBoolean(good);
        output.writeInt(data == null ? -1 : data.length);
        if (data != null && data.length > 0) {
            output.write(data);
        }
    }

    @Override
    public void decode(final ObjectReader input) throws IOException {
        id = input.readLong();
        name = input.readString();
        flag = input.readByte();
        good = input.readBoolean();
        int len = input.readInt();
        if (len >= 0) {
            data = new byte[len];
            if (len > 0) {
                input.read(data);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Apple apple = (Apple) o;

        if (id != apple.id) {
            return false;
        }
        if (flag != apple.flag) {
            return false;
        }
        if (good != apple.good) {
            return false;
        }
        if (name != null ? !name.equals(apple.name) : apple.name != null) {
            return false;
        }
        return Arrays.equals(data, apple.data);

    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (int) flag;
        result = 31 * result + (good ? 1 : 0);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }
}
