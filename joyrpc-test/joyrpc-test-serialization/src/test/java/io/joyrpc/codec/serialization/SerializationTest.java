package io.joyrpc.codec.serialization;

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


import io.joyrpc.cluster.discovery.backup.BackupDatum;
import io.joyrpc.cluster.discovery.backup.BackupShard;
import io.joyrpc.codec.serialization.exception.NotFoundException;
import io.joyrpc.codec.serialization.model.*;
import io.joyrpc.codec.serialization.model.ArrayObject.Foo;
import io.joyrpc.exception.MethodOverloadException;
import io.joyrpc.extension.ExtensionMeta;
import io.joyrpc.extension.Name;
import io.joyrpc.permission.SerializerTypeScanner;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.ResponsePayload;
import io.joyrpc.util.ClassUtils;
import io.joyrpc.util.GrpcMethod;
import io.joyrpc.util.GrpcType;
import io.joyrpc.util.SystemClock;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

import static io.joyrpc.Plugin.*;

public class SerializationTest {

    protected void serializeAndDeserialize(final Serialization serialization, final Object target,
                                           final UnsafeByteArrayOutputStream baos,
                                           final BiConsumer<Object, Object> consumer) {
        Serializer serializer = serialization.getSerializer();
        serializer.serialize(baos, target);
        UnsafeByteArrayInputStream bais = new UnsafeByteArrayInputStream(baos.toByteArray());
        Object data = serializer.deserialize(bais, target.getClass());
        if (consumer == null) {
            Assert.assertEquals(serialization.getContentType(), data, target);
        } else {
            consumer.accept(data, target);
        }
    }

    protected void serializeAndDeserialize(final Serialization serialization, final Object target, final UnsafeByteArrayOutputStream baos) {
        serializeAndDeserialize(serialization, target, baos, null);
    }

    protected void serializeAndDeserialize(final Object target) {
        serializeAndDeserialize(target, null);
    }

    protected void serializeAndDeserialize(final Object target, final BiConsumer<Object, Object> consumer) {
        List<String> types = SERIALIZATION.names();
        types.remove("xml");

        Serialization serialization;
        UnsafeByteArrayOutputStream baos = new UnsafeByteArrayOutputStream(1024);
        for (String type : types) {
            serialization = SERIALIZATION.get(type);
            baos.reset();
            serializeAndDeserialize(serialization, target, baos, consumer);
        }
    }

    protected void serializeAndDeserialize(final String type, final Object target) {
        UnsafeByteArrayOutputStream baos = new UnsafeByteArrayOutputStream(1024);
        Serialization serialization = SERIALIZATION.get(type);
        serializeAndDeserialize(serialization, target, baos);
    }

    protected void serializeAndDeserialize(final Object[] targets) {
        List<String> types = SERIALIZATION.names();
        types.remove("xml");

        Serialization serialization;
        UnsafeByteArrayOutputStream baos = new UnsafeByteArrayOutputStream(1024);
        for (String type : types) {
            serialization = SERIALIZATION.get(type);
            for (Object target : targets) {
                baos.reset();
                serializeAndDeserialize(serialization, target, baos);
            }
        }
    }

    @Test
    public void testBackup() {
        Map<String, List<BackupShard>> clusters = new HashMap<>();
        List<BackupShard> shards = new LinkedList<>();
        shards.add(new BackupShard("test", null, null, "joyrpc", "joyrpc://192.168.1.1:22000", 100));
        clusters.put("test", shards);
        Map<String, Map<String, String>> configs = new HashMap<>();
        Map<String, String> config = new HashMap<>();
        config.put("socketTimeout", "10000");
        configs.put("test", config);

        BackupDatum datum = new BackupDatum();
        datum.setClusters(clusters);
        datum.setConfigs(configs);

        serializeAndDeserialize(datum);
    }

