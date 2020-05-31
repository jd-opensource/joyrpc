package io.joyrpc.spring.context;

@FunctionalInterface
public interface PropertyKeysSupplier {

    String[] propertyKeys();
}
