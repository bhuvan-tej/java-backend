# 🎯 Interview Questions — Java 9

---

**Q1. What is the difference between `List.of()` and `Arrays.asList()`?**

> Both create lists from elements but differ in mutability:
>
> `Arrays.asList()` — fixed size but **mutable**. You can call `set()` to
> replace elements, but `add()` and `remove()` throw `UnsupportedOperationException`.
> Also allows `null` elements:
> ```
> List<String> asList = Arrays.asList("a", "b", "c");
> asList.set(0, "x");   // ✅ allowed
> asList.add("d");      // ❌ UnsupportedOperationException
> asList = Arrays.asList("a", null, "c"); // ✅ nulls allowed
> ```
>
> `List.of()` — fully **immutable**. No `add`, `remove`, or `set`. No `null`
> elements allowed — throws `NullPointerException`:
> ```
> List<String> listOf = List.of("a", "b", "c");
> listOf.set(0, "x");   // ❌ UnsupportedOperationException
> listOf.add("d");      // ❌ UnsupportedOperationException
> List.of("a", null);   // ❌ NullPointerException
> ```
>
> Also: `Arrays.asList()` is backed by the original array — changes to the
> array reflect in the list. `List.of()` has no such backing.
>
> In production: prefer `List.of()` for constants and method return values
> where immutability is desired. Use `new ArrayList<>(List.of(...))` when
> you need a mutable copy.

---

**Q2. What is the difference between `takeWhile` and `filter` in streams?**

> `filter` — evaluates every element in the stream, keeps those matching
> the predicate. Order does not matter:
> ```
> List.of(1, 5, 2, 3).stream().filter(n -> n < 4)
> // evaluates all 4 elements → [1, 2, 3]
> ```
>
> `takeWhile` — evaluates elements in order, keeps them while predicate holds,
> **stops at the first failure**. Elements after the first failure are never seen:
> ```
> List.of(1, 5, 2, 3).stream().takeWhile(n -> n < 4)
> // evaluates 1 (pass), 5 (fail → stop) → [1]
> // never sees 2 and 3 even though they would pass
> ```
>
> Practical use: `takeWhile` is for already-sorted or naturally-ordered data
> where you want a prefix. `dropWhile` is the complement — skip the prefix,
> keep the rest. Both are meaningless (or surprising) on unordered data.
>
> Example: reading log entries ordered by time, take until a certain timestamp:
> ```
> logLines.stream()
>     .takeWhile(line -> line.timestamp.isBefore(cutoff))
>     .collect(toList());
> ```

---

**Q3. What does `Stream.ofNullable` solve? How did you handle this before Java 9?**

> `Stream.ofNullable(value)` returns a stream of one element if value is
> non-null, or an empty stream if null. It removes a common null-check pattern:
>
> Before Java 9:
> ```
> // Verbose null guard before streaming
> Stream<String> s = value == null ? Stream.empty() : Stream.of(value);
>
> // Or in flatMap — skip nulls from a list
> list.stream()
>     .flatMap(v -> v == null ? Stream.empty() : Stream.of(v))
>     .collect(toList());
> ```
>
> Java 9:
> ```
> Stream<String> s = Stream.ofNullable(value); // clean
>
> // Skip nulls
> list.stream()
>     .flatMap(Stream::ofNullable)
>     .collect(toList());
> ```
>
> Most useful in `flatMap` when mapping over a list that may contain nulls
> and you want to discard them without a separate filter step.

---

**Q4. What does `Optional.or()` do and how is it different from `orElseGet()`?**

> `orElseGet(Supplier<T>)` — returns the unwrapped value `T`. Once called,
> you are out of the `Optional` world:
> ```
> String value = optional.orElseGet(() -> "default"); // returns String
> ```
>
> `or(Supplier<Optional<T>>)` — returns another `Optional<T>`. Keeps the
> chain alive so you can continue calling Optional methods:
> ```
> Optional<String> result = findInCache(id)
>     .or(() -> findInDb(id))       // returns Optional
>     .or(() -> findInConfig(id))   // still Optional
>     .or(() -> Optional.of("default")); // still Optional
> ```
>
> The key difference: `or()` lets you chain multiple fallback sources that
> each return `Optional`, without collapsing to a raw value prematurely.
> This is the Optional equivalent of a fallback chain — try cache, then DB,
> then config, then default — each step is lazy and only executes if the
> previous returned empty.

---

**Q5. What are private methods in interfaces and why were they added in Java 9?**

> Java 8 added `default` and `static` methods to interfaces to support
> backwards-compatible API evolution. But this created a problem: multiple
> default methods that needed to share helper logic had no clean option —
> either duplicate the code or expose the helper as another `default` method
> (which pollutes the public API of every implementing class):
>
> ```
> // Java 8 — helper exposed as default (bad — now part of API)
> interface Validator {
>     default boolean isNotEmpty(String s) { ... }    // helper leaked into API
>     default boolean validateEmail(String s) { return isNotEmpty(s) && ...; }
>     default boolean validatePhone(String s) { return isNotEmpty(s) && ...; }
> }
> ```
>
> Java 9 private methods solve this:
> ```
> interface Validator {
>     // Private — shared, not in public API
>     private boolean isNotEmpty(String s) { return s != null && !s.isBlank(); }
>
>     default boolean validateEmail(String s) { return isNotEmpty(s) && s.contains("@"); }
>     default boolean validatePhone(String s) { return isNotEmpty(s) && s.matches("\\d{10}"); }
> }
> ```
>
> Two forms: `private` (instance, usable by default methods) and
> `private static` (usable by static and default methods).
> Cannot be abstract, cannot be accessed from implementing classes.

---

**Q6. What is the three-argument `Stream.iterate` and what problem does it solve?**

> Java 8's `Stream.iterate(seed, fn)` produces an infinite stream — you
> always need `limit()` to terminate it:
> ```
> // Java 8 — must remember to add limit or it runs forever
> Stream.iterate(1, n -> n * 2).limit(7).collect(toList()); // [1,2,4,8,16,32,64]
> ```
>
> Java 9 adds a predicate as the second argument — the stream terminates
> naturally when the predicate fails, like a for-loop:
> ```
> // Java 9 — self-terminating, reads like for(int n=1; n<=100; n*=2)
> Stream.iterate(1, n -> n <= 100, n -> n * 2).collect(toList());
> ```
>
> It directly mirrors a traditional for-loop:
> ```
> Stream.iterate(init, condition, update)
> for (T n = init; condition(n); n = update(n))
> ```
>
> Useful for generating sequences with a clear termination condition —
> powers of 2 up to a limit, dates up to a deadline, pagination until
> no more results.