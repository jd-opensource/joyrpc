package io.joyrpc.extension;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * 包装方法参数
 */
public class WrapperParametric implements Parametric {
    /**
     * 源参数对象
     */
    protected Parametric source;
    /**
     * 名称
     */
    protected String name;
    /**
     * 键转换函数
     */
    protected BiFunction<String, String, String> keyFunc;
    /**
     * 键匹配函数
     */
    protected Predicate<String> predicate;

    public WrapperParametric(final Parametric source,
                             final String name,
                             final BiFunction<String, String, String> keyFunc,
                             final Predicate<String> predicate) {
        this.source = source;
        this.name = name;
        this.keyFunc = keyFunc;
        this.predicate = predicate;
    }

    public String getName() {
        return name;
    }

    @Override
    public <T> T getObject(final String key) {
        return source.getObject(getKey(key));
    }

    @Override
    public String getString(final String key) {
        return source.getString(getKey(key));
    }

    @Override
    public String getString(final String key, final String def) {
        return source.getString(getKey(key), def);
    }

    @Override
    public String getString(final String key, final String candidate, final String def) {
        return source.getString(getKey(key), getKey(candidate), def);
    }

    @Override
    public String getString(final URLOption<String> option) {
        return source.getString(getURLOption(option));
    }

    @Override
    public String getString(final URLBiOption<String> option) {
        return source.getString(getURLBiOption(option));
    }

    @Override
    public Date getDate(final String key, final Date def) {
        return source.getDate(getKey(key), def);
    }

    @Override
    public Date getDate(final String key, final SimpleDateFormat format) {
        return source.getDate(getKey(key), format);
    }

    @Override
    public Date getDate(final String key, final SimpleDateFormat format, Date def) {
        return source.getDate(getKey(key), format, def);
    }

    @Override
    public Date getDate(final String key, final DateParser parser, final Date def) {
        return source.getDate(getKey(key), parser, def);
    }

    @Override
    public Float getFloat(final String key) {
        return source.getFloat(getKey(key));
    }

    @Override
    public Float getFloat(final String key, final Float def) {
        return source.getFloat(getKey(key), def);
    }

    @Override
    public Float getFloat(final String key, final String candidate, final Float def) {
        return source.getFloat(getKey(key), getKey(candidate), def);
    }

    @Override
    public Float getFloat(final URLOption<Float> option) {
        return source.getFloat(getURLOption(option));
    }

    @Override
    public Float getFloat(final URLBiOption<Float> option) {
        return source.getFloat(getURLBiOption(option));
    }

    @Override
    public Double getDouble(final String key) {
        return source.getDouble(getKey(key));
    }

    @Override
    public Double getDouble(final String key, final String candidate, final Double def) {
        return source.getDouble(getKey(key), getKey(candidate), def);
    }

    @Override
    public Double getDouble(final String key, final Double def) {
        return source.getDouble(getKey(key), def);
    }

    @Override
    public Double getDouble(final URLOption<Double> option) {
        return source.getDouble(getURLOption(option));
    }

    @Override
    public Double getDouble(final URLBiOption<Double> option) {
        return source.getDouble(getURLBiOption(option));
    }

    @Override
    public Long getLong(final String key) {
        return source.getLong(getKey(key));
    }

    @Override
    public Long getLong(final String key, final String candidate, final Long def) {
        return source.getLong(getKey(key), getKey(candidate), def);
    }

    @Override
    public Long getLong(final String key, final Long def) {
        return source.getLong(getKey(key), def);
    }

    @Override
    public Long getLong(final URLOption<Long> option) {
        return source.getLong(getURLOption(option));
    }

    @Override
    public Long getLong(final URLBiOption<Long> option) {
        return source.getLong(getURLBiOption(option));
    }

    @Override
    public Integer getInteger(final String key) {
        return source.getInteger(getKey(key));
    }

    @Override
    public Integer getInteger(final String key, final Integer def) {
        return source.getInteger(getKey(key), def);
    }

