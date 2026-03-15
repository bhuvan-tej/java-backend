# 🎯 Interview Questions — Java 17

---

**Q1. Java 17 is an LTS release. What exactly does that mean and why does it matter?**

> LTS (Long Term Support) means Oracle and other vendors provide extended
> updates and security patches — Java 17 is supported until September 2029.
> Non-LTS releases (18, 19, 20) receive updates only until the next release,
> roughly 6 months.
>
> Why it matters in practice:
> - Enterprises adopt LTS releases — 8, 11, 17, 21 — and skip non-LTS
> - Spring Boot 3.x requires Java 17 minimum — the single biggest driver of migration
> - Jakarta EE 10 requires Java 17
> - Most cloud providers, Docker base images, and CI pipelines standardize on LTS
>
> Java 17 is the current enterprise standard for new projects (along with 21).
> If you're starting a new Spring Boot application today, Java 17 is the minimum.

---

**Q2. What is the wither pattern for records and why is it needed?**

> Records are immutable — there are no setters. When you need to create a
> modified copy of a record, you use the wither pattern — a method that
> returns a new record instance with one field changed:
>
> ```
> record User(String id, String name, String email) {
>     User withEmail(String newEmail) {
>         return new User(id, name, newEmail); // copy with email changed
>     }
>     User withName(String newName) {
>         return new User(id, newName, email);
>     }
> }
>
> var user    = new User("U1", "Alice", "old@example.com");
> var updated = user.withEmail("new@example.com"); // original unchanged
> ```
>
> This is the record equivalent of a builder's `set` method. It preserves
> immutability — the original record is never modified. The pattern is
> common in functional programming and domain-driven design where value
> objects must be immutable but need to represent state transitions.
>
> Java does not generate withers automatically — you write them manually.
> Lombok's `@With` annotation generates them. Future Java versions may
> add a `with` keyword for records.

---

**Q3. What is the RandomGenerator API introduced in Java 17?**

> Java 17 introduced `java.util.random.RandomGenerator` — a unified
> interface that all random number generators now implement. Before this,
> `Random`, `ThreadLocalRandom`, `SplittableRandom`, and `SecureRandom`
> were unrelated classes with inconsistent APIs.
>
> ```
> // Write code against the interface — algorithm is swappable
> RandomGenerator rng = RandomGenerator.getDefault();
> rng.nextInt(100);
> rng.nextDouble();
> rng.ints(10, 0, 100).boxed().toList();
>
> // Specific algorithm
> RandomGenerator xo = RandomGenerator.of("Xoshiro256PlusPlus");
>
> // Discover algorithms by properties
> RandomGeneratorFactory.all()
>     .filter(f -> f.isJumpable() && f.stateBits() >= 128)
>     .findFirst();
> ```
>
> Key benefits:
> - `ThreadLocalRandom` and `SplittableRandom` now implement `RandomGenerator`
> - New high-quality algorithms: `L64X128MixRandom`, `Xoshiro256PlusPlus`
> - `RandomGeneratorFactory` lets you discover and select algorithms programmatically
> - Consistent stream API — `ints()`, `longs()`, `doubles()` on all implementations

---

**Q4. What is strong encapsulation in Java 17 and what broke when migrating from Java 11?**

> Java 9 introduced the module system and began restricting access to JDK
> internal APIs. But to ease migration, `--illegal-access=permit` was the
> default — access was allowed with a warning.
>
> Java 17 removed the `--illegal-access` flag entirely. Internal APIs are
> now always inaccessible. Libraries that used reflection to access `sun.*`,
> `com.sun.*`, or `jdk.internal.*` classes throw `InaccessibleObjectException`.
>
> Common things that broke:
> - Older cglib and byte-buddy (used by Spring, Hibernate for proxying)
> - Older Lombok versions
> - Some serialisation frameworks
> - Direct use of `sun.misc.Unsafe`
>
> Fixes:
> ```bash
> # Per-package workaround
> --add-opens java.base/java.lang=ALL-UNNAMED
>
> # Permanent fix — upgrade to Java 17 compatible library versions
> # Spring Boot 3.x is fully Java 17 native — no --add-opens needed
> ```
>
> This is the most common migration pain point from Java 11 to 17 in
> large codebases with many transitive dependencies.

---

**Q5. What is the difference between `sealed` and `final` for restricting class hierarchies?**

> `final` — prevents ALL extension. No class can extend a `final` class:
> ```
> final class Config { ... }
> class AppConfig extends Config { } // compile error — always
> ```
>
> `sealed` — controls WHO can extend. Only listed permitted subtypes can extend:
> ```
> sealed class Shape permits Circle, Rectangle {}
> class Triangle extends Shape {} // compile error — not in permits
> class Circle extends Shape {}   // ✅ — in permits list
> ```
>
> Key differences:
> - `final` is all-or-nothing — no extension at all
> - `sealed` is selective — a fixed, known set of subtypes
> - `sealed` enables exhaustive pattern matching — compiler knows all variants
> - Permitted subtypes can themselves be `final`, `sealed`, or `non-sealed`
> - `sealed` expresses domain intent — "these are all the shapes that exist"
>
> Use `final` when a class should never be extended.
> Use `sealed` when extension is valid but must be controlled — domain
> hierarchies, ADTs, error types, state machines.

---

**Q6. How does Java 17 compare to Java 11 as a migration target? Which should you choose for a new project?**

> Java 11 — previous LTS, widely adopted, supported until 2026.
> Still common in existing codebases but now dated for new projects.
>
> Java 17 — current mainstream LTS, supported until 2029.
> Has everything Java 11 has plus:
> - Records, sealed classes, text blocks, switch expressions — all stable
> - Better performance (improved GC, JIT improvements across 12-17)
> - Strong encapsulation — better security posture
> - Required by Spring Boot 3.x and Jakarta EE 10
>
> For a new project today: **Java 17 is the minimum, Java 21 is preferred**.
>
> Java 21 (next LTS) adds virtual threads — a significant improvement for
> I/O-bound applications. If your stack supports it (Spring Boot 3.2+),
> start on Java 21. If you need a more conservative choice, Java 17 is
> the safe, well-supported baseline.
>
> Choosing Java 11 for a new project in 2024+ is only justified if you
> have a specific constraint — legacy cloud provider, corporate policy,
> or a dependency that hasn't caught up.