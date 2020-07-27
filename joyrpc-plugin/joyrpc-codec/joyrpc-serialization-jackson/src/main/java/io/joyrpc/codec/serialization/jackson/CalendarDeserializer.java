package io.joyrpc.codec.serialization.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * 日历反序列化
 */
public class CalendarDeserializer extends DateDeserializers.CalendarDeserializer {

    public static final CalendarDeserializer INSTANCE = new CalendarDeserializer();

    public CalendarDeserializer() {
    }

    public CalendarDeserializer(Class<? extends Calendar> cc) {
        super(cc);
    }

    @Override
    public Calendar deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        Date d = _parseDate(p, ctxt);
        if (d == null) {
            return null;
        }
        if (_defaultCtor == null) {
            return ctxt.constructCalendar(d);
        }
        try {
            Calendar c = _defaultCtor.newInstance();
            TimeZone tz = ctxt.getTimeZone();
            if (tz != null) {
                c.setTimeZone(tz);
            }
            c.setTimeInMillis(d.getTime());
            return c;
        } catch (Exception e) {
            return (Calendar) ctxt.handleInstantiationProblem(handledType(), d, e);
        }
    }
}
