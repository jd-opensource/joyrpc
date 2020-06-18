package io.joyrpc.example.service;

import io.joyrpc.Callback;
import io.joyrpc.example.service.vo.*;

public interface CallbackService {

    String echoCallback(Callback<String, String> callback);

    String echoGenericCallback(Callback callback);

    String echoRequestCallback(Callback<EchoRequest<EchoData>, EchoResponse<EchoData>> callback);

    String echoGenericRequestCallback(Callback<EchoRequest, EchoResponse> callback);

    String echoDataRequestCallback(Callback<EchoDataRequest, EchoDataResponse> callback);
}
