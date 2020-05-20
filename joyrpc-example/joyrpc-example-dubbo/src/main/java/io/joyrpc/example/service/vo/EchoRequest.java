package io.joyrpc.example.service.vo;

public class EchoRequest<T> {

    private EchoHeader header;

    private T body;

    public EchoRequest() {
    }

    public EchoRequest(EchoHeader header) {
        this.header = header;
    }

    public EchoRequest(EchoHeader header, T body) {
        this.header = header;
        this.body = body;
    }

    public EchoHeader getHeader() {
        return header;
    }

    public void setHeader(EchoHeader header) {
        this.header = header;
    }

    public T getBody() {
        return body;
    }

    public void setBody(T body) {
        this.body = body;
    }
}