    @Override
    public Integer getInteger(final String key, final String candidate, final Integer def) {
        return source.getInteger(getKey(key), getKey(candidate), def);
    }

    @Override
    public Integer getInteger(final URLOption<Integer> option) {
        return source.getInteger(getURLOption(option));
    }

    @Override
    public Integer getInteger(final URLBiOption<Integer> option) {
        return source.getInteger(getURLBiOption(option));
    }

    @Override
    public Short getShort(String key) {
        return source.getShort(getKey(key));
    }

    @Override
    public Short getShort(String key, Short def) {
        return source.getShort(getKey(key), def);
    }

    @Override
    public Short getShort(String key, String candidate, Short def) {
        return source.getShort(getKey(key), getKey(candidate), def);
    }

    @Override
    public Short getShort(final URLOption<Short> option) {
        return source.getShort(getURLOption(option));
    }

    @Override
    public Short getShort(final URLBiOption<Short> option) {
        return source.getShort(getURLBiOption(option));
    }

    @Override
    public Byte getByte(final String key) {
        return source.getByte(getKey(key));
    }

    @Override
    public Byte getByte(final String key, final Byte def) {
        return source.getByte(getKey(key), def);
    }

    @Override
    public Byte getByte(final String key, final String candidate, final Byte def) {
        return source.getByte(getKey(key), getKey(candidate), def);
    }

    @Override
    public Byte getByte(final URLOption<Byte> option) {
        return source.getByte(getURLOption(option));
    }

    @Override
    public Byte getByte(final URLBiOption<Byte> option) {
        return source.getByte(getURLBiOption(option));
    }

    @Override
    public Boolean getBoolean(final String key) {
        return source.getBoolean(getKey(key));
    }

    @Override
    public Boolean getBoolean(final String key, final Boolean def) {
        return source.getBoolean(getKey(key), def);
    }

    @Override
    public Boolean getBoolean(final String key, final String candidate, final Boolean def) {
        return source.getBoolean(getKey(key), getKey(candidate), def);
    }

    @Override
    public Boolean getBoolean(final URLOption<Boolean> option) {
        return source.getBoolean(getURLOption(option));
    }

    @Override
    public Boolean getBoolean(final URLBiOption<Boolean> option) {
        return source.getBoolean(getURLBiOption(option));
    }

    @Override
    public Long getNatural(final String key, final Long def) {
        return source.getNatural(getKey(key), def);
    }

    @Override
    public Long getNatural(final String key, final String candidate, final Long def) {
        return source.getNatural(getKey(key), getKey(candidate), def);
    }

    @Override
    public Long getNaturalLong(final URLOption<Long> option) {
        return source.getNaturalLong(getURLOption(option));
    }

    @Override
    public Long getNaturalLong(final URLBiOption<Long> option) {
        return source.getNaturalLong(getURLBiOption(option));
    }

    @Override
    public Integer getNatural(final String key, final Integer def) {
        return source.getNatural(getKey(key), def);
    }

    @Override
    public Integer getNatural(final String key, final String candidate, final Integer def) {
        return source.getNatural(getKey(key), getKey(candidate), def);
    }

    @Override
    public Integer getNaturalInt(final URLOption<Integer> option) {
        return source.getNaturalInt(getURLOption(option));
    }

    @Override
    public Integer getNaturalInt(final URLBiOption<Integer> option) {
        return source.getNaturalInt(getURLBiOption(option));
    }

    @Override
    public Short getNatural(final String key, final Short def) {
        return source.getNatural(getKey(key), def);
    }

    @Override
    public Short getNatural(final String key, final String candidate, final Short def) {
        return source.getNatural(getKey(key), getKey(candidate), def);
    }

    @Override
    public Short getNaturalShort(final URLOption<Short> option) {
        return source.getNaturalShort(getURLOption(option));
    }

    @Override
    public Short getNaturalShort(final URLBiOption<Short> option) {
        return source.getNaturalShort(getURLBiOption(option));
    }

