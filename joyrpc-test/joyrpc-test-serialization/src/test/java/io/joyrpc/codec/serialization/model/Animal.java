package io.joyrpc.codec.serialization.model;

public class Animal<T> {

    private T[] owners;

    public T[] getOwners() {
        return owners;
    }

    public void setOwners(T[] owners) {
        this.owners = owners;
    }
}
