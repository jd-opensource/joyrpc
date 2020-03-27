package io.joyrpc.invoker;

import io.joyrpc.transport.session.DefaultSession;

/**
 * 服务端会话
 */
public class ProviderSession extends DefaultSession {
    /**
     * 服务端输出
     */
    protected Exporter exporter;

    public ProviderSession() {
    }

    public ProviderSession(int sessionId) {
        super(sessionId);
    }

    public ProviderSession(int sessionId, long timeout) {
        super(sessionId, timeout);
    }

    public Exporter getExporter() {
        return exporter;
    }

    public void setExporter(Exporter exporter) {
        this.exporter = exporter;
    }
}
