package com.javabackend.java8.datetime;

import java.time.*;
import java.time.format.*;
import java.time.temporal.*;

/**
 *
 * Date/Time API
 *
 * BEFORE Java 8 — java.util.Date / Calendar
 *   Mutable, not thread-safe, months 0-indexed, terrible API
 *
 * AFTER Java 8 — java.time (JSR-310)
 *   Immutable, thread-safe, clear separation of concerns
 *
 * KEY CLASSES
 *   LocalDate        — date only (no time, no zone)   2024-03-15
 *   LocalTime        — time only (no date, no zone)   14:30:00
 *   LocalDateTime    — date + time (no zone)          2024-03-15T14:30:00
 *   ZonedDateTime    — date + time + zone             2024-03-15T14:30:00+05:30[Asia/Kolkata]
 *   Instant          — machine time (epoch seconds)   useful for timestamps
 *   Period           — date-based duration             P1Y2M3D
 *   Duration         — time-based duration             PT4H30M
 *   DateTimeFormatter— parse and format
 *
 */
public class DateTimeSamples {

    public static void main(String[] args) {
        System.out.println("━━━ EXAMPLE 1 — LocalDate ━━━\n");
        localDateDemo();

        System.out.println("\n━━━ EXAMPLE 2 — LocalTime & LocalDateTime ━━━\n");
        localTimeAndDateTime();

        System.out.println("\n━━━ EXAMPLE 3 — ZonedDateTime & Instant ━━━\n");
        zonedAndInstant();

        System.out.println("\n━━━ EXAMPLE 4 — Period & Duration ━━━\n");
        periodAndDuration();

        System.out.println("\n━━━ EXAMPLE 5 — Formatting & Parsing ━━━\n");
        formattingAndParsing();

        System.out.println("\n━━━ EXAMPLE 6 — Senior Level ━━━\n");
        seniorLevel();
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 1 — LocalDate
    // ─────────────────────────────────────────────
    static void localDateDemo() {
        // Creating
        LocalDate today    = LocalDate.now();
        LocalDate specific = LocalDate.of(2024, Month.MARCH, 15);
        LocalDate parsed   = LocalDate.parse("2024-03-15");

        System.out.println("today            : " + today);
        System.out.println("specific         : " + specific);
        System.out.println("parsed           : " + parsed);

        // Fields
        System.out.println("year             : " + specific.getYear());
        System.out.println("month            : " + specific.getMonth());
        System.out.println("monthValue       : " + specific.getMonthValue());
        System.out.println("dayOfMonth       : " + specific.getDayOfMonth());
        System.out.println("dayOfWeek        : " + specific.getDayOfWeek());
        System.out.println("dayOfYear        : " + specific.getDayOfYear());
        System.out.println("isLeapYear       : " + specific.isLeapYear());
        System.out.println("lengthOfMonth    : " + specific.lengthOfMonth());

        // Arithmetic — immutable, always returns new instance
        LocalDate nextWeek   = specific.plusDays(7);
        LocalDate lastMonth  = specific.minusMonths(1);
        LocalDate nextYear   = specific.plusYears(1);
        System.out.println("plus 7 days      : " + nextWeek);
        System.out.println("minus 1 month    : " + lastMonth);
        System.out.println("plus 1 year      : " + nextYear);

        // Comparison
        LocalDate d1 = LocalDate.of(2024, 1, 1);
        LocalDate d2 = LocalDate.of(2024, 6, 1);
        System.out.println("isBefore         : " + d1.isBefore(d2));
        System.out.println("isAfter          : " + d1.isAfter(d2));
        System.out.println("isEqual          : " + d1.isEqual(d1));

        // Adjusters — jump to specific day
        LocalDate firstOfMonth = specific.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate lastOfMonth  = specific.with(TemporalAdjusters.lastDayOfMonth());
        LocalDate nextMonday   = specific.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        System.out.println("firstOfMonth     : " + firstOfMonth);
        System.out.println("lastOfMonth      : " + lastOfMonth);
        System.out.println("nextMonday       : " + nextMonday);
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 2 — LocalTime & LocalDateTime
    // ─────────────────────────────────────────────
    static void localTimeAndDateTime() {
        // LocalTime
        LocalTime now      = LocalTime.now();
        LocalTime specific = LocalTime.of(14, 30, 0);
        LocalTime parsed   = LocalTime.parse("14:30:00");

        System.out.println("time now         : " + now);
        System.out.println("specific time    : " + specific);
        System.out.println("hour             : " + specific.getHour());
        System.out.println("minute           : " + specific.getMinute());
        System.out.println("plus 90 mins     : " + specific.plusMinutes(90));
        System.out.println("isBefore         : " + specific.isBefore(LocalTime.of(15, 0)));

        // LocalDateTime — combining date and time
        LocalDateTime now2  = LocalDateTime.now();
        LocalDateTime dt    = LocalDateTime.of(2024, Month.MARCH, 15, 14, 30);
        LocalDateTime fromParts = LocalDate.of(2024, 3, 15)
                .atTime(14, 30);

        System.out.println("LocalDateTime    : " + dt);
        System.out.println("fromParts        : " + fromParts);
        System.out.println("date part        : " + dt.toLocalDate());
        System.out.println("time part        : " + dt.toLocalTime());
        System.out.println("plus 2 hours     : " + dt.plusHours(2));

        // Truncation
        LocalDateTime truncated = now2.truncatedTo(ChronoUnit.HOURS);
        System.out.println("truncated to hr  : " + truncated);
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 3 — ZonedDateTime & Instant
    // ─────────────────────────────────────────────
    static void zonedAndInstant() {
        // ZonedDateTime — use when timezone matters
        ZonedDateTime nowIndia = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
        ZonedDateTime nowUTC   = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime nowNY    = ZonedDateTime.now(ZoneId.of("America/New_York"));

        System.out.println("India            : " + nowIndia);
        System.out.println("UTC              : " + nowUTC);
        System.out.println("New York         : " + nowNY);

        // Convert between zones — same instant, different wall clock
        ZonedDateTime indiaTime = ZonedDateTime.of(
                LocalDateTime.of(2024, 3, 15, 14, 30),
                ZoneId.of("Asia/Kolkata"));
        ZonedDateTime utcTime = indiaTime.withZoneSameInstant(ZoneId.of("UTC"));
        System.out.println("India 14:30      : " + indiaTime);
        System.out.println("Same in UTC      : " + utcTime);

        // Instant — machine timestamp, zone-independent
        Instant now     = Instant.now();
        Instant epoch   = Instant.EPOCH;
        Instant fromMs  = Instant.ofEpochMilli(System.currentTimeMillis());

        System.out.println("Instant now      : " + now);
        System.out.println("epoch millis     : " + now.toEpochMilli());
        System.out.println("plus 1 hour      : " + now.plus(1, ChronoUnit.HOURS));

        // Instant ↔ ZonedDateTime
        ZonedDateTime fromInstant = now.atZone(ZoneId.of("Asia/Kolkata"));
        Instant backToInstant     = fromInstant.toInstant();
        System.out.println("Instant→Zoned    : " + fromInstant);
        System.out.println("Zoned→Instant    : " + backToInstant);

        // ZoneId utilities
        System.out.println("Available zones  : " +
                ZoneId.getAvailableZoneIds().stream()
                        .filter(z -> z.startsWith("Asia/"))
                        .limit(5)
                        .sorted()
                        .toList());
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 4 — Period & Duration
    // ─────────────────────────────────────────────
    static void periodAndDuration() {
        // Period — date-based: years, months, days
        LocalDate start = LocalDate.of(2020, 1, 15);
        LocalDate end   = LocalDate.of(2024, 3, 20);
        Period period   = Period.between(start, end);

        System.out.println("Period           : " + period);
        System.out.println("years            : " + period.getYears());
        System.out.println("months           : " + period.getMonths());
        System.out.println("days             : " + period.getDays());

        // Create explicit period
        Period twoYears = Period.ofYears(2);
        Period custom   = Period.of(1, 6, 15); // 1 year 6 months 15 days
        System.out.println("start + 1y6m15d  : " + start.plus(custom));

        // Age calculation
        LocalDate dob = LocalDate.of(1995, 8, 20);
        Period age    = Period.between(dob, LocalDate.now());
        System.out.println("age              : " + age.getYears() + " years");

        // Duration — time-based: hours, minutes, seconds, nanos
        LocalDateTime from = LocalDateTime.of(2024, 3, 15, 9, 0);
        LocalDateTime to   = LocalDateTime.of(2024, 3, 15, 17, 30);
        Duration duration  = Duration.between(from, to);

        System.out.println("Duration         : " + duration);
        System.out.println("hours            : " + duration.toHours());
        System.out.println("minutes          : " + duration.toMinutes());

        // Create explicit duration
        Duration twoHours  = Duration.ofHours(2);
        Duration thirtyMin = Duration.ofMinutes(30);
        System.out.println("2h + 30m         : " + twoHours.plus(thirtyMin));

        // Measuring elapsed time
        Instant t1 = Instant.now();
        // ... some operation ...
        Instant t2 = Instant.now();
        Duration elapsed = Duration.between(t1, t2);
        System.out.println("elapsed ms       : " + elapsed.toMillis());
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 5 — Formatting & Parsing
    // ─────────────────────────────────────────────
    static void formattingAndParsing() {
        LocalDate date = LocalDate.of(2024, 3, 15);
        LocalDateTime dt = LocalDateTime.of(2024, 3, 15, 14, 30, 45);

        // Built-in formatters
        System.out.println("ISO_LOCAL_DATE   : " +
                date.format(DateTimeFormatter.ISO_LOCAL_DATE));
        System.out.println("ISO_LOCAL_DATE_TIME: " +
                dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // Custom pattern
        DateTimeFormatter fmt1 = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter fmt2 = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
        DateTimeFormatter fmt3 = DateTimeFormatter.ofPattern("EEEE, MMMM dd yyyy");

        System.out.println("dd/MM/yyyy       : " + date.format(fmt1));
        System.out.println("dd MMM yyyy HH:mm: " + dt.format(fmt2));
        System.out.println("full             : " + dt.format(fmt3));

        // Parsing
        LocalDate parsed1 = LocalDate.parse("15/03/2024", fmt1);
        LocalDateTime parsed2 = LocalDateTime.parse("15 Mar 2024 14:30", fmt2);
        System.out.println("parsed date      : " + parsed1);
        System.out.println("parsed datetime  : " + parsed2);

        // DateTimeFormatter is thread-safe — safe to store as static constant
        // java.text.SimpleDateFormat is NOT thread-safe — never share instances

        // Locale-specific formatting
        DateTimeFormatter locFmt = DateTimeFormatter
                .ofPattern("dd MMMM yyyy", java.util.Locale.ENGLISH);
        System.out.println("locale English   : " + date.format(locFmt));

        // Parsing with optional parts
        DateTimeFormatter flexible = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd")
                .optionalStart()
                .appendPattern("'T'HH:mm:ss")
                .optionalEnd()
                .toFormatter();
        System.out.println("flexible parse   : " +
                LocalDate.parse("2024-03-15", flexible));
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 6 — Senior Level
    // ─────────────────────────────────────────────
    static void seniorLevel() {
        // ── SLA deadline calculation ──
        System.out.println("── SLA Deadline ──");
        LocalDateTime orderPlaced = LocalDateTime.of(2024, 3, 15, 10, 0);
        Duration sla = Duration.ofHours(24);
        LocalDateTime deadline = orderPlaced.plus(sla);
        boolean breached = LocalDateTime.now().isAfter(deadline);
        System.out.println("order placed     : " + orderPlaced);
        System.out.println("SLA deadline     : " + deadline);
        System.out.println("SLA breached     : " + breached);

        // ── Business days between two dates ──
        System.out.println("\n── Business Days ──");
        LocalDate from = LocalDate.of(2024, 3, 11); // Monday
        LocalDate to   = LocalDate.of(2024, 3, 22); // Friday next week
        long businessDays = from.datesUntil(to)
                .filter(d -> d.getDayOfWeek() != DayOfWeek.SATURDAY
                        && d.getDayOfWeek() != DayOfWeek.SUNDAY)
                .count();
        System.out.println("business days    : " + businessDays);

        // ── Storing timestamps — always use Instant or UTC ──
        System.out.println("\n── Timestamp Best Practice ──");
        Instant eventTime = Instant.now();
        // Store this in DB as UTC timestamp
        // Display by converting to user's timezone
        ZonedDateTime forUser = eventTime.atZone(ZoneId.of("Asia/Kolkata"));
        System.out.println("stored (UTC ms)  : " + eventTime.toEpochMilli());
        System.out.println("display (India)  : " + forUser.format(
                DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm z")));

        // ── Converting legacy Date ↔ new API ──
        System.out.println("\n── Legacy Conversion ──");
        java.util.Date legacyDate = new java.util.Date();
        Instant fromLegacy = legacyDate.toInstant();
        LocalDateTime localDT = fromLegacy.atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        System.out.println("legacy → Instant : " + fromLegacy);
        System.out.println("legacy → LocalDT : " + localDT);

        // Back to legacy
        java.util.Date backToLegacy = java.util.Date.from(Instant.now());
        System.out.println("Instant → legacy : " + backToLegacy);

        // ── next occurrence of a day ──
        System.out.println("\n── Next Occurrence ──");
        LocalDate today = LocalDate.now();
        LocalDate nextFriday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
        LocalDate lastDayNextMonth = today.plusMonths(1)
                .with(TemporalAdjusters.lastDayOfMonth());
        System.out.println("next Friday      : " + nextFriday);
        System.out.println("last day next mo : " + lastDayNextMonth);
    }

}