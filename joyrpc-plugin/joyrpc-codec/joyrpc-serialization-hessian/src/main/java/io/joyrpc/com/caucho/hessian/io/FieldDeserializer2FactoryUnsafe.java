/*
 * Copyright (c) 2001-2008 Caucho Technology, Inc.  All rights reserved.
 *
 * The Apache Software License, Version 1.1
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Burlap", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Scott Ferguson
 */

package io.joyrpc.com.caucho.hessian.io;

import sun.misc.Unsafe;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Serializing an object for known object types.
 */
public class FieldDeserializer2FactoryUnsafe extends FieldDeserializer2Factory {
    private static final Logger log
            = Logger.getLogger(JavaDeserializer.class.getName());

    private static boolean _isEnabled;
    @SuppressWarnings("restriction")
    private static Unsafe _unsafe;

    /**
     * Creates a map of the classes fields.
     */
    @Override
    public FieldDeserializer2 create(Field field) {
        if (Modifier.isTransient(field.getModifiers())
                || Modifier.isStatic(field.getModifiers())) {
            return NullFieldDeserializer.DESER;
        }

    /*
    // XXX: could parameterize the handler to only deal with public
    try {
      field.setAccessible(true);
    } catch (Throwable e) {
      e.printStackTrace();
    }
    */

        Class<?> type = field.getType();
        FieldDeserializer2 deser;

        if (String.class.equals(type)) {
            deser = new StringFieldDeserializer(field);
        } else if (byte.class.equals(type)) {
            deser = new ByteFieldDeserializer(field);
        } else if (char.class.equals(type)) {
            deser = new CharFieldDeserializer(field);
        } else if (short.class.equals(type)) {
            deser = new ShortFieldDeserializer(field);
        } else if (int.class.equals(type)) {
            deser = new IntFieldDeserializer(field);
        } else if (long.class.equals(type)) {
            deser = new LongFieldDeserializer(field);
        } else if (float.class.equals(type)) {
            deser = new FloatFieldDeserializer(field);
        } else if (double.class.equals(type)) {
            deser = new DoubleFieldDeserializer(field);
        } else if (boolean.class.equals(type)) {
            deser = new BooleanFieldDeserializer(field);
        } else if (java.sql.Date.class.equals(type)) {
            deser = new SqlDateFieldDeserializer(field);
        } else if (java.sql.Timestamp.class.equals(type)) {
            deser = new SqlTimestampFieldDeserializer(field);
        } else if (java.sql.Time.class.equals(type)) {
            deser = new SqlTimeFieldDeserializer(field);
        } else {
            deser = new ObjectFieldDeserializer(field);
        }

        return deser;
    }

    static class NullFieldDeserializer implements FieldDeserializer2 {
        static NullFieldDeserializer DESER = new NullFieldDeserializer();

        @Override
        public void deserialize(AbstractHessianInput in, Object obj) throws IOException {
            in.readObject();
        }
    }

    static class ObjectFieldDeserializer implements FieldDeserializer2 {
        private final Field _field;
        private final long _offset;

        @SuppressWarnings("restriction")
        ObjectFieldDeserializer(Field field) {
            _field = field;
            _offset = _unsafe.objectFieldOffset(_field);
        }

        @SuppressWarnings("restriction")
        public void deserialize(AbstractHessianInput in, Object obj) throws IOException {
            Object value = null;

            try {
                value = in.readObject(_field.getType());

                _unsafe.putObject(obj, _offset, value);
            } catch (Exception e) {
                logDeserializeError(_field, obj, value, e);
            }
        }
    }

    static class BooleanFieldDeserializer implements FieldDeserializer2 {
        private final Field _field;
        private final long _offset;

        @SuppressWarnings("restriction")
        BooleanFieldDeserializer(Field field) {
            _field = field;
            _offset = _unsafe.objectFieldOffset(_field);
        }

        @SuppressWarnings("restriction")
        public void deserialize(AbstractHessianInput in, Object obj) throws IOException {
            boolean value = false;

            try {
                value = in.readBoolean();

                _unsafe.putBoolean(obj, _offset, value);
            } catch (Exception e) {
                logDeserializeError(_field, obj, value, e);
            }
        }
    }

    static class ByteFieldDeserializer implements FieldDeserializer2 {
        private final Field _field;
        private final long _offset;

        @SuppressWarnings("restriction")
        ByteFieldDeserializer(Field field) {
            _field = field;
            _offset = _unsafe.objectFieldOffset(_field);
        }

