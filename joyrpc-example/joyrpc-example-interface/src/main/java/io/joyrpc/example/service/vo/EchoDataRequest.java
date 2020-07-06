package io.joyrpc.example.service.vo;


public class EchoDataRequest extends EchoRequest<EchoData>{

    public EchoDataRequest() {
    }

    public EchoDataRequest(EchoHeader header) {
        super(header);
    }

    public EchoDataRequest(EchoHeader header, EchoData body) {
        super(header, body);
    }
}
