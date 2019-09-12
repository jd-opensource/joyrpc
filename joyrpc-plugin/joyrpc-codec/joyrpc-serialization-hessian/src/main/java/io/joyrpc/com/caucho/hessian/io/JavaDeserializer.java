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

import io.joyrpc.com.caucho.hessian.io.FieldDeserializer2Factory.NullFieldDeserializer;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.HashMap;

/**
 * Serializing an object for known object types.
 */
public class JavaDeserializer extends AbstractMapDeserializer {
    private Class<?> _type;
    private HashMap<?, FieldDeserializer2> _fieldMap;
    private Method _readResolve;
    private Constructor<?> _constructor;
    private Object[] _constructorArgs;

    public JavaDeserializer(Class<?> cl, FieldDeserializer2Factory fieldFactory) {
        _type = cl;
        _fieldMap = getFieldMap(cl, fieldFactory);

        _readResolve = getReadResolve(cl);

        if (_readResolve != null) {
            _readResolve.setAccessible(true);
        }

        _constructor = getConstructor(cl);
        _constructorArgs = getConstructorArgs(_constructor);
    }

    protected Constructor<?> getConstructor(Class<?> cl) {
        Constructor<?>[] constructors = cl.getDeclaredConstructors();
        long bestCost = Long.MAX_VALUE;

        Constructor<?> constructor = null;

        for (int i = 0; i < constructors.length; i++) {
            Class<?>[] param = constructors[i].getParameterTypes();
            long cost = 0;

            for (int j = 0; j < param.length; j++) {
                cost = 4 * cost;

                if (Object.class.equals(param[j])) {
                    cost += 1;
                } else if (String.class.equals(param[j])) {
                    cost += 2;
                } else if (int.class.equals(param[j])) {
                    cost += 3;
                } else if (long.class.equals(param[j])) {
                    cost += 4;
                } else if (param[j].isPrimitive()) {
                    cost += 5;
                } else {
                    cost += 6;
                }
            }

            if (cost < 0 || cost > (1 << 48)) {
                cost = 1 << 48;
            }

            cost += (long) param.length << 48;

            if (cost < bestCost) {
                constructor = constructors[i];
                bestCost = cost;
            }
        }

        if (constructor != null) {
            constructor.setAccessible(true);
        }

        return constructor;
    }

    protected Object[] getConstructorArgs(Constructor<?> constructor) {
        Object[] constructorArgs = null;

        if (constructor != null) {
            Class<?>[] params = constructor.getParameterTypes();
            constructorArgs = new Object[params.length];
            for (int i = 0; i < params.length; i++) {
                constructorArgs[i] = getParamArg(params[i]);
            }
        }

        return constructorArgs;
    }

    @Override
    public Class<?> getType() {
        return _type;
    }

    @Override
    public boolean isReadResolve() {
        return _readResolve != null;
    }

    public Object readMap(AbstractHessianInput in) throws IOException {
        try {
            Object obj = instantiate();

            return readMap(in, obj);
        } catch (IOException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IOExceptionWrapper(_type.getName() + ":" + e.getMessage(), e);
        }
    }

    @Override
    public Object[] createFields(int len) {
        return new FieldDeserializer2[len];
    }

    @Override
    public Object createField(String name) {
        Object reader = _fieldMap.get(name);

        if (reader == null) {
            reader = NullFieldDeserializer.DESER;
        }

        return reader;
    }

    @Override
    public Object readObject(AbstractHessianInput in,
                             Object[] fields) throws IOException {
        try {
            Object obj = instantiate();

            return readObject(in, obj, (FieldDeserializer2[]) fields);
        } catch (IOException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IOExceptionWrapper(_type.getName() + ":" + e.getMessage(), e);
        }
    }

    @Override
    public Object readObject(AbstractHessianInput in,
                             String[] fieldNames) throws IOException {
        try {
            Object obj = instantiate();

            return readObject(in, obj, fieldNames);
        } catch (IOException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IOExceptionWrapper(_type.getName() + ":" + e.getMessage(), e);
        }
    }