    @Test
    public void testJava8Time() {
        ZoneId zoneId = ZoneId.of("UTC");
        Object[] times = new Object[]{Duration.ofMillis(1000), Instant.now(), LocalDateTime.now(),
                LocalDate.now(), LocalTime.now(), MonthDay.now(), OffsetTime.now(),
                Period.of(0, 1, 1), YearMonth.of(0, 1), Year.of(2000),
                ZonedDateTime.of(LocalDateTime.now(zoneId), zoneId), zoneId, ZoneOffset.ofTotalSeconds(0)
        };

        serializeAndDeserialize(times);
    }

    @Test
    public void testJsonTime() {
        ZoneId zoneId = ZoneId.of("UTC");
        Object[] times = new Object[]{new java.util.Date(), new Date(SystemClock.now()), Calendar.getInstance(),
                Duration.ofMillis(1000), Instant.now(), LocalDateTime.now(),
                LocalDate.now(), LocalTime.now(), MonthDay.now(), OffsetTime.now(),
                Period.of(0, 1, 1), YearMonth.of(0, 1), Year.of(2000),
                ZonedDateTime.of(LocalDateTime.now(zoneId), zoneId), zoneId, ZoneOffset.ofTotalSeconds(0)
        };
        Json fastJson = JSON.get("json@fastjson");
        Json jackson = JSON.get("json@jackson");
        for (Object time : times) {
            String value1 = fastJson.toJSONString(time);
            String value2 = jackson.toJSONString(time);
            System.out.println(time.getClass() + ":" + value1);
            Object time1 = jackson.parseObject(value1, time.getClass());
            Object time2 = fastJson.parseObject(value2, time.getClass());
            Assert.assertEquals(time, time1);
            Assert.assertEquals(time, time2);
        }
    }

    @Test
    public void testJsonThrowable() {

        Json fastJson = JSON.get("json@fastjson");
        Json jackson = JSON.get("json@jackson");
        try {
            Integer.parseInt("String");
        } catch (NumberFormatException e) {
            RuntimeException runtimeException = new RuntimeException(e);
            String serializedException = jackson.toJSONString(runtimeException);
            System.out.println(serializedException);
            Throwable throwable = fastJson.parseObject(serializedException, Throwable.class);
            throwable.printStackTrace();
        }
    }

    @Test
    public void testJsonResponsePayload() {
        Json fastJson = JSON.get("json@fastjson");
        Json jackson = JSON.get("json@jackson");
        ResponsePayload payload = new ResponsePayload();
        payload.setException(new NumberFormatException());
        String value = fastJson.toJSONString(payload);
        ResponsePayload target = jackson.parseObject(value, ResponsePayload.class);
        Assert.assertNotNull(target.getException());
        Assert.assertEquals(target.getException().getClass(), NumberFormatException.class);
        payload.setException(null);
        payload.setResponse(new Apple());
        value = fastJson.toJSONString(payload);
        target = jackson.parseObject(value, ResponsePayload.class);
        Assert.assertNotNull(target.getResponse());
        Assert.assertEquals(target.getResponse().getClass(), Apple.class);
    }

    @Test
    public void testInvocation() {
        Json fastJson = JSON.get("json@fastjson");
        Json jackson = JSON.get("json@jackson");
        Invocation invocation = new Invocation();
        invocation.setClassName(HelloGrpc.class.getName());
        invocation.setMethodName("hello");
        invocation.setAlias("test");
        invocation.setArgs(new Object[]{"111", PhoneType.HOME});
        invocation.addAttachment("test", Boolean.TRUE);
        String value = fastJson.toJSONString(invocation);
        Invocation target = jackson.parseObject(value, Invocation.class);
        Assert.assertNotNull(target.getArgs());
        Assert.assertArrayEquals(target.getArgs(), new Object[]{"111", PhoneType.HOME});
    }

    @Test
    public void testLocale() {
        serializeAndDeserialize(new Locale("zh", "CN", ""));
    }

    @Test
    public void testSqlDate() {
        serializeAndDeserialize(new SerializationSQlDate());
    }

