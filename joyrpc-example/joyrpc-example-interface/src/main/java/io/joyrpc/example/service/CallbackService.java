package io.joyrpc.example.service;

import io.joyrpc.Callback;
import io.joyrpc.example.service.vo.*;

public interface CallbackService {

    boolean echoGenericCallback(Callback callback);

    boolean echoRequestCallback(Callback<EchoRequest<EchoData>, EchoResponse<EchoData>> callback);

    boolean echoGenericRequestCallback(Callback<EchoRequest, EchoResponse> callback);

    boolean echoDataRequestCallback(Callback<EchoDataRequest, EchoDataResponse> callback);

    boolean echoGenericListener(Listener listener);

    boolean echoRequestListener(Listener<EchoRequest<EchoData>, EchoResponse<EchoData>> listener);

    interface Listener<Q, S> {
        S notify(Q result);
    }
}
