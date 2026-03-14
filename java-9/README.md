# ☕ Java 9 Features

> Java 9 (September 2017) — not an LTS release.
> Key theme: convenience APIs, Stream/Optional improvements, interface polish.

---

## 🧠 What Changed

- **Collection factory methods** — `List.of()`, `Set.of()`, `Map.of()` — concise immutable collections
- **Stream additions** — `takeWhile`, `dropWhile`, `iterate` with predicate, `ofNullable`
- **Optional additions** — `ifPresentOrElse`, `or`, `stream`
- **Interface private methods** — shared helper logic without polluting the public API
- **Process API** — `ProcessHandle` for inspecting and managing OS processes

---

## 📄 Classes in this Module

### `Java9Features.java`

| Example | What it covers |
|---------|----------------|
| Collection Factory Methods | List.of, Set.of, Map.of, Map.ofEntries, copyOf, immutability rules |
| Stream Additions | takeWhile, dropWhile, iterate with predicate, ofNullable |
| Optional Additions | ifPresentOrElse, or, Optional.stream |
| Interface Private Methods | private and private static methods in interfaces |
| Process API | ProcessHandle, pid, info, allProcesses, onExit |

---

## ⚡ Collection Factory Methods

```java
// Immutable List
List<String> names = List.of("Alice", "Bob", "Carol");

// Immutable Set — no duplicates, no nulls
Set<String> roles = Set.of("ADMIN", "USER", "VIEWER");

// Immutable Map — up to 10 entries
Map<String, Integer> scores = Map.of("Alice", 95, "Bob", 87);

// Immutable Map — more than 10 entries
Map<String, Integer> more = Map.ofEntries(
    Map.entry("Alice", 95),
    Map.entry("Bob",   87)
);

// Copy existing collection into immutable
List<String> immutable = List.copyOf(mutableList);
```

**Rules:**
- `add` / `remove` / `set` → `UnsupportedOperationException`
- `null` elements → `NullPointerException`
- `Set.of` / `Map.of` — insertion order not guaranteed
- `Map.of` duplicate keys → `IllegalArgumentException` at runtime

**Before Java 9:**
```
// Verbose and mutable
List<String> old = Collections.unmodifiableList(Arrays.asList("a", "b", "c"));
```

---

## ⚡ Stream Additions

```
// takeWhile — take elements while predicate holds, stop at first failure
List.of(1,2,3,4,5,6).stream()
    .takeWhile(n -> n < 4)     // [1, 2, 3] — stops when 4 fails
    .collect(toList());

// dropWhile — skip elements while predicate holds, keep rest
List.of(1,2,3,4,5,6).stream()
    .dropWhile(n -> n < 4)     // [4, 5, 6] — drops until 4
    .collect(toList());

// takeWhile vs filter — ORDER MATTERS for takeWhile
List.of(1,3,5,2,4).stream()
    .takeWhile(n -> n < 5)     // [1, 3] — stops at 5, never sees 2,4
    .collect(toList());
// filter(n -> n < 5) would give [1, 3, 2, 4]

// iterate with predicate — self-terminating, no limit() needed
Stream.iterate(1, n -> n <= 100, n -> n * 2)  // [1, 2, 4, 8, 16, 32, 64]
    .collect(toList());

// ofNullable — 0 or 1 element stream, avoids null check
Stream.ofNullable(null).count();   // 0
Stream.ofNullable("Alice").count(); // 1

// flatMap with ofNullable — remove nulls from list
list.stream()
    .flatMap(Stream::ofNullable)   // nulls become empty streams
    .collect(toList());
```

---

## ⚡ Optional Additions

```
// ifPresentOrElse — value action + empty action
optional.ifPresentOrElse(
    v  -> System.out.println("found: " + v),
    () -> System.out.println("not found")
);

// or — return alternative Optional if empty (keeps chain in Optional)
Optional<String> result = findInCache(id)
    .or(() -> findInDb(id))
    .or(() -> Optional.of("guest"));
// orElse/orElseGet return T — or() returns Optional<T>

// Optional.stream — bridge into Stream pipeline
List<Optional<String>> optionals = ...;
List<String> values = optionals.stream()
    .flatMap(Optional::stream)  // empty → 0 elements, present → 1 element
    .collect(toList());
```

---

## ⚡ Interface Private Methods

```
interface Validator {
    boolean validate(String input);

    // Private — shared helper, NOT part of public API
    private boolean isNotEmpty(String input) {
        return input != null && !input.isBlank();
    }

    // Private static — shared static helper
    private static String sanitise(String input) {
        return input == null ? "" : input.trim();
    }

    // Default method using private helper
    default boolean validateWithLog(String input) {
        if (!isNotEmpty(input)) {
            System.out.println("empty input");
            return false;
        }
        return validate(input);
    }
}
```

**Why:** Java 8 default methods had no way to share logic — either duplicate
code across defaults or expose a helper as a public default (pollutes API).
Private methods solve this cleanly.

---

## 🔑 Common Mistakes

```
// ❌ Trying to add to List.of
List<String> list = List.of("a", "b");
list.add("c"); // UnsupportedOperationException

// ❌ Null in factory collections
List.of("a", null, "b"); // NullPointerException — use ArrayList for nullable

// ❌ Duplicate keys in Map.of
Map.of("a", 1, "a", 2); // IllegalArgumentException at runtime

// ❌ Using takeWhile like filter on unordered data
List.of(5, 1, 2, 3).stream()
    .takeWhile(n -> n < 5) // [] — first element 5 fails immediately!
// Use filter() when order doesn't matter

// ❌ Confusing or() with orElse()
optional.orElse("default");           // returns String
optional.or(() -> Optional.of("x"));  // returns Optional<String>
```

---