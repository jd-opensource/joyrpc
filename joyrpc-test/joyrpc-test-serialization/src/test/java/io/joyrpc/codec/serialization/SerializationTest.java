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
import io.joyrpc.codec.UnsafeByteArrayInputStream;
import io.joyrpc.codec.UnsafeByteArrayOutputStream;
import io.joyrpc.codec.serialization.exception.NotFoundException;
import io.joyrpc.codec.serialization.model.*;
import io.joyrpc.codec.serialization.model.ArrayObject.Foo;
import io.joyrpc.exception.MethodOverloadException;
import io.joyrpc.extension.ExtensionMeta;
import io.joyrpc.extension.Name;
import io.joyrpc.permission.SerializerTypeScanner;
import io.joyrpc.permission.SerializerWhiteList;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.ResponsePayload;
import io.joyrpc.util.ClassUtils;
import io.joyrpc.util.IDLMethod;
import io.joyrpc.util.IDLMethodDesc;
import io.joyrpc.util.SystemClock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Date;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import static io.joyrpc.Plugin.*;

public class SerializationTest {

    @BeforeAll
    public static void beforeClass() {
        SerializerWhiteList.getGlobalWhitelist().setEnabled(false);
    }

    protected <T> void serializeAndDeserialize(final Serialization serialization,
                                               final T target,
                                               final UnsafeByteArrayOutputStream baos,
                                               final SerialConsumer<T> consumer) {
        Serializer serializer = serialization.getSerializer();
        serializer.serialize(baos, target);
        UnsafeByteArrayInputStream bais = new UnsafeByteArrayInputStream(baos.toByteArray());
        T data = serializer.deserialize(bais, target.getClass());
        if (consumer == null) {
            Assertions.assertEquals(data, target, serialization.getContentType());
        } else {
            consumer.accept(data, target, serialization);
        }
    }

    protected <T> void serializeAndDeserialize(final Serialization serialization,
                                               final T target,
                                               final UnsafeByteArrayOutputStream baos) {
        serializeAndDeserialize(serialization, target, baos, null);
    }

    protected <T> void serializeAndDeserialize(final T target) {
        serializeAndDeserialize(target, null);
    }

