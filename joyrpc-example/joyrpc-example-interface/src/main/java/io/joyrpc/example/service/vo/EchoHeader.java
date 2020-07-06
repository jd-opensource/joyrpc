package io.joyrpc.example.service.vo;

import java.util.HashMap;
import java.util.Map;

public class EchoHeader {

    private Map<String, String> attrs = new HashMap<>();

    public EchoHeader() {
    }

    public EchoHeader(Map<String, String> attrs) {
        this.attrs = attrs;
    }

    public Map<String, String> getAttrs() {
        return attrs;
    }

    public void setAttrs(Map<String, String> attrs) {
        this.attrs = attrs;
    }

    @Override
    public String toString() {
        return "EchoHeader{" +
                "attrs=" + attrs +
                '}';
    }
}