    @Test
    public void testCodec() {
        serializeAndDeserialize(new Apple(1000, "appale", (byte) 1, true, new byte[]{1, 2}));
    }

    @Test
    public void testTransient() {
        TransientObj t1 = new TransientObj(1, 1);
        TransientObj t2 = new TransientObj(1, 0);
        serializeAndDeserialize(t1, (o1, o2) -> Assert.assertEquals(o1, t2));
    }

    @Test
    public void testArrayObject() {
        ArrayObject wrap = new ArrayObject();
        wrap.strArray = new String[]{"hello", null, "world"};
        wrap.fooArray = new Foo[]{null, new Foo("0", 0), null};
        wrap.objArray = new Object[]{new Foo("hello", 0), new Foo("world", 1), null};
        serializeAndDeserialize("protostuff", wrap);
        serializeAndDeserialize("protobuf", wrap);
    }

    @Test
    public void testLinkedHashMap() {
        MapObj obj = new MapObj();
        LinkedHashSet<String> set = new LinkedHashSet<>();
        set.add("1");
        set.add("2");
        obj.setSet(set);
        LinkedHashMap<String, String> map = new LinkedHashMap();
        map.putIfAbsent("test", "test");
        map.putIfAbsent("test2", "test2");
        obj.setMap(map);
        serializeAndDeserialize("hessian", obj);
    }

    @Test
    public void testGrpc() throws NoSuchMethodException, MethodOverloadException, IllegalAccessException {
        PhoneNumber phoneNumber = new PhoneNumber("123456789", PhoneType.MOBILE);

        Serialization serialization = SERIALIZATION.get("protobuf");
        Serializer serializer = serialization.getSerializer();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        serializer.serialize(baos, phoneNumber);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        GrpcMethod grpcMethod = ClassUtils.getPublicMethod(HelloGrpc.class, "hello", (c, m) -> GRPC_FACTORY.get().generate(c, m));
        Method method = grpcMethod.getMethod();
        GrpcType grpcType = grpcMethod.getType();
        Object obj = serializer.deserialize(bais, grpcType.getRequest().getClazz());
        List<Field> fields = ClassUtils.getFields(grpcType.getRequest().getClazz());
        fields.forEach(o -> o.setAccessible(true));
        Assert.assertEquals(phoneNumber.getNumber(), fields.get(0).get(obj));
        Assert.assertEquals(phoneNumber.getType(), fields.get(1).get(obj));
    }

    @Test
    public void testOverrideField() {
        MyEmployee person = new MyEmployee(0, "china", 20, 161, 65);
        serializeAndDeserialize("hessian", person);
    }

    @Test
    public void testScan() {
        SerializerTypeScanner scanner = new SerializerTypeScanner(HelloWold.class);
        Set<Class<?>> set = scanner.scan();
        Assert.assertTrue(set.contains(MyBook.class));
        Assert.assertTrue(set.contains(Map.class));
        Assert.assertTrue(set.contains(Employee.class));
        Assert.assertTrue(set.contains(List.class));
        Assert.assertTrue(set.contains(Person.class));
        Assert.assertTrue(set.contains(PhoneNumber.class));
        Assert.assertTrue(set.contains(int.class));
        Assert.assertTrue(set.contains(long.class));
        Assert.assertTrue(set.contains(double.class));
        Assert.assertTrue(set.contains(String.class));
        Assert.assertTrue(set.contains(PhoneType.class));
        Assert.assertTrue(set.contains(NotFoundException.class));
        Assert.assertTrue(set.contains(Integer.class));
        Assert.assertFalse(set.contains(CompletableFuture.class));
    }

