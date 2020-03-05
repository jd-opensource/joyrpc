package io.joyrpc.invoker;

import io.joyrpc.Invoker;
import io.joyrpc.event.Event;

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
    protected Invoker invoker;

    public ExporterEvent(EventType type, String name, Invoker invoker) {
        this.type = type;
        this.name = name;
        this.invoker = invoker;
    }

    public EventType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Invoker getInvoker() {
        return invoker;
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
