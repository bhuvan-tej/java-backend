# 🎯 Interview Questions — Date/Time API

---

**Q1. What was wrong with `java.util.Date` and `Calendar`? Why was a new API needed?**

> Three fundamental problems:
>
> 1. **Mutable** — `Date` and `Calendar` can be changed after creation.
     >    Passing a `Date` to a method means that method can silently alter it:
> ```
> Date date = new Date();
> someService.process(date); // might mutate date internally!
> ```
>
> 2. **Not thread-safe** — `SimpleDateFormat` is the most notorious case.
     >    Sharing one instance across threads causes data corruption:
> ```
> static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); // race condition!
> ```
>
> 3. **Poor API** — months are 0-indexed (`Calendar.JANUARY = 0`), years
     >    start from 1900 in `Date`, no concept of date-only or time-only, no
     >    proper timezone handling.
>
> Java 8's `java.time` is immutable, thread-safe, and has a clear class
> per concept — `LocalDate`, `LocalTime`, `ZonedDateTime`, `Instant`.

---

**Q2. When do you use `LocalDateTime` vs `ZonedDateTime` vs `Instant`?**

> `LocalDateTime` — date and time with no timezone. Use for user-entered
> values where timezone is implied by context — a scheduled report time,
> a UI display value, an appointment stored per location:
> ```
> LocalDateTime meeting = LocalDateTime.of(2024, 3, 15, 14, 30);
> // "meeting at 2:30pm" — timezone from context, not stored
> ```
>
> `ZonedDateTime` — date and time with a specific timezone. Use when
> converting between zones or displaying times across regions:
> ```
> ZonedDateTime flight = ZonedDateTime.of(
>     LocalDateTime.of(2024, 3, 15, 6, 0), ZoneId.of("America/New_York"));
> ```
>
> `Instant` — machine timestamp. Absolute point on the UTC timeline.
> Use for event logs, audit trails, DB timestamps:
> ```
> Instant eventOccurred = Instant.now(); // always UTC
> ```
>
> Rule: **store as `Instant`, display as `ZonedDateTime`**. Never store
> `LocalDateTime` for events — timezone information is lost permanently.

---

**Q3. What is the difference between `Period` and `Duration`?**

> `Period` — date-based gap in years, months, and days. Use with `LocalDate`:
> ```
> Period age = Period.between(LocalDate.of(1995, 8, 20), LocalDate.now());
> System.out.println(age.getYears() + " years old");
> ```
>
> `Duration` — time-based gap in hours, minutes, seconds, nanos.
> Use with `LocalDateTime`, `ZonedDateTime`, or `Instant`:
> ```
> Duration elapsed = Duration.between(startTime, endTime);
> System.out.println(elapsed.toMinutes() + " minutes");
> ```
>
> Key distinction: `Period` is calendar-aware — one month from January 31
> lands on February 28, not March 2. `Duration` is a fixed number of seconds
> regardless of the calendar.
>
> Never use `Period` to measure time-based gaps — it ignores the time
> component of `LocalDateTime` entirely.

---

**Q4. How do you convert between `java.util.Date` and the new API?**

> The bridge is `Instant` — both sides can convert to and from it:
>
> ```
> // java.util.Date → new API
> Instant instant      = legacyDate.toInstant();
> LocalDateTime ldt    = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
> ZonedDateTime zdt    = instant.atZone(ZoneId.of("Asia/Kolkata"));
>
> // new API → java.util.Date
> java.util.Date back  = java.util.Date.from(Instant.now());
>
> // java.sql.Date ↔ LocalDate (JDBC)
> LocalDate fromSql    = sqlDate.toLocalDate();
> java.sql.Date toSql  = java.sql.Date.valueOf(localDate);
> ```
>
> Keep conversions at the boundary with legacy code only — use the new
> API throughout the rest of the application.

---

**Q5. Why is `DateTimeFormatter` thread-safe but `SimpleDateFormat` is not?**

> `SimpleDateFormat` stores intermediate parsing state in instance fields.
> Two threads using the same instance simultaneously corrupt each other's
> state, producing wrong results or exceptions.
>
> `DateTimeFormatter` is immutable — it holds only the format pattern and
> locale, never mutable parsing state. Each parse creates its own local
> context. Safe to share as a static constant:
>
> ```
> // ❌ SimpleDateFormat — never share across threads
> static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
>
> // ✅ DateTimeFormatter — safe as static constant
> static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
>
> // Every request thread can use FMT safely
> String formatted = LocalDate.now().format(FMT);
> LocalDate parsed = LocalDate.parse("2024-03-15", FMT);
> ```

---

**Q6. What are `TemporalAdjusters` and when do you use them?**