    /**
     * Returns the readResolve method
     */
    protected Method getReadResolve(Class<?> cl) {
        for (; cl != null; cl = cl.getSuperclass()) {
            Method[] methods = cl.getDeclaredMethods();

            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];

                if (method.getName().equals("readResolve")
                        && method.getParameterTypes().length == 0) {
                    return method;
                }
            }
        }

        return null;
    }

    public Object readMap(AbstractHessianInput in, Object obj) throws IOException {
        try {
            int ref = in.addRef(obj);

            while (!in.isEnd()) {
                Object key = in.readObject();

                FieldDeserializer2 deser = _fieldMap.get(key);

                if (deser != null) {
                    deser.deserialize(in, obj);
                } else {
                    in.readObject();
                }
            }

            in.readMapEnd();

            Object resolve = resolve(in, obj);

            if (obj != resolve) {
                in.setRef(ref, resolve);
            }

            return resolve;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOExceptionWrapper(e);
        }
    }

    private Object readObject(AbstractHessianInput in,
                              Object obj,
                              FieldDeserializer2[] fields) throws IOException {
        try {
            int ref = in.addRef(obj);

            for (FieldDeserializer2 reader : fields) {
                reader.deserialize(in, obj);
            }

            Object resolve = resolve(in, obj);

            if (obj != resolve) {
                in.setRef(ref, resolve);
            }

            return resolve;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOExceptionWrapper(obj.getClass().getName() + ":" + e, e);
        }
    }

    public Object readObject(AbstractHessianInput in,
                             Object obj,
                             String[] fieldNames) throws IOException {
        try {
            int ref = in.addRef(obj);

            for (String fieldName : fieldNames) {
                FieldDeserializer2 reader = _fieldMap.get(fieldName);

                if (reader != null) {
                    reader.deserialize(in, obj);
                } else {
                    in.readObject();
                }
            }

            Object resolve = resolve(in, obj);

            if (obj != resolve) {
                in.setRef(ref, resolve);
            }

            return resolve;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOExceptionWrapper(obj.getClass().getName() + ":" + e, e);
        }
    }

    protected Object resolve(AbstractHessianInput in, Object obj) throws Exception {
        // if there's a readResolve method, call it
        try {
            if (_readResolve != null) {
                return _readResolve.invoke(obj, new Object[0]);
            }
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            } else {
                throw e;
            }
        }

        return obj;
    }

    protected Object instantiate() throws Exception {
        try {
            if (_constructor != null) {
                return _constructor.newInstance(_constructorArgs);
            } else {
                return _type.newInstance();
            }
        } catch (Exception e) {
            throw new HessianProtocolException("'" + _type.getName() + "' could not be instantiated", e);
        }
    }

    /**
     * Creates a map of the classes fields.
     */
    protected HashMap<String, FieldDeserializer2>
    getFieldMap(Class<?> cl, FieldDeserializer2Factory fieldFactory) {
        HashMap<String, FieldDeserializer2> fieldMap
                = new HashMap<String, FieldDeserializer2>();

        for (; cl != null; cl = cl.getSuperclass()) {
            Field[] fields = cl.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];

                if (Modifier.isTransient(field.getModifiers())
                        || Modifier.isStatic(field.getModifiers())) {
                    continue;
                } else if (fieldMap.get(field.getName()) != null) {
                    continue;
                }

	/*
        // XXX: could parameterize the handler to only deal with public
        try {
          field.setAccessible(true);
        } catch (Throwable e) {
          e.printStackTrace();
        }
	*/

                FieldDeserializer2 deser = fieldFactory.create(field);

                fieldMap.put(field.getName(), deser);
            }
        }

        return fieldMap;
    }

    /**
     * Creates a map of the classes fields.
     */
    protected static Object getParamArg(Class<?> cl) {
        if (!cl.isPrimitive()) {
            return null;
        } else if (boolean.class.equals(cl)) {
            return Boolean.FALSE;
        } else if (byte.class.equals(cl)) {
            return new Byte((byte) 0);
        } else if (short.class.equals(cl)) {
            return new Short((short) 0);
        } else if (char.class.equals(cl)) {
            return new Character((char) 0);
        } else if (int.class.equals(cl)) {
            return Integer.valueOf(0);
        } else if (long.class.equals(cl)) {
            return Long.valueOf(0);
        } else if (float.class.equals(cl)) {
            return Float.valueOf(0);
        } else if (double.class.equals(cl)) {
            return Double.valueOf(0);
        } else {
            throw new UnsupportedOperationException();
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
}
