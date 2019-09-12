package io.joyrpc.codec.serialization.jaxb;

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

import io.joyrpc.codec.serialization.Serialization;
import io.joyrpc.codec.serialization.Serializer;
import io.joyrpc.codec.serialization.Xml;
import io.joyrpc.exception.SerializerException;
import io.joyrpc.extension.Extension;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Jaxb序列化
 */
@Extension(value = "xml", provider = "jaxb", order = Serialization.ORDER_JAXB)
public class JaxbSerialization implements Serialization, Xml {

    @Override
    public byte getTypeId() {
        return XML_ID;
    }

    @Override
    public String getContentType() {
        return "text/xml";
    }

    @Override
    public Serializer getSerializer() {
        return JaxbSerializer.INSTANCE;
    }

    @Override
    public void marshall(final OutputStream os, final Object target) throws SerializerException {
        JaxbSerializer.INSTANCE.serialize(os, target);
    }

    @Override
    public void marshall(final Writer writer, final Object target) throws SerializerException {
        JaxbSerializer.INSTANCE.serialize(writer, target);
    }

    @Override
    public <T> T unmarshall(final Reader reader, final Class<T> clazz) throws SerializerException {
        return JaxbSerializer.INSTANCE.deserialize(reader, clazz);
    }

    @Override
    public <T> T unmarshall(final InputStream is, final Class<T> clazz) throws SerializerException {
        return JaxbSerializer.INSTANCE.deserialize(is, clazz);
    }

    /**
     * Xml序列化和反序列化实现
     */
    protected static final class JaxbSerializer implements Serializer {

        protected static final ConcurrentMap<Class, JAXBContext> contexts = new ConcurrentHashMap<Class, JAXBContext>();

        protected static final JaxbSerializer INSTANCE = new JaxbSerializer();

        protected JaxbSerializer() {
        }

        /**
         * 获取JAXB上下文
         *
         * @param clazz
         * @return
         * @throws JAXBException
         */
        protected static JAXBContext getJaxbContext(final Class clazz) throws JAXBException {
            if (clazz == null) {
                return null;
            }
            JAXBContext context = contexts.get(clazz);
            if (context == null) {
                context = JAXBContext.newInstance(clazz);
                JAXBContext exists = contexts.putIfAbsent(clazz, context);
                if (exists != null) {
                    context = exists;
                }
            }
            return context;
        }

        @Override
        public <T> void serialize(final OutputStream os, final T object) throws SerializerException {
            if (object == null) {
                return;
            }
            try {
                JAXBContext context = getJaxbContext(object.getClass());
                Marshaller marshaller = context.createMarshaller();
                marshaller.marshal(object, os);
            } catch (JAXBException e) {
                throw new SerializerException(String.format("Error occurs while serializing %s", object.getClass()), e);
            }
        }

        /**
         * 序列化
         *
         * @param writer
         * @param object
         * @param <T>
         * @throws SerializerException
         */
        public <T> void serialize(final Writer writer, final T object) throws SerializerException {
            if (object == null) {
                return;
            }
            try {
                JAXBContext context = getJaxbContext(object.getClass());
                Marshaller marshaller = context.createMarshaller();
                marshaller.marshal(object, writer);
            } catch (JAXBException e) {
                throw new SerializerException(String.format("Error occurs while serializing %s", object.getClass()), e);
            }
        }

        /**
         * 反序列化
         *
         * @param reader
         * @param type
         * @param <T>
         * @return
         * @throws SerializerException
         */
        public <T> T deserialize(final Reader reader, final Type type) throws SerializerException {
            if (type == null || !(type instanceof Class)) {
                return null;
            }
            Class clazz = (Class) type;
            Annotation annotation = clazz.getAnnotation(XmlRootElement.class);
            if (annotation == null) {
                return null;
            }
            try {
                JAXBContext context = getJaxbContext(clazz);
                Unmarshaller marshaller = context.createUnmarshaller();
                return (T) marshaller.unmarshal(reader);
            } catch (JAXBException e) {
                throw new SerializerException(String.format("Error occurs while deserializing %s", type), e);
            }
        }

        @Override
        public <T> T deserialize(final InputStream is, final Type type) throws SerializerException {
            if (type == null || !(type instanceof Class)) {
                return null;
            }
            Class clazz = (Class) type;
            Annotation annotation = clazz.getAnnotation(XmlRootElement.class);
            if (annotation == null) {
                return null;
            }
            try {
                JAXBContext context = getJaxbContext(clazz);
                Unmarshaller marshaller = context.createUnmarshaller();
                return (T) marshaller.unmarshal(is);
            } catch (JAXBException e) {
                throw new SerializerException(String.format("Error occurs while deserializing %s", type), e);
            }
        }

    }
}
