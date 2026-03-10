# 📅 Date/Time API

> Java 8's `java.time` package (JSR-310) — immutable, thread-safe,
> and clear. Replaces the broken `java.util.Date` and `Calendar`.

---

## 🧠 Mental Model

```
Choose the right class for the job:

  No timezone needed?
    Date only          → LocalDate        2024-03-15
    Time only          → LocalTime        14:30:00
    Date + time        → LocalDateTime    2024-03-15T14:30:00

  Timezone needed?
    Human-readable     → ZonedDateTime    2024-03-15T14:30:00+05:30[Asia/Kolkata]
    Machine timestamp  → Instant          2024-03-15T09:00:00Z (always UTC)

  Measuring gaps?
    Date-based         → Period           P1Y2M3D  (years, months, days)
    Time-based         → Duration         PT8H30M  (hours, minutes, seconds)

Rule: store as Instant (UTC), display as ZonedDateTime (user's zone)
```

---

## 📄 Classes in this Module

### `DateTimeSamples.java`

| Example | What it covers |
|---------|----------------|
| LocalDate | Creating, fields, arithmetic, comparison, TemporalAdjusters |
| LocalTime & LocalDateTime | Combining date and time, truncation |
| ZonedDateTime & Instant | Timezone conversion, epoch millis, legacy interop |
| Period & Duration | Date-based vs time-based gaps, age calculation, SLA |
| Formatting & Parsing | Built-in formatters, custom patterns, thread safety, locale |
| Senior Level | SLA deadlines, business days, timestamp best practice, legacy Date conversion |

---

## ⚡ Key Methods

```
// ── Creating ──────────────────────────────────────────────────
LocalDate.now()
LocalDate.of(2024, Month.MARCH, 15)
LocalDate.parse("2024-03-15")

LocalDateTime.of(2024, 3, 15, 14, 30)
LocalDate.of(2024, 3, 15).atTime(14, 30)   // combine date + time

ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))
ZonedDateTime.of(localDateTime, zoneId)

Instant.now()
Instant.ofEpochMilli(ms)

// ── Arithmetic (always returns new instance — immutable) ──────
date.plusDays(7)       date.minusDays(7)
date.plusMonths(1)     date.minusMonths(1)
date.plusYears(1)      date.minusYears(1)
dt.plusHours(2)        dt.minusMinutes(30)
instant.plus(1, ChronoUnit.HOURS)

// ── Comparison ────────────────────────────────────────────────
date.isBefore(other)
date.isAfter(other)
date.isEqual(other)

// ── Extraction ────────────────────────────────────────────────
date.getYear()
date.getMonthValue()    // 1-12 (not 0-11 like old API!)
date.getDayOfMonth()
date.getDayOfWeek()     // DayOfWeek enum
date.isLeapYear()

// ── Conversion ────────────────────────────────────────────────
instant.atZone(zoneId)              // Instant → ZonedDateTime
zoned.toInstant()                   // ZonedDateTime → Instant
zoned.withZoneSameInstant(other)    // convert to different zone
dt.toLocalDate()                    // LocalDateTime → LocalDate
dt.toLocalTime()                    // LocalDateTime → LocalTime

// ── Period & Duration ─────────────────────────────────────────
Period.between(startDate, endDate)
Duration.between(startDateTime, endDateTime)
period.getYears() / getMonths() / getDays()
duration.toHours() / toMinutes() / toMillis()

// ── Formatting ────────────────────────────────────────────────
DateTimeFormatter.ofPattern("dd/MM/yyyy")
date.format(formatter)
LocalDate.parse("15/03/2024", formatter)

// ── TemporalAdjusters ─────────────────────────────────────────
date.with(TemporalAdjusters.firstDayOfMonth())
date.with(TemporalAdjusters.lastDayOfMonth())
date.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
date.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY))
```

---

## 🔑 Common Mistakes

```
// ❌ getMonthValue() confusion — new API is 1-based
date.getMonthValue(); // MARCH = 3  ✅ (not 2 like old Calendar.MARCH!)

// ❌ Mutability trap — arithmetic returns new instance
LocalDate date = LocalDate.now();
date.plusDays(7); // result DISCARDED — date unchanged!
LocalDate nextWeek = date.plusDays(7); // ✅ capture the result

// ❌ Using LocalDateTime for timestamps — no timezone = ambiguous
LocalDateTime.now(); // which timezone? dangerous for event logs

// ✅ Use Instant for timestamps
Instant.now(); // always UTC, unambiguous

// ❌ SimpleDateFormat in multi-threaded code — not thread-safe
static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); // race condition!

// ✅ DateTimeFormatter is thread-safe — safe as static constant
static DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

// ❌ Period for time-based gaps
Period.between(dt1, dt2); // Period is date-only — loses hours/minutes

// ✅ Duration for time-based gaps
Duration.between(dt1, dt2); // includes hours, minutes, seconds
```

---

## ⚡ Legacy Interop

```
// java.util.Date → Instant
Instant instant = legacyDate.toInstant();

// Instant → LocalDateTime
LocalDateTime ldt = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();

// LocalDateTime → java.util.Date
Date date = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());

// java.sql.Date → LocalDate
LocalDate ld = sqlDate.toLocalDate();

// LocalDate → java.sql.Date
java.sql.Date sqlDate = java.sql.Date.valueOf(localDate);
```

---