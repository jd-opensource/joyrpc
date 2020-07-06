package io.joyrpc.example.service.vo;

import java.io.Serializable;
import java.time.*;
import java.util.Objects;
import java.util.TimeZone;

public class Java8TimeObj implements Serializable {

    private LocalTime localTime;

    private LocalDate localDate;

    private LocalDateTime localDateTime;

    private Instant instant;

    private Duration duration;

    private Period period;

    private Year year;

    private YearMonth yearMonth;

    private MonthDay monthDay;

    private OffsetTime offsetTime;

    private ZoneOffset zoneOffset;

    private OffsetDateTime offsetDateTime;

    private ZonedDateTime zonedDateTime;

    private ZoneId zoneId;

    private TimeZone timeZone;

    public LocalTime getLocalTime() {
        return localTime;
    }

    public void setLocalTime(LocalTime localTime) {
        this.localTime = localTime;
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public void setLocalDate(LocalDate localDate) {
        this.localDate = localDate;
    }

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public void setLocalDateTime(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }

    public Instant getInstant() {
        return instant;
    }

    public void setInstant(Instant instant) {
        this.instant = instant;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public Period getPeriod() {
        return period;
    }

    public void setPeriod(Period period) {
        this.period = period;
    }

    public Year getYear() {
        return year;
    }

    public void setYear(Year year) {
        this.year = year;
    }

    public YearMonth getYearMonth() {
        return yearMonth;
    }

    public void setYearMonth(YearMonth yearMonth) {
        this.yearMonth = yearMonth;
    }

    public MonthDay getMonthDay() {
        return monthDay;
    }

    public void setMonthDay(MonthDay monthDay) {
        this.monthDay = monthDay;
    }

    public OffsetTime getOffsetTime() {
        return offsetTime;
    }

    public void setOffsetTime(OffsetTime offsetTime) {
        this.offsetTime = offsetTime;
    }

    public ZoneOffset getZoneOffset() {
        return zoneOffset;
    }

    public void setZoneOffset(ZoneOffset zoneOffset) {
        this.zoneOffset = zoneOffset;
    }

    public OffsetDateTime getOffsetDateTime() {
        return offsetDateTime;
    }

    public void setOffsetDateTime(OffsetDateTime offsetDateTime) {
        this.offsetDateTime = offsetDateTime;
    }

    public ZonedDateTime getZonedDateTime() {
        return zonedDateTime;
    }

    public void setZonedDateTime(ZonedDateTime zonedDateTime) {
        this.zonedDateTime = zonedDateTime;
    }

    public ZoneId getZoneId() {
        return zoneId;
    }

    public void setZoneId(ZoneId zoneId) {
        this.zoneId = zoneId;
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Java8TimeObj that = (Java8TimeObj) o;
        return Objects.equals(localTime, that.localTime) &&
                Objects.equals(localDate, that.localDate) &&
                Objects.equals(localDateTime, that.localDateTime) &&
                Objects.equals(instant, that.instant) &&
                Objects.equals(duration, that.duration) &&
                Objects.equals(period, that.period) &&
                Objects.equals(year, that.year) &&
                Objects.equals(yearMonth, that.yearMonth) &&
                Objects.equals(monthDay, that.monthDay) &&
                Objects.equals(offsetTime, that.offsetTime) &&
                Objects.equals(zoneOffset, that.zoneOffset) &&
                Objects.equals(offsetDateTime, that.offsetDateTime) &&
                Objects.equals(zonedDateTime, that.zonedDateTime) &&
                Objects.equals(zoneId, that.zoneId) &&
                Objects.equals(timeZone, that.timeZone);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localTime, localDate, localDateTime, instant, duration, period, year, yearMonth, monthDay, offsetTime, zoneOffset, offsetDateTime, zonedDateTime, zoneId, timeZone);
    }

    public static Java8TimeObj newJava8TimeObj() {
        Java8TimeObj timeObj = new Java8TimeObj();
        timeObj.setLocalTime(LocalTime.now());
        timeObj.setLocalDate(LocalDate.now());
        timeObj.setLocalDateTime(LocalDateTime.now());
        timeObj.setInstant(Instant.now());
        timeObj.setDuration(Duration.ofMillis(System.currentTimeMillis()));
        timeObj.setPeriod(Period.of(0, 1, 1));
        timeObj.setYear(Year.now());
        timeObj.setYearMonth(YearMonth.now());
        timeObj.setMonthDay(MonthDay.now());
        timeObj.setOffsetTime(OffsetTime.now());
        timeObj.setZoneOffset(ZoneOffset.ofHours(1));
        timeObj.setOffsetDateTime(OffsetDateTime.now());
        timeObj.setZonedDateTime(ZonedDateTime.now());
        timeObj.setZoneId(ZoneId.of("UTC"));
        timeObj.setTimeZone(TimeZone.getDefault());
        return timeObj;
    }
}