        @SuppressWarnings("restriction")
        public void deserialize(AbstractHessianInput in, Object obj) throws IOException {
            int value = 0;

            try {
                value = in.readInt();

                _unsafe.putByte(obj, _offset, (byte) value);
            } catch (Exception e) {
                logDeserializeError(_field, obj, value, e);
            }
        }
    }

    static class CharFieldDeserializer implements FieldDeserializer2 {
        private final Field _field;
        private final long _offset;

        @SuppressWarnings("restriction")
        CharFieldDeserializer(Field field) {
            _field = field;
            _offset = _unsafe.objectFieldOffset(_field);
        }

        @SuppressWarnings("restriction")
        public void deserialize(AbstractHessianInput in, Object obj) throws IOException {
            String value = null;

            try {
                value = in.readString();

                char ch;

                if (value != null && value.length() > 0) {
                    ch = value.charAt(0);
                } else {
                    ch = 0;
                }

                _unsafe.putChar(obj, _offset, ch);
            } catch (Exception e) {
                logDeserializeError(_field, obj, value, e);
            }
        }
    }

    static class ShortFieldDeserializer implements FieldDeserializer2 {
        private final Field _field;
        private final long _offset;

        @SuppressWarnings("restriction")
        ShortFieldDeserializer(Field field) {
            _field = field;
            _offset = _unsafe.objectFieldOffset(_field);
        }

        @SuppressWarnings("restriction")
        public void deserialize(AbstractHessianInput in, Object obj) throws IOException {
            int value = 0;

            try {
                value = in.readInt();

                _unsafe.putShort(obj, _offset, (short) value);
            } catch (Exception e) {
                logDeserializeError(_field, obj, value, e);
            }
        }
    }

    static class IntFieldDeserializer implements FieldDeserializer2 {
        private final Field _field;
        private final long _offset;

        @SuppressWarnings("restriction")
        IntFieldDeserializer(Field field) {
            _field = field;
            _offset = _unsafe.objectFieldOffset(_field);
        }

        @SuppressWarnings("restriction")
        public void deserialize(AbstractHessianInput in, Object obj) throws IOException {
            int value = 0;

            try {
                value = in.readInt();

                _unsafe.putInt(obj, _offset, value);
            } catch (Exception e) {
                logDeserializeError(_field, obj, value, e);
            }
        }
    }

    static class LongFieldDeserializer implements FieldDeserializer2 {
        private final Field _field;
        private final long _offset;

        @SuppressWarnings("restriction")
        LongFieldDeserializer(Field field) {
            _field = field;
            _offset = _unsafe.objectFieldOffset(_field);
        }

        @SuppressWarnings("restriction")
        public void deserialize(AbstractHessianInput in, Object obj) throws IOException {
            long value = 0;

            try {
                value = in.readLong();

                _unsafe.putLong(obj, _offset, value);
            } catch (Exception e) {
                logDeserializeError(_field, obj, value, e);
            }
        }
    }

    static class FloatFieldDeserializer implements FieldDeserializer2 {
        private final Field _field;
        private final long _offset;

        @SuppressWarnings("restriction")
        FloatFieldDeserializer(Field field) {
            _field = field;
            _offset = _unsafe.objectFieldOffset(_field);
        }

        public void deserialize(AbstractHessianInput in, Object obj) throws IOException {
            double value = 0;

            try {
                value = in.readDouble();

                _unsafe.putFloat(obj, _offset, (float) value);
            } catch (Exception e) {
                logDeserializeError(_field, obj, value, e);
            }
        }
    }

    static class DoubleFieldDeserializer implements FieldDeserializer2 {
        private final Field _field;
        private final long _offset;

        DoubleFieldDeserializer(Field field) {
            _field = field;
            _offset = _unsafe.objectFieldOffset(_field);
        }

        @SuppressWarnings("restriction")
        public void deserialize(AbstractHessianInput in, Object obj) throws IOException {
            double value = 0;

            try {
                value = in.readDouble();

                _unsafe.putDouble(obj, _offset, value);
            } catch (Exception e) {
                logDeserializeError(_field, obj, value, e);
            }
        }
    }

    static class StringFieldDeserializer implements FieldDeserializer2 {
        private final Field _field;
        private final long _offset;

        @SuppressWarnings("restriction")
        StringFieldDeserializer(Field field) {
            _field = field;
            _offset = _unsafe.objectFieldOffset(_field);
        }

