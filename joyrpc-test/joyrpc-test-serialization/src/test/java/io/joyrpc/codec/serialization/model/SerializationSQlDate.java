package io.joyrpc.codec.serialization.model;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

public class SerializationSQlDate implements Serializable {

    protected java.sql.Date date = new java.sql.Date(System.currentTimeMillis());
    protected java.sql.Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    //Jackson用时间字符串序列化，会丢失日期，所以要统一时间单位，去掉日期
    protected java.sql.Time time = new java.sql.Time(System.currentTimeMillis() % (24 * 3600 * 1000) / 1000 * 1000);

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public Time getTime() {
        return time;
    }

    public void setTime(Time time) {
        this.time = time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SerializationSQlDate that = (SerializationSQlDate) o;

        if (date != null ? !date.equals(that.date) : that.date != null) {
            return false;
        }
        if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null) {
            return false;
        }
        boolean result = time != null ? time.equals(that.time) : that.time == null;
        return result;

    }

    @Override
    public int hashCode() {
        int result = date != null ? date.hashCode() : 0;
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = 31 * result + (time != null ? time.hashCode() : 0);
        return result;
    }
}
