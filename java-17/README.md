# ☕ Java 17 Features (LTS)

> Java 17 (September 2021) — LTS release, supported until 2029.
> Key theme: stabilization of all preview features from 14-16,
> strong encapsulation, new RandomGenerator API.

---

## 🧠 What Changed

- **Sealed classes** — stable (preview in 15, 16)
- **Records** — stable (preview in 14, 15)
- **Pattern matching instanceof** — stable (preview in 14, 15)
- **Strong encapsulation** — JDK internals fully locked down
- **RandomGenerator API** — unified interface for all RNG implementations
- **Spring Boot 3.x / Jakarta EE 10** — require Java 17 minimum

---

## 📄 Classes in this Module

### `Features.java`

| Example | What it covers |
|---------|----------------|
| Sealed Classes Stable | HTTP response hierarchy, sealed + records + default methods |
| Records Stable | Nested records, wither pattern, generic records, Comparable |
| Pattern Matching Stable | Guards, negation, sealed + pattern dispatch |
| Strong Encapsulation | JDK internals, --add-opens, migration impact |
| Java 17 as LTS | What stabilised, what's still preview, migration checklist |
| RandomGenerator API | RandomGenerator interface, algorithms, factory, streams |

---

## ⚡ Sealed Classes — Production Patterns

```
// Sealed interface with nested records — clean domain model
sealed interface HttpResponse permits HttpResponse.Ok,
                                       HttpResponse.ClientError,
                                       HttpResponse.ServerError {
    int statusCode();

    record Ok(int statusCode, String body)           implements HttpResponse {}
    record ClientError(int statusCode, String reason) implements HttpResponse {}
    record ServerError(int statusCode, String trace)  implements HttpResponse {}

    static HttpResponse ok(String body)           { return new Ok(200, body); }
    static HttpResponse notFound(String reason)   { return new ClientError(404, reason); }

    default boolean isSuccess() { return this instanceof Ok; }
}

// Dispatch
if (response instanceof HttpResponse.Ok ok)         { handle(ok.body()); }
if (response instanceof HttpResponse.ClientError ce) { log(ce.reason()); }
if (response instanceof HttpResponse.ServerError se) { alert(se.trace()); }
```

---

## ⚡ Records — Advanced Patterns

```
// Wither pattern — immutable update
record User(String id, String name, String email) {
    User withEmail(String newEmail) {
        return new User(id, name, newEmail); // new instance, one field changed
    }
}

// Generic record
record Pair<A, B>(A first, B second) {
    static <A, B> Pair<A, B> of(A a, B b) { return new Pair<>(a, b); }
    Pair<B, A> swap() { return new Pair<>(second, first); }
}

// Record implementing Comparable
record Version(int major, int minor, int patch) implements Comparable<Version> {
    @Override
    public int compareTo(Version other) {
        if (major != other.major) return Integer.compare(major, other.major);
        if (minor != other.minor) return Integer.compare(minor, other.minor);
        return Integer.compare(patch, other.patch);
    }
}
```

---

## ⚡ RandomGenerator API

```
// Unified interface — write against RandomGenerator, swap algorithm freely
RandomGenerator rng = RandomGenerator.getDefault();
rng.nextInt(100)     // 0-99
rng.nextDouble()     // 0.0-1.0
rng.nextBoolean()

// Specific algorithm
RandomGenerator xo  = RandomGenerator.of("Xoshiro256PlusPlus");
RandomGenerator l64 = RandomGenerator.of("L64X128MixRandom");

// Discover available algorithms
RandomGeneratorFactory.all()
    .map(RandomGeneratorFactory::name)
    .sorted()
    .forEach(System.out::println);

// Find by properties
RandomGeneratorFactory.all()
    .filter(f -> f.isJumpable() && f.stateBits() >= 128)
    .findFirst()
    .map(RandomGeneratorFactory::name);

// Stream of random values
List<Integer> randoms = rng.ints(5, 1, 100).boxed().toList();

// ThreadLocalRandom + SplittableRandom now implement RandomGenerator
RandomGenerator tlr = ThreadLocalRandom.current(); // is a RandomGenerator
```

---

## ⚡ Strong Encapsulation

```
Java 9-16:  --illegal-access=permit  (default — warns but allows)
Java 17:    --illegal-access removed — internal access always denied
```

**Impact:** Libraries using reflection to access `sun.*`, `com.sun.*`,
`jdk.internal.*` throw `InaccessibleObjectException`.

**Fix options:**
```bash
# Short-term — open specific package
--add-opens java.base/java.lang=ALL-UNNAMED

# Long-term — upgrade library to Java 17 compatible version
```

**Common affected libraries:** older cglib, byte-buddy, some Lombok versions,
older Spring versions (Spring Boot 3.x is Java 17 native).

---

## 🔑 What Stabilised in Java 17

| Feature | First Preview | Stable |
|---------|--------------|--------|
| Switch expressions | Java 12 | Java 14 |
| Text blocks | Java 13 | Java 15 |
| Records | Java 14 | Java 16 |
| Pattern instanceof | Java 14 | Java 16 |
| Sealed classes | Java 15 | Java 17 |

---

## 🔑 Java 17 Migration Checklist

```
□ Upgrade build tools — Maven 3.8+, Gradle 7.3+
□ Check all dependencies for Java 17 compatibility
□ Remove --illegal-access flags — replace with --add-opens if needed
□ Replace deprecated SecurityManager usage (removed in 17)
□ Embrace records, sealed classes, text blocks, switch expressions
□ Spring Boot 3.x requires Java 17 — upgrade path is clean
```

---