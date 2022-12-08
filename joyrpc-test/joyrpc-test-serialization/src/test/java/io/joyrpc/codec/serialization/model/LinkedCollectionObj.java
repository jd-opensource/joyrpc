package io.joyrpc.codec.serialization.model;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

public class LinkedCollectionObj {

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

        LinkedCollectionObj mapObj = (LinkedCollectionObj) o;

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
