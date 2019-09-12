package io.joyrpc.util;

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

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.ByteOrder;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

/**
 * 内存操作
 */
public class Memory {
    public static final Unsafe UNSAFE;
    protected static final MemoryOperation memory;
    protected static final long BYTE_ARRAY_OFFSET;
    protected static final long SHORT_ARRAY_OFFSET;
    protected static final long SHORT_ARRAY_STRIDE;
    public static final boolean FAST_ACCESS_SUPPORTED;

    static {
        try {
            final PrivilegedExceptionAction<Unsafe> action = new PrivilegedExceptionAction<Unsafe>() {
                public Unsafe run() throws Exception {
                    Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                    theUnsafe.setAccessible(true);
                    return (Unsafe) theUnsafe.get(null);
                }
            };
            UNSAFE = AccessController.doPrivileged(action);
            BYTE_ARRAY_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);
            SHORT_ARRAY_OFFSET = UNSAFE.arrayBaseOffset(short[].class);
            SHORT_ARRAY_STRIDE = UNSAFE.arrayIndexScale(short[].class);

            // Try to only load one implementation of Memory to assure the call sites are monomorphic (fast)
            MemoryOperation target = null;

            // TODO enable UnsafeMemory on big endian machines
            //
            // The current UnsafeMemory code assumes the machine is little endian, and will
            // not work correctly on big endian CPUs.  For now, we will disable UnsafeMemory on
            // big endian machines.  This will make the code significantly slower on big endian.
            // In the future someone should add the necessary flip bytes calls to make this
            // work efficiently on big endian machines.
            if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                try {
                    MemoryOperation unsafeMemory = new UnsafeMemory();
                    if (unsafeMemory.getInt(new byte[4], 0) == 0) {
                        target = unsafeMemory;
                    }
                } catch (Throwable ignored) {
                }
            }
            if (target == null) {
                try {
                    MemoryOperation slowMemory = new SlowMemory();
                    if (slowMemory.getInt(new byte[4], 0) == 0) {
                        target = slowMemory;
                    } else {
                        throw new AssertionError("SlowMemory class is broken!");
                    }
                } catch (Throwable ignored) {
                    throw new AssertionError("Could not find SlowMemory class");
                }
            }
            memory = target;
            FAST_ACCESS_SUPPORTED = memory.fastAccessSupported();
        } catch (Exception e) {
            throw new RuntimeException("Unable to load unsafe", e);
        }

    }

    protected Memory() {
    }

    /**
     * 读取字节数据
     *
     * @param data  缓冲器
     * @param index 索引
     * @return 字节
     */
    public static int getByte(final byte[] data, final int index) {
        return memory.getByte(data, index);
    }

    /**
     * 读取短整数数据
     *
     * @param data  缓冲器
     * @param index 索引
     * @return 短整数
     */
    public static int getShort(final short[] data, final int index) {
        return memory.getShort(data, index);
    }

    /**
     * 读取整形数据
     *
     * @param data  缓冲器
     * @param index 索引
     * @return 整数
     */
    public static int getInt(final byte[] data, final int index) {
        return memory.getInt(data, index);
    }

    /**
     * 读取长整形数据
     *
     * @param data  缓冲器
     * @param index 索引
     * @return 长整数
     */
    public static long getLong(final byte[] data, final int index) {
        return memory.getLong(data, index);
    }

    /**
     * 拷贝长整数(8字节)
     *
     * @param src       源数据
     * @param srcIndex  源位置
     * @param dest      目标数组
     * @param destIndex 目标位置
     */
    public static void copyLong(final byte[] src, final int srcIndex, final byte[] dest, final int destIndex) {
        memory.copyLong(src, srcIndex, dest, destIndex);
    }

    /**
     * 拷贝数据
     *
     * @param src       源数据
     * @param srcIndex  源位置
     * @param dest      目标数组
     * @param destIndex 目标位置
     * @param length    长度
     */
    public static void copyMemory(final byte[] src, final int srcIndex, final byte[] dest, final int destIndex,
                                  final int length) {
        memory.copyMemory(src, srcIndex, dest, destIndex, length);
    }


    /**
     * 内存操作
     */
    interface MemoryOperation {
        /**
         * 是否只是快速操作
         *
         * @return
         */
        boolean fastAccessSupported();

        /**
         * 读取字节数据
         *
         * @param data  缓冲器
         * @param index 索引
         * @return 字节
         */
        int getByte(byte[] data, int index);

        /**
         * 读取短整数数据
         *
         * @param data  缓冲器
         * @param index 索引
         * @return 短整数
         */
        int getShort(short[] data, int index);

        /**
         * 读取整形数据
         *
         * @param data  缓冲器
         * @param index 索引
         * @return 整数
         */
        int getInt(byte[] data, int index);

        /**
         * 读取长整形数据
         *
         * @param data  缓冲器
         * @param index 索引
         * @return 长整数
         */
        long getLong(byte[] data, int index);

        /**
         * 拷贝长整数(8字节)
         *
         * @param src       源数据
         * @param srcIndex  源位置
         * @param dest      目标数组
         * @param destIndex 目标位置
         */
        void copyLong(byte[] src, int srcIndex, byte[] dest, int destIndex);

        /**
         * 拷贝数据
         *
         * @param src       源数据
         * @param srcIndex  源位置
         * @param dest      目标数组
         * @param destIndex 目标位置
         * @param length    长度
         */
        void copyMemory(byte[] src, int srcIndex, byte[] dest, int destIndex, int length);
    }

    static class SlowMemory implements MemoryOperation {
        @Override
        public boolean fastAccessSupported() {
            return false;
        }

        @Override
        public int getShort(final short[] data, final int index) {
            return data[index] & 0xFFFF;
        }

        @Override
        public int getByte(final byte[] data, final int index) {
            return data[index] & 0xFF;
        }

        @Override
        public int getInt(final byte[] data, final int index) {
            return (data[index] & 0xff) | (data[index + 1] & 0xff) << 8 | (data[index + 2] & 0xff) << 16 |
                    (data[index + 3] & 0xff) << 24;
        }

        @Override
        public void copyLong(final byte[] src, final int srcIndex, final byte[] dest, final int destIndex) {
            for (int i = 0; i < 8; i++) {
                dest[destIndex + i] = src[srcIndex + i];
            }
        }

        @Override
        public long getLong(final byte[] data, final int index) {
            return (data[index] & 0xffL) | (data[index + 1] & 0xffL) << 8 | (data[index + 2] & 0xffL) << 16 |
                    (data[index + 3] & 0xffL) << 24 | (data[index + 4] & 0xffL) << 32 | (data[index + 5] & 0xffL) <<
                    40 | (data[index + 6] & 0xffL) << 48 | (data[index + 7] & 0xffL) << 56;
        }

        @Override
        public void copyMemory(final byte[] src, final int srcIndex, final byte[] dest, final int destIndex,
                               final int length) {
            System.arraycopy(src, srcIndex, dest, destIndex, length);
        }
    }

    static class UnsafeMemory implements MemoryOperation {
        @Override
        public boolean fastAccessSupported() {
            return true;
        }

        @Override
        public int getShort(final short[] data, final int index) {
            assert index >= 0;
            assert index <= data.length;
            return UNSAFE.getShort(data, SHORT_ARRAY_OFFSET + (index * SHORT_ARRAY_STRIDE)) & 0xFFFF;
        }

        @Override
        public int getByte(final byte[] data, final int index) {
            assert index >= 0;
            assert index <= data.length;
            return UNSAFE.getByte(data, BYTE_ARRAY_OFFSET + index) & 0xFF;
        }

        @Override
        public int getInt(final byte[] data, final int index) {
            assert index >= 0;
            assert index + 4 <= data.length;
            return UNSAFE.getInt(data, BYTE_ARRAY_OFFSET + index);
        }

        @Override
        public void copyLong(final byte[] src, final int srcIndex, final byte[] dest, final int destIndex) {
            assert srcIndex >= 0;
            assert srcIndex + 8 <= src.length;
            assert destIndex >= 0;
            assert destIndex + 8 <= dest.length;
            long value = UNSAFE.getLong(src, BYTE_ARRAY_OFFSET + srcIndex);
            UNSAFE.putLong(dest, (BYTE_ARRAY_OFFSET + destIndex), value);
        }

        @Override
        public long getLong(final byte[] data, final int index) {
            assert index > 0;
            assert index + 4 < data.length;
            return UNSAFE.getLong(data, BYTE_ARRAY_OFFSET + index);
        }

        @Override
        public void copyMemory(final byte[] src, final int srcIndex, final byte[] dest, final int destIndex,
                               final int length) {
            assert srcIndex >= 0;
            assert srcIndex + length <= src.length;
            assert destIndex >= 0;
            assert destIndex + length <= dest.length;
            UNSAFE.copyMemory(src, BYTE_ARRAY_OFFSET + srcIndex, dest, BYTE_ARRAY_OFFSET + destIndex, length);
        }
    }

}
