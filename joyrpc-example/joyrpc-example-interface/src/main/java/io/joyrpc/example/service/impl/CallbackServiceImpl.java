package io.joyrpc.example.service.impl;

import io.joyrpc.Callback;
import io.joyrpc.example.service.CallbackService;
import io.joyrpc.example.service.vo.*;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class CallbackServiceImpl implements CallbackService {

    protected Set<Callback> genericCallbacks = new HashSet<>();

    protected Set<Callback<EchoRequest<EchoData>, EchoResponse<EchoData>>> requestCallbacks = new HashSet<>();

    protected Set<Callback<EchoRequest, EchoResponse>> genericRequestCallbacks = new HashSet<>();

    protected Set<Callback<EchoDataRequest, EchoDataResponse>> dataRequestCallbacks = new HashSet<>();

    protected Set<Listener> genericListeners = new HashSet<>();

    protected Set<Listener<EchoRequest<EchoData>, EchoResponse<EchoData>>> requestListeners = new HashSet<>();

    public CallbackServiceImpl() {
        AtomicInteger counter = new AtomicInteger();
        ScheduledExecutorService scheduled = Executors.newSingleThreadScheduledExecutor();
        scheduled.scheduleWithFixedDelay(() -> {
            genericCallbacks.forEach(c -> doCallback("GenericCallback",
                    () -> c.notify("req " + counter.incrementAndGet())
            ));
            requestCallbacks.forEach(c -> doCallback("RequestCallback",
                    () -> c.notify(new EchoRequest<>(new EchoHeader(), new EchoData(counter.get(), "the " + counter.get() + "req")))
            ));
            genericRequestCallbacks.forEach(c -> doCallback("GenericRequestCallback",
                    () -> c.notify(new EchoRequest<>(new EchoHeader(), new EchoData(counter.get(), "the " + counter.get() + "req")))
            ));
            dataRequestCallbacks.forEach(c -> doCallback("DataRequestCallback",
                    () -> c.notify(new EchoDataRequest(new EchoHeader(), new EchoData(counter.get(), "the " + counter.get() + "req")))
            ));
            genericListeners.forEach(c -> doCallback("GenericListener",
                    () -> c.notify("req " + counter.incrementAndGet())
            ));
            requestListeners.forEach(c -> doCallback("RequestListener",
                    () -> c.notify(new EchoRequest<>(new EchoHeader(), new EchoData(counter.get(), "the " + counter.get() + "req")))
            ));
        }, 5000, 5000, TimeUnit.MILLISECONDS);
    }

    public void doCallback(String name, Supplier supplier) {
        try {
            Object resq = supplier.get();
            System.out.println(name + " response " + resq);
        } catch (Exception e) {
            System.out.println(name + " callback error " + e.getMessage());
        }
    }

    @Override
    public boolean echoGenericCallback(Callback callback) {
        return genericCallbacks.add(callback);
    }

    @Override
    public boolean echoRequestCallback(Callback<EchoRequest<EchoData>, EchoResponse<EchoData>> callback) {
        return requestCallbacks.add(callback);
    }

    @Override
    public boolean echoGenericRequestCallback(Callback<EchoRequest, EchoResponse> callback) {
        return genericRequestCallbacks.add(callback);
    }

    @Override
    public boolean echoDataRequestCallback(Callback<EchoDataRequest, EchoDataResponse> callback) {
        return dataRequestCallbacks.add(callback);
    }

    @Override
    public boolean echoGenericListener(Listener listener) {
        return genericListeners.add(listener);
    }

    @Override
    public boolean echoRequestListener(Listener<EchoRequest<EchoData>, EchoResponse<EchoData>> listener) {
        return requestListeners.add(listener);
    }
}
