package io.joyrpc.example.service.vo;

public class EchoResponse<T> {

    private EchoHeader header;

    private T body;

    public EchoResponse() {
    }

    public EchoResponse(EchoHeader header) {
        this.header = header;
    }

    public EchoResponse(EchoHeader header, T body) {
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