    protected <T> void serializeAndDeserialize(final T target, final SerialConsumer<T> consumer) {
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

    protected <T> void serializeAndDeserialize(final String type, final T target) {
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

        Json fastJson = JSON.get("json@fastjson2");
        Json jackson = JSON.get("json@jackson");
        Duration time = Duration.ofMillis(10000L);
        String value1 = fastJson.toJSONString(time);
        String value2 = jackson.toJSONString(time);
        System.out.println(time.getClass() + ":" + value1);
        System.out.println(time.getClass() + ":" + value2);
        Object time1 = jackson.parseObject(value1, time.getClass());
        Object time2 = fastJson.parseObject(value2, time.getClass());
        Assertions.assertEquals(time, time1);
        Assertions.assertEquals(time, time2);

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
        Json fastJson = JSON.get("json@fastjson2");
        Json jackson = JSON.get("json@jackson");
        for (Object time : times) {
            String value1 = fastJson.toJSONString(time);
            String value2 = jackson.toJSONString(time);
            System.out.println(time.getClass() + ":" + value1);
            Object time1 = jackson.parseObject(value1, time.getClass());
            Object time2 = fastJson.parseObject(value2, time.getClass());
            Assertions.assertEquals(time, time1);
            Assertions.assertEquals(time, time2);
        }
    }

    @Test
    public void testJsonThrowable() {

        //Json fastJson1 = JSON.get("json@fastjson2");
        Json fastJson2 = JSON.get("json@fastjson2");
        Json jackson = JSON.get("json@jackson");
        try {
            Integer.parseInt("String");
        } catch (NumberFormatException e) {
            String serializedException = jackson.toJSONString(e);
            System.out.println(serializedException);
            //Throwable throwable1 = fastJson1.parseObject(serializedException, Throwable.class);
            //serializedException = fastJson1.toJSONString(throwable1);
            Throwable throwable2 = fastJson2.parseObject(serializedException, Throwable.class);
            System.out.printf(fastJson2.toJSONString(e));
            //Assertions.assertEquals(runtimeException.getMessage(), throwable1.getMessage());
            //Assertions.assertEquals(runtimeException.getClass(), throwable1.getClass());
            //Assertions.assertEquals(runtimeException.getCause().getClass(), throwable1.getCause().getClass());
            Assertions.assertEquals(e.getMessage(), throwable2.getMessage());
            Assertions.assertEquals(e.getClass(), throwable2.getClass());
        }
    }

    @Test
    public void testJsonResponsePayload() {
        Json fastJson = JSON.get("json@fastjson2");
        Json jackson = JSON.get("json@jackson");
        ResponsePayload payload = new ResponsePayload();
        payload.setException(new NumberFormatException());
        String value = fastJson.toJSONString(payload);
        String value1 = jackson.toJSONString(payload);
        System.out.println(value);
        System.out.println(value1);
        ResponsePayload target = jackson.parseObject(value, ResponsePayload.class);
        ResponsePayload target1 = fastJson.parseObject(value1, ResponsePayload.class);
        Assertions.assertNotNull(target.getException());
        Assertions.assertEquals(target.getException().getClass(), NumberFormatException.class);
        payload.setException(null);
        payload.setResponse(new Apple());
        value = fastJson.toJSONString(payload);
        target = jackson.parseObject(value, ResponsePayload.class);
        Assertions.assertNotNull(target.getResponse());
        Assertions.assertEquals(target.getResponse().getClass(), Apple.class);
    }

    @Test
    public void testInvocation() {
        Invocation invocation = new Invocation();
        invocation.setClassName(HelloGrpc.class.getName());
        invocation.setMethodName("hello");
        invocation.setAlias("test");
        invocation.setArgsType(new String[]{String.class.getName(), PhoneType.class.getName()});
        invocation.setCallback(true);
        invocation.setArgs(new Object[]{"111", PhoneType.HOME});
        invocation.addAttachment("test", Boolean.TRUE);
        List<Json> jsons = allJson();
        for (Json json : jsons) {
            String value = json.toJSONString(invocation);
            System.out.println(value);
            for (Json json1 : jsons) {
                Invocation target = json1.parseObject(value, Invocation.class);
                Assertions.assertNotNull(target.getArgs());
                Assertions.assertArrayEquals(target.getArgs(), new Object[]{"111", PhoneType.HOME});
            }
        }
    }

    @Test
    public void testLocale() {
        serializeAndDeserialize(new Locale("zh", "CN", ""), (src, target, serial) -> {
            Assertions.assertEquals(src.getLanguage(), target.getLanguage(), serial.getContentType());
            Assertions.assertEquals(src.getCountry(), target.getCountry(), serial.getContentType());
            Assertions.assertEquals(src.getVariant(), target.getVariant(), serial.getContentType());
        });
    }

    @Test
    public void testArray() {
        List<Employee> employees = new ArrayList<>();
        employees.add(new Employee(0, "china", 20, 161, 65));
        employees.add(new Employee(0, "hongkong", 20, 161, 65));
        List<Json> jsons = allJson();
        String value = jsons.get(0).toJSONString(employees);
        System.out.println(value);
        for (Json json : jsons) {
            json.parseArray(value, o -> {
                Object e = o.apply(Employee.class);
                System.out.println(e.toString());
                return true;
            });
        }
    }

    protected List<Json> allJson() {
        List<Json> jsons = new ArrayList<>(5);
        JSON.extensions().forEach(o -> jsons.add(o));
        return jsons;
    }

    @Test
    public void testCustomTypes() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("11111", new Employee(0, "china", 20, 161, 65));
        map.put("11112", new Employee(0, "hongkong", 20, 161, 65));
        List<Json> jsons = allJson();
        String value = jsons.get(0).toJSONString(map);
        System.out.println(value);
        for (Json json : jsons) {
            json.parseObject(value, (name, o) -> {
                Object e = o.apply(Employee.class);
                System.out.println(e.toString());
                return true;
            });
        }
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
        serializeAndDeserialize(t1, (src, target, serial) -> Assertions.assertEquals(src, t2, serial.getContentType()));
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
        LinkedCollectionObj obj = new LinkedCollectionObj();
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

        IDLMethod idlMethod = ClassUtils.getPublicMethod(HelloGrpc.class, "hello", (c, m) -> GRPC_FACTORY.get().build(c, m));
        Method method = idlMethod.getMethod();
        IDLMethodDesc methodDesc = idlMethod.getType();
        Object obj = serializer.deserialize(bais, methodDesc.getRequest().getClazz());
        List<Field> fields = ClassUtils.getFields(methodDesc.getRequest().getClazz());
        fields.forEach(o -> o.setAccessible(true));
        Assertions.assertEquals(phoneNumber.getNumber(), fields.get(0).get(obj));
        Assertions.assertEquals(phoneNumber.getType(), fields.get(1).get(obj));
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
        Assertions.assertTrue(set.contains(MyBook.class));
        Assertions.assertTrue(set.contains(Map.class));
        Assertions.assertTrue(set.contains(Employee.class));
        Assertions.assertTrue(set.contains(List.class));
        Assertions.assertTrue(set.contains(Person.class));
        Assertions.assertTrue(set.contains(PhoneNumber.class));
        Assertions.assertTrue(set.contains(int.class));
        Assertions.assertTrue(set.contains(long.class));
        Assertions.assertTrue(set.contains(double.class));
        Assertions.assertTrue(set.contains(String.class));
        Assertions.assertTrue(set.contains(PhoneType.class));
        Assertions.assertTrue(set.contains(NotFoundException.class));
        Assertions.assertTrue(set.contains(Integer.class));
        Assertions.assertFalse(set.contains(CompletableFuture.class));
        Assertions.assertTrue(set.contains(Animal.class));
    }

    @Test
    public void testTps() throws ExecutionException, InterruptedException {

        Employee person = new Employee(0, "china", 20, 161, 65);

        long count = 100000;
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

    @FunctionalInterface
    protected interface SerialConsumer<T> {
        void accept(T src, T target, Serialization serialization);

    }

}
