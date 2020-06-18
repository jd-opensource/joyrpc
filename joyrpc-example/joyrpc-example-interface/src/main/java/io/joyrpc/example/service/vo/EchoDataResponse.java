package io.joyrpc.example.service.vo;

public class EchoDataResponse extends EchoResponse<EchoData> {

    public EchoDataResponse() {
    }

    public EchoDataResponse(EchoHeader header) {
        super(header);
    }

    public EchoDataResponse(EchoHeader header, EchoData body) {
        super(header, body);
    }
}