    @Test
    public void testTps() throws ExecutionException, InterruptedException {

        Employee person = new Employee(0, "china", 20, 161, 65);

        long count = 1000000;
        int threads = 4;
        ExecutorService service = Executors.newFixedThreadPool(threads);
        Future<SerializationTime>[] futures = new Future[threads];

        Name<? extends Serialization, String> name;
        for (ExtensionMeta<Serialization, String> meta : SERIALIZATION.metas()) {
            name = meta.getExtension();
            if (name.getName().equals("xml")) {
                continue;
            }
            Serialization serialization = meta.getTarget();
            if (serialization instanceof Registration) {
                ((Registration) serialization).register(Employee.class);
            }
            final Serializer serializer = serialization.getSerializer();

            for (int k = 0; k < threads; k++) {
                futures[k] = service.submit(() -> {
                    SerializationTime time = new SerializationTime();
                    long startTime;
                    long endTime;
                    final UnsafeByteArrayOutputStream baos = new UnsafeByteArrayOutputStream(1024);
                    for (int i = 0; i < count; i++) {
                        baos.reset();
                        startTime = System.nanoTime();
                        serializer.serialize(baos, person);
                        endTime = System.nanoTime();
                        time.encodeTime += endTime - startTime;
                        time.size += baos.size();
                        UnsafeByteArrayInputStream bais = new UnsafeByteArrayInputStream(baos.toByteArray());
                        startTime = System.nanoTime();
                        serializer.deserialize(bais, Employee.class);
                        endTime = System.nanoTime();
                        time.decodeTime += endTime - startTime;
                    }
                    return time;
                });
            }
            SerializationTime total = new SerializationTime();
            for (Future<SerializationTime> future : futures) {
                SerializationTime time = future.get();
                total.encodeTime += time.encodeTime;
                total.decodeTime += time.decodeTime;
                total.size += time.size;
            }
            long totalCount = count * threads;
            System.out.println(String.format("%s@%s encode_tps %d decode_tps %d size %d in %d threads", name.getName(), meta.getProvider(),
                    totalCount * 1000000000L / total.encodeTime, totalCount * 1000000000L / total.decodeTime, total.size / totalCount, threads));
        }
    }

    protected static class SerializationSQlDate implements Serializable {

        protected java.sql.Date date = new java.sql.Date(System.currentTimeMillis());
        protected java.sql.Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        //Jackson用时间字符串序列化，会丢失日期，所以要统一时间单位，去掉日期
        protected java.sql.Time time = new java.sql.Time(System.currentTimeMillis() % (24 * 3600 * 1000) / 1000 * 1000);

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public Timestamp getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Timestamp timestamp) {
            this.timestamp = timestamp;
        }

        public Time getTime() {
            return time;
        }

        public void setTime(Time time) {
            this.time = time;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            SerializationSQlDate that = (SerializationSQlDate) o;

            if (date != null ? !date.equals(that.date) : that.date != null) {
                return false;
            }
            if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null) {
                return false;
            }
            boolean result = time != null ? time.equals(that.time) : that.time == null;
            return result;

        }

        @Override
        public int hashCode() {
            int result = date != null ? date.hashCode() : 0;
            result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
            result = 31 * result + (time != null ? time.hashCode() : 0);
            return result;
        }
    }

    protected static class SerializationTime {
        long encodeTime;
        long decodeTime;
        long size;
    }

    protected static class MapObj {

        private LinkedHashMap<String, String> map;

        private LinkedHashSet<String> set;

        public LinkedHashMap<String, String> getMap() {
            return map;
        }

        public void setMap(LinkedHashMap<String, String> map) {
            this.map = map;
        }

        public LinkedHashSet<String> getSet() {
            return set;
        }

        public void setSet(LinkedHashSet<String> set) {
            this.set = set;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            MapObj mapObj = (MapObj) o;

            if (map != null ? !map.equals(mapObj.map) : mapObj.map != null) {
                return false;
            }
            return set != null ? set.equals(mapObj.set) : mapObj.set == null;
        }

        @Override
        public int hashCode() {
            int result = map != null ? map.hashCode() : 0;
            result = 31 * result + (set != null ? set.hashCode() : 0);
            return result;
        }
    }

}
