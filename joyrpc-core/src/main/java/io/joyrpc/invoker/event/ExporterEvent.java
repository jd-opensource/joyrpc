package io.joyrpc.invoker.event;

import io.joyrpc.event.Event;
import io.joyrpc.invoker.Exporter;

/**
 * 事件
 */
public class ExporterEvent implements Event {
    /**
     * 事件类型
     */
    protected EventType type;
    /**
     * 名称
     */
    protected String name;
    /**
     * 调用器
     */
    protected Exporter exporter;

    public ExporterEvent(EventType type, String name, Exporter exporter) {
        this.type = type;
        this.name = name;
        this.exporter = exporter;
    }

    public EventType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Exporter getExporter() {
        return exporter;
    }

    /**
     * 事件类型
     */
    public enum EventType {
        INITIAL,
        OPEN,
        CLOSE
    }

}