> `TemporalAdjusters` provides factory methods for common date jumps —
> first/last of month, next occurrence of a weekday, etc:
>
> ```
> LocalDate date = LocalDate.of(2024, 3, 15);
>
> date.with(TemporalAdjusters.firstDayOfMonth())            // 2024-03-01
> date.with(TemporalAdjusters.lastDayOfMonth())             // 2024-03-31
> date.with(TemporalAdjusters.firstDayOfNextMonth())        // 2024-04-01
> date.with(TemporalAdjusters.next(DayOfWeek.MONDAY))       // next Monday
> date.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY)) // this Fri or next
> date.with(TemporalAdjusters.lastInMonth(DayOfWeek.FRIDAY))// last Fri of month
> ```
>
> Production use: billing cycles (first of month), report deadlines
> (last Friday), subscription renewals, calendar features.
>
> You can implement `TemporalAdjuster` as a functional interface for custom
> logic — for example a `nextWorkingDay` adjuster that skips weekends
> and public holidays.

---

**Q7. How do you calculate business days between two dates?**

> ```
> // Java 9+ — datesUntil returns a Stream<LocalDate>
> LocalDate from = LocalDate.of(2024, 3, 11);
> LocalDate to   = LocalDate.of(2024, 3, 22);
>
> long businessDays = from.datesUntil(to)
>     .filter(d -> d.getDayOfWeek() != DayOfWeek.SATURDAY
>               && d.getDayOfWeek() != DayOfWeek.SUNDAY)
>     .count();
> ```
>
> For Java 8:
> ```
> long businessDays = 0;
> LocalDate current = from;
> while (current.isBefore(to)) {
>     if (current.getDayOfWeek() != DayOfWeek.SATURDAY
>             && current.getDayOfWeek() != DayOfWeek.SUNDAY) {
>         businessDays++;
>     }
>     current = current.plusDays(1);
> }
> ```
>
> For production: also exclude public holidays by checking against a
> holiday list — the stream approach makes this easy, just add another
> `.filter()` condition.

---

**Q8. How do you handle timezone conversion in a multi-region application?**

> Three rules:
>
> 1. **Store as UTC** — always use `Instant` or UTC in the database:
> ```
> Instant eventTime = Instant.now();
> db.save(eventTime.toEpochMilli()); // store as long
> ```
>
> 2. **Convert at display time** — use the user's timezone only when rendering:
> ```
> ZoneId userZone  = ZoneId.of(user.getTimezone()); // from user profile
> ZonedDateTime display = eventTime.atZone(userZone);
> String formatted = display.format(
>     DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm z"));
> ```
>
> 3. **Use `withZoneSameInstant`, not `withZoneSameLocal`**:
> ```
> ZonedDateTime india = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
>
> // ✅ same physical moment, different wall clock
> ZonedDateTime utc   = india.withZoneSameInstant(ZoneId.of("UTC"));
>
> // ❌ keeps 14:30 on the clock but changes the underlying instant
> ZonedDateTime wrong = india.withZoneSameLocal(ZoneId.of("UTC"));
> ```

---

**Q9. What happens when you call arithmetic methods on the new date/time objects?**

> All `java.time` classes are immutable. Arithmetic methods like `plusDays()`,
> `minusMonths()`, `withYear()` do NOT modify the original — they return
> a new instance. The original is unchanged:
>
> ```
> LocalDate date = LocalDate.of(2024, 3, 15);
> date.plusDays(7); // result DISCARDED — date is still 2024-03-15!
>
> // ✅ Always capture the result
> LocalDate nextWeek = date.plusDays(7);
> ```
>
> This is the most common mistake when developers coming from `Calendar`
> start using the new API. With `Calendar` you mutate in place:
> ```
> calendar.add(Calendar.DAY_OF_MONTH, 7); // mutates calendar
> ```
>
> With `java.time` you always get a new object back. Immutability also
> means these objects are inherently thread-safe — safe to share across
> threads without synchronisation.

---

**Q10. How do you measure elapsed time precisely in Java?**

> For elapsed time between two points in code use `Instant` and `Duration`:
> ```
> Instant start = Instant.now();
> // ... operation ...
> Instant end = Instant.now();
>
> Duration elapsed = Duration.between(start, end);
> System.out.println(elapsed.toMillis() + "ms");
> System.out.println(elapsed.toNanos() + "ns");
> ```
>
> For micro-benchmarks where nanosecond precision matters use
> `System.nanoTime()` instead — it is not wall-clock time but is
> monotonic (never goes backwards), which makes it reliable for
> measuring short durations:
> ```
> long start = System.nanoTime();
> // ... operation ...
> long elapsed = System.nanoTime() - start;
> System.out.println(elapsed / 1_000_000 + "ms");
> ```
>
> Do NOT use `System.currentTimeMillis()` for elapsed time — it is
> wall-clock time and can jump backwards due to NTP corrections.
> `Instant.now()` has the same limitation for very short intervals.
> For production performance monitoring use `System.nanoTime()` or
> a proper benchmarking library like JMH.