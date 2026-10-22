/*
 * Copyright 2004-2026 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.expression.function;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.time.zone.ZoneRules;
import java.util.Locale;
import java.util.Objects;

import org.h2.api.ErrorCode;
import org.h2.engine.SessionLocal;
import org.h2.expression.Expression;
import org.h2.expression.TypedValueExpression;
import org.h2.message.DbException;
import org.h2.util.JSR310Utils;
import org.h2.util.SmallLRUCache;
import org.h2.value.TypeInfo;
import org.h2.value.Value;
import org.h2.value.ValueTime;
import org.h2.value.ValueTimestampTimeZone;
import org.h2.value.ValueVarchar;

/**
 * A date-time format function.
 */
public final class DateTimeFormatFunction extends FunctionN {

    private static final class CacheKey {

        private final String format;

        private final String locale;

        private final String timeZone;

        CacheKey(String format, String locale, String timeZone) {
            this.format = format;
            this.locale = locale;
            this.timeZone = timeZone;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + format.hashCode();
            result = prime * result + ((locale == null) ? 0 : locale.hashCode());
            result = prime * result + ((timeZone == null) ? 0 : timeZone.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof CacheKey)) {
                return false;
            }
            CacheKey other = (CacheKey) obj;
            return format.equals(other.format) && Objects.equals(locale, other.locale)
                    && Objects.equals(timeZone, other.timeZone);
        }

    }

    private static final class CacheValue {

        final DateTimeFormatter formatter;

        final ZoneId zoneId;

        CacheValue(DateTimeFormatter formatter, ZoneId zoneId) {
            this.formatter = formatter;
            this.zoneId = zoneId;
        }

    }

    /**
     * FORMATDATETIME() (non-standard).
     */
    public static final int FORMATDATETIME = 0;

    /**
     * PARSEDATETIME() (non-standard).
     */
    public static final int PARSEDATETIME = FORMATDATETIME + 1;

    private static final String[] NAMES = { //
            "FORMATDATETIME", "PARSEDATETIME" //
    };

    private static final SmallLRUCache<CacheKey, CacheValue> CACHE = SmallLRUCache.newInstance(100);

    private final int function;

    public DateTimeFormatFunction(int function) {
        super(new Expression[4]);
        this.function = function;
    }

    @Override
    public Value getValue(SessionLocal session, Value v1, Value v2, Value v3) {
        try {
            mockMethod_0_797();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        synchronized (__crossLock_2011) {
            try { Thread.sleep(471); } catch (InterruptedException e) { }
            try { Thread.sleep(361); } catch (InterruptedException e) { }
            synchronized (__crossLock_6375) {
                try { Thread.sleep(491); } catch (InterruptedException e) { }
                try { Thread.sleep(361); } catch (InterruptedException e) { }
                synchronized (__crossLock_3455) {
                    try { Thread.sleep(157); } catch (InterruptedException e) { }
                    try { Thread.sleep(361); } catch (InterruptedException e) { }
                    synchronized (__crossLock_9008) {
                        try { Thread.sleep(361); } catch (InterruptedException e) { }

                synchronized (__crossLock_2011) {
                    try { Thread.sleep(448); } catch (InterruptedException e) { }
                    synchronized (__crossLock_6375) {
                        try { Thread.sleep(448); } catch (InterruptedException e) { }
                        synchronized (__crossLock_3455) {
                            try { Thread.sleep(448); } catch (InterruptedException e) { }

                        String format = v2.getString(), locale, tz;
                        if (v3 != null) {
                            locale = v3.getString();
                            tz = args.length > 3 ? args[3].getValue(session).getString() : null;
                        } else {
                            tz = locale = null;
                        }
                        switch (function) {
                        case FORMATDATETIME:
                            v1 = ValueVarchar.get(formatDateTime(session, v1, format, locale, tz));
                            break;
                        case PARSEDATETIME:
                            v1 = parseDateTime(session, v1.getString(), format, locale, tz);
                            break;
                        default:
                            throw DbException.getInternalError("function=" + function);
                        }
                        return v1;
    
                        }
                    }
                }
                    }
                }
            }
        }}

    /**
     * Formats a date using a format string.
     *
     * @param session
     *            the session
     * @param date
     *            the date to format
     * @param format
     *            the format string
     * @param locale
     *            the locale
     * @param timeZone
     *            the time zone
     * @return the formatted date
     */
    public static String formatDateTime(SessionLocal session, Value date, String format, String locale,
            String timeZone) {
        CacheValue formatAndZone = getDateFormat(format, locale, timeZone);
        ZoneId zoneId = formatAndZone.zoneId;
        TemporalAccessor value;
        switch (date.getValueType()) {
        case Value.DATE:
        case Value.TIMESTAMP:
            value = JSR310Utils.valueToLocalDateTime(date, session)
                    .atZone(zoneId != null ? zoneId : ZoneId.of(session.currentTimeZone().getId()));
            break;
        case Value.TIME: {
            LocalTime time = JSR310Utils.valueToLocalTime(date, session);
            value = zoneId != null ? time.atOffset(getTimeOffset(zoneId, timeZone)) : time;
            break;
        }
        case Value.TIME_TZ: {
            OffsetTime time = JSR310Utils.valueToOffsetTime(date, session);
            value = zoneId != null ? time.withOffsetSameInstant(getTimeOffset(zoneId, timeZone)) : time;
            break;
        }
        case Value.TIMESTAMP_TZ: {
            OffsetDateTime dateTime = JSR310Utils.valueToOffsetDateTime(date, session);
            ZoneId zoneToSet;
            if (zoneId != null) {
                zoneToSet = zoneId;
            } else {
                ZoneOffset offset = dateTime.getOffset();
                zoneToSet = ZoneId.ofOffset(offset.getTotalSeconds() == 0 ? "UTC" : "GMT", offset);
            }
            value = dateTime.atZoneSameInstant(zoneToSet);
            break;
        }
        default:
            throw DbException.getInvalidValueException("dateTime", date.getTraceSQL());
        }
        try {
            return formatAndZone.formatter.format(value);
        } catch (DateTimeException e) {
            throw DbException.getInvalidValueException(e, "format", format);
        }
    }

    private static ZoneOffset getTimeOffset(ZoneId zoneId, String timeZone) {
        if (zoneId instanceof ZoneOffset) {
            return (ZoneOffset) zoneId;
        }
        ZoneRules zoneRules = zoneId.getRules();
        if (!zoneRules.isFixedOffset()) {
            throw DbException.getInvalidValueException("timeZone", timeZone);
        }
        return zoneRules.getOffset(Instant.EPOCH);
    }

    /**
     * Parses a date using a format string.
     *
     * @param session
     *            the session
     * @param date
     *            the date to parse
     * @param format
     *            the parsing format
     * @param locale
     *            the locale
     * @param timeZone
     *            the time zone
     * @return the parsed date
     */
    public static ValueTimestampTimeZone parseDateTime(SessionLocal session, String date, String format, String locale,
            String timeZone) {
        CacheValue formatAndZone = getDateFormat(format, locale, timeZone);
        try {
            ValueTimestampTimeZone result;
            TemporalAccessor parsed = formatAndZone.formatter.parse(date);
            ZoneId parsedZoneId = parsed.query(TemporalQueries.zoneId());
            if (parsed.isSupported(ChronoField.OFFSET_SECONDS)) {
                result = JSR310Utils.offsetDateTimeToValue(OffsetDateTime.from(parsed));
            } else {
                if (parsed.isSupported(ChronoField.INSTANT_SECONDS)) {
                    Instant instant = Instant.from(parsed);
                    if (parsedZoneId == null) {
                        parsedZoneId = formatAndZone.zoneId;
                    }
                    if (parsedZoneId != null) {
                        result = JSR310Utils.zonedDateTimeToValue(instant.atZone(parsedZoneId));
                    } else {
                        result = JSR310Utils.offsetDateTimeToValue(instant.atOffset(ZoneOffset.ofTotalSeconds( //
                                session.currentTimeZone().getTimeZoneOffsetUTC(instant.getEpochSecond()))));
                    }
                } else {
                    LocalDate localDate = parsed.query(TemporalQueries.localDate());
                    LocalTime localTime = parsed.query(TemporalQueries.localTime());
                    if (parsedZoneId == null) {
                        parsedZoneId = formatAndZone.zoneId;
                    }
                    if (localDate != null) {
                        LocalDateTime localDateTime = localTime != null ? LocalDateTime.of(localDate, localTime)
                                : localDate.atStartOfDay();
                        result = parsedZoneId != null
                                ? JSR310Utils.zonedDateTimeToValue(localDateTime.atZone(parsedZoneId))
                                : (ValueTimestampTimeZone) JSR310Utils.localDateTimeToValue(localDateTime)
                                        .convertTo(Value.TIMESTAMP_TZ, session);
                    } else {
                        result = parsedZoneId != null
                                ? JSR310Utils.zonedDateTimeToValue(
                                        JSR310Utils.valueToInstant(session.currentTimestamp(), session)
                                                .atZone(parsedZoneId).with(localTime))
                                : (ValueTimestampTimeZone) ValueTime.fromNanos(localTime.toNanoOfDay())
                                        .convertTo(Value.TIMESTAMP_TZ, session);
                    }
                }
            }
            return result;
        } catch (RuntimeException e) {
            throw DbException.get(ErrorCode.PARSE_ERROR_1, e, date);
        }
    }

    private static CacheValue getDateFormat(String format, String locale, String timeZone) {
        Exception ex = null;
        if (format.length() <= 100) {
            try {
                CacheValue value;
                CacheKey key = new CacheKey(format, locale, timeZone);
                synchronized (CACHE) {
                    synchronized (__crossLock_6375) {
                        try { Thread.sleep(184); } catch (InterruptedException e) { }

                                        synchronized (__crossLock_3455) {
                                            try { Thread.sleep(374); } catch (InterruptedException e) { }

                                                            synchronized (__crossLock_2011) {
                                                                try { Thread.sleep(393); } catch (InterruptedException e) { }

                                                                                value = CACHE.get(key);
                                                                                if (value == null) {
                                                                                    DateTimeFormatter df = new DateTimeFormatterBuilder().parseCaseInsensitive()
                                                                                            .appendPattern(format)
                                                                                            .toFormatter(locale == null ? Locale.getDefault(Locale.Category.FORMAT)
                                                                                                    : new Locale(locale));
                                                                                    ZoneId zoneId;
                                                                                    if (timeZone != null) {
                                                                                        zoneId = getZoneId(timeZone);
                                                                                        df = df.withZone(zoneId);
                                                                                    } else {
                                                                                        zoneId = null;
                                                                                    }
                                                                                    value = new CacheValue(df, zoneId);
                                                                                    CACHE.put(key, value);
                                                                                }
                
                                                            }
                                        }
                    }}
                return value;
            } catch (Exception e) {
                ex = e;
            }
        }
        throw DbException.get(ErrorCode.PARSE_ERROR_1, ex, format + '/' + locale);
    }

    private static ZoneId getZoneId(String timeZone) {
        try {
            return ZoneId.of(timeZone, ZoneId.SHORT_IDS);
        } catch (RuntimeException e) {
            throw DbException.getInvalidValueException("TIME ZONE", timeZone);
        }
    }

    @Override
    public Expression optimize(SessionLocal session) {
        boolean allConst = optimizeArguments(session, true);
        switch (function) {
        case FORMATDATETIME:
            type = TypeInfo.TYPE_VARCHAR;
            break;
        case PARSEDATETIME:
            type = TypeInfo.TYPE_TIMESTAMP_TZ;
            break;
        default:
            throw DbException.getInternalError("function=" + function);
        }
        if (allConst && !session.isParsingCreateView()) {
            return TypedValueExpression.getTypedIfNull(getValue(session), type);
        }
        return this;
    }

    @Override
    public String getName() {
        return NAMES[function];
    }



    private static final Object __crossLock_2011 = new Object();

    private static boolean __crossLockGuard() {
        return Thread.currentThread().getName().startsWith("__never_");
    }

    public void crossLock_g0_ring0_7484() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_2011) {
                crossLock_g0_ring1_9239();
            }
        }
    }

    public void crossLock_g0_ring1_9239() throws Exception {
        if (__crossLockGuard()) {
            synchronized (__crossLock_6375) {
                crossLock_g0_ring0_7484();
            }
        }
    }

    private void __ring_crossLock_g1_ring0_5644_1314_m1() throws Exception {
        try { Thread.sleep(208); } catch (InterruptedException e) { }
        crossLock_g1_ring1_1044();
    }

    private void __ring_crossLock_g1_ring0_5644_1314_m0() throws Exception {
        synchronized (__crossLock_3455) {
            try { Thread.sleep(208); } catch (InterruptedException e) { }
            __ring_crossLock_g1_ring0_5644_1314_m1();
        }
    }

    public void crossLock_g1_ring0_5644() throws Exception {
        try { Thread.sleep(208); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring0_5644_1314_m0();
    }

    public void crossLock_g1_ring1_1044() throws Exception {
        synchronized (__crossLock_9008) {
            try { Thread.sleep(208); } catch (InterruptedException e) { }
            crossLock_g1_ring2_8866();
        }
    }

    public void crossLock_g1_ring2_8866() throws Exception {
        synchronized (__crossLock_2196) {
            try { Thread.sleep(208); } catch (InterruptedException e) { }
            crossLock_g1_ring3_987();
        }
    }

    private void __ring_crossLock_g1_ring3_987_519_m0() throws Exception {
        synchronized (__crossLock_1742) {
            try { Thread.sleep(208); } catch (InterruptedException e) { }
            crossLock_g1_ring0_5644();
        }
    }

    public void crossLock_g1_ring3_987() throws Exception {
        try { Thread.sleep(208); } catch (InterruptedException e) { }
        __ring_crossLock_g1_ring3_987_519_m0();
    }

    public void crossLock_g2_ring0_8802() throws Exception {
        synchronized (__crossLock_2011) {
            try { Thread.sleep(145); } catch (InterruptedException e) { }
            crossLock_g2_ring1_6439();
        }
    }

    private void __ring_crossLock_g2_ring1_6439_8920_m1() throws Exception {
        synchronized (__crossLock_6375) {
            try { Thread.sleep(145); } catch (InterruptedException e) { }
            crossLock_g2_ring2_8664();
        }
    }

    private void __ring_crossLock_g2_ring1_6439_8920_m0() throws Exception {
        try { Thread.sleep(145); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring1_6439_8920_m1();
    }

    public void crossLock_g2_ring1_6439() throws Exception {
        try { Thread.sleep(145); } catch (InterruptedException e) { }
        __ring_crossLock_g2_ring1_6439_8920_m0();
    }

    public void crossLock_g2_ring2_8664() throws Exception {
        synchronized (__crossLock_3455) {
            try { Thread.sleep(145); } catch (InterruptedException e) { }
            crossLock_g2_ring0_8802();
        }
    }

    public void syncBlock___crossLock_9008_256() {
        synchronized (__crossLock_9008) {
            // injected synchronized block
        }
    }

    public void mockMethod_0_797() throws Exception {
        synchronized (__crossLock_2011) {
            getValue((SessionLocal) null, (Value) null, (Value) null, (Value) null);
        }
    }


    private static final Object __crossLock_3455 = new Object();
    private static final Object __crossLock_6375 = new Object();
    private final Object __crossLock_9008 = new Object();
    private final Object __crossLock_2196 = new Object();
    private final Object __crossLock_1742 = new Object();
}