        @SuppressWarnings("restriction")
        public void deserialize(AbstractHessianInput in, Object obj) throws IOException {
            String value = null;

            try {
                value = in.readString();

                _unsafe.putObject(obj, _offset, value);
            } catch (Exception e) {
                logDeserializeError(_field, obj, value, e);
            }
        }
    }

    static class SqlDateFieldDeserializer implements FieldDeserializer2 {
        private final Field _field;
        private final long _offset;

        @SuppressWarnings("restriction")
        SqlDateFieldDeserializer(Field field) {
            _field = field;
            _offset = _unsafe.objectFieldOffset(_field);
        }

        @SuppressWarnings("restriction")
        public void deserialize(AbstractHessianInput in, Object obj) throws IOException {
            java.sql.Date value = null;

            try {
                java.util.Date date = (java.util.Date) in.readObject();

                if (date != null) {
                    value = new java.sql.Date(date.getTime());

                    _unsafe.putObject(obj, _offset, value);
                } else {
                    _unsafe.putObject(obj, _offset, null);
                }
            } catch (Exception e) {
                logDeserializeError(_field, obj, value, e);
            }
        }
    }

    static class SqlTimestampFieldDeserializer implements FieldDeserializer2 {
        private final Field _field;
        private final long _offset;

        @SuppressWarnings("restriction")
        SqlTimestampFieldDeserializer(Field field) {
            _field = field;
            _offset = _unsafe.objectFieldOffset(_field);
        }

        @SuppressWarnings("restriction")
        public void deserialize(AbstractHessianInput in, Object obj) throws IOException {
            java.sql.Timestamp value = null;

            try {
                java.util.Date date = (java.util.Date) in.readObject();

                if (date != null) {
                    value = new java.sql.Timestamp(date.getTime());

                    _unsafe.putObject(obj, _offset, value);
                } else {
                    _unsafe.putObject(obj, _offset, null);
                }
            } catch (Exception e) {
                logDeserializeError(_field, obj, value, e);
            }
        }
    }

    static class SqlTimeFieldDeserializer implements FieldDeserializer2 {
        private final Field _field;
        private final long _offset;

        @SuppressWarnings("restriction")
        SqlTimeFieldDeserializer(Field field) {
            _field = field;
            _offset = _unsafe.objectFieldOffset(_field);
        }

        @SuppressWarnings("restriction")
        public void deserialize(AbstractHessianInput in, Object obj) throws IOException {
            java.sql.Time value = null;

            try {
                java.util.Date date = (java.util.Date) in.readObject();

                if (date != null) {
                    value = new java.sql.Time(date.getTime());

                    _unsafe.putObject(obj, _offset, value);
                } else {
                    _unsafe.putObject(obj, _offset, null);
                }
            } catch (Exception e) {
                logDeserializeError(_field, obj, value, e);
            }
        }
    }

    static void logDeserializeError(Field field, Object obj, Object value,
                                    Throwable e) throws IOException {
        String fieldName = (field.getDeclaringClass().getName()
                + "." + field.getName());

        if (e instanceof HessianFieldException) {
            throw (HessianFieldException) e;
        } else if (e instanceof IOException) {
            throw new HessianFieldException(fieldName + ": " + e.getMessage(), e);
        }

        if (value != null) {
            throw new HessianFieldException(fieldName + ": " + value.getClass().getName() + " (" + value + ")"
                    + " cannot be assigned to '" + field.getType().getName() + "'", e);
        } else {
            throw new HessianFieldException(fieldName + ": " + field.getType().getName() + " cannot be assigned from null", e);
        }
    }

    static {
        boolean isEnabled = false;

        try {
            Class<?> unsafe = Class.forName("sun.misc.Unsafe");
            Field theUnsafe = null;
            for (Field field : unsafe.getDeclaredFields()) {
                if (field.getName().equals("theUnsafe")) {
                    theUnsafe = field;
                }
            }

            if (theUnsafe != null) {
                theUnsafe.setAccessible(true);
                _unsafe = (Unsafe) theUnsafe.get(null);
            }

            isEnabled = _unsafe != null;

            String unsafeProp = System.getProperty("com.caucho.hessian.unsafe");

            if ("false".equals(unsafeProp)) {
                isEnabled = false;
            }
        } catch (Throwable e) {
            log.log(Level.FINER, e.toString(), e);
        }

        _isEnabled = isEnabled;
    }
}