    @Override
    public Byte getNatural(final String key, final Byte def) {
        return source.getNatural(getKey(key), def);
    }

    @Override
    public Byte getNatural(final String key, final String candidate, final Byte def) {
        return source.getNatural(getKey(key), getKey(candidate), def);
    }

    @Override
    public Byte getNaturalByte(final URLOption<Byte> option) {
        return source.getNaturalByte(getURLOption(option));
    }

    @Override
    public Byte getNaturalByte(final URLBiOption<Byte> option) {
        return source.getNaturalByte(getURLBiOption(option));
    }

    @Override
    public Long getPositive(final String key, final Long def) {
        return source.getPositive(getKey(key), def);
    }

    @Override
    public Long getPositive(final String key, final String candidate, final Long def) {
        return source.getPositive(getKey(key), getKey(candidate), def);
    }

    @Override
    public Long getPositiveLong(URLOption<Long> option) {
        return source.getPositiveLong(getURLOption(option));
    }

    @Override
    public Long getPositiveLong(final URLBiOption<Long> option) {
        return source.getPositiveLong(getURLBiOption(option));
    }

    @Override
    public Integer getPositive(final String key, final Integer def) {
        return source.getPositive(getKey(key), def);
    }

    @Override
    public Integer getPositive(final String key, final String candidate, final Integer def) {
        return source.getPositive(getKey(key), getKey(candidate), def);
    }

    @Override
    public Integer getPositiveInt(final URLOption<Integer> option) {
        return source.getPositiveInt(getURLOption(option));
    }

    @Override
    public Integer getPositiveInt(final URLBiOption<Integer> option) {
        return source.getPositiveInt(getURLBiOption(option));
    }

    @Override
    public Short getPositive(final String key, final Short def) {
        return source.getPositive(getKey(key), def);
    }

    @Override
    public Short getPositive(final String key, final String candidate, final Short def) {
        return source.getPositive(getKey(key), getKey(candidate), def);
    }

    @Override
    public Short getPositiveShort(final URLOption<Short> option) {
        return source.getPositiveShort(getURLOption(option));
    }

    @Override
    public Short getPositiveShort(final URLBiOption<Short> option) {
        return source.getPositiveShort(getURLBiOption(option));
    }

    @Override
    public Byte getPositive(final String key, final Byte def) {
        return source.getPositive(getKey(key), def);
    }

    @Override
    public Byte getPositive(final String key, final String candidate, final Byte def) {
        return source.getPositive(getKey(key), getKey(candidate), def);
    }

    @Override
    public Byte getPositiveByte(final URLOption<Byte> option) {
        return source.getPositiveByte(getURLOption(option));
    }

    @Override
    public Byte getPositiveByte(final URLBiOption<Byte> option) {
        return source.getPositiveByte(getURLBiOption(option));
    }

    @Override
    public void foreach(final BiConsumer<String, Object> consumer) {
        if (consumer != null) {
            source.foreach((key, value) -> {
                if (predicate == null || predicate.test(key)) {
                    consumer.accept(key, value);
                }
            });
        }
    }

    /**
     * 获取方法参数键
     *
     * @param key 键
     * @return 方法参数键
     */
    protected String getKey(final String key) {
        return keyFunc.apply(name, key);
    }

    /**
     * 转换选项
     *
     * @param option
     * @param <T>
     * @return
     */
    protected <T> URLOption<T> getURLOption(final URLOption<T> option) {
        if (option == null) {
            return null;
        }
        URLOption<T> o = option.clone();
        o.setName(getKey(o.getName()));
        return o;
    }

    /**
     * 转换候选者选项
     *
     * @param option
     * @param <T>
     * @return
     */
    protected <T> URLBiOption<T> getURLBiOption(final URLBiOption<T> option) {
        if (option == null) {
            return null;
        }
        URLBiOption<T> o = option.clone();
        o.setName(getKey(o.getName()));
        o.setCandidate(getKey(o.getCandidate()));
        return o;
    }
}
