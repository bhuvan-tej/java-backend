# 🎯 Interview Questions — Java 15-16

---

**Q1. What is a Record in Java? What does the compiler generate automatically?**

> A record is a concise immutable data carrier. You declare the components
> and the compiler generates everything else:
>
> ```
> record Point(int x, int y) {}
> ```
>
> Generated automatically:
> - **Private final fields** for each component
> - **Canonical constructor** with all components as parameters
> - **Accessors** named after components — `x()`, `y()` (not `getX()`)
> - **`equals()`** — value-based, compares all components
> - **`hashCode()`** — consistent with equals
> - **`toString()`** — `Point[x=3, y=4]`
>
> Records implicitly extend `java.lang.Record` — they cannot extend any
> other class. They can implement interfaces, add methods, and have static
> members, but cannot add instance fields beyond their components.

---

**Q2. What is a compact constructor and when would you use it?**

> A compact constructor is a special constructor syntax for records that
> omits the parameter list. The components are implicitly available and
> assigned automatically at the end of the block — you don't need to
> write `this.name = name`:
>
> ```
> record Person(String name, int age) {
>     Person {                          // no parameter list
>         name = name.strip();          // can reassign params before auto-assignment
>         if (age < 0) throw new IllegalArgumentException("negative age");
>         // this.name = name; happens automatically after this block
>     }
> }
> ```
>
> Use compact constructors for:
> - **Validation** — throw if invariants are violated
> - **Normalisation** — strip whitespace, uppercase, round values
>
> Important: you can reassign the parameter variables inside the compact
> constructor (e.g. `name = name.strip()`) and the modified value is what
> gets assigned to the field. You cannot assign to `this.name` directly.

---

**Q3. What is the difference between a Record and a regular immutable class?**

> Both can represent immutable data but differ in verbosity, semantics,
> and constraints:
>
> **Regular immutable class** — full control, lots of boilerplate:
> ```
> public final class Point {
>     private final int x, y;
>     public Point(int x, int y) { this.x = x; this.y = y; }
>     public int x() { return x; }
>     public int y() { return y; }
>     @Override public boolean equals(Object o) { ... } // manual
>     @Override public int hashCode() { ... }           // manual
>     @Override public String toString() { ... }         // manual
> }
> ```
>
> **Record** — compiler does it all, but with constraints:
> ```
> record Point(int x, int y) {}
> ```
>
> Key differences:
> - Records cannot add instance fields — only components
> - Records cannot extend classes — implicitly extend `Record`
> - Regular class `equals()` can be customised any way — record `equals()` is always component-based
> - Records are transparent — all state is exposed via accessors
>
> Use records for: DTOs, value objects, tuple-like returns, map keys.
> Use regular classes when: you need non-component fields, custom
> equality, or class inheritance.

---

**Q4. What is a sealed class and what problem does it solve?**

> A sealed class restricts which classes can extend it, using `sealed`
> and `permits` keywords:
>
> ```
> sealed abstract class Shape permits Circle, Rectangle, Triangle {}
> ```
>
> The problem it solves: before sealed classes, any class in any package
> could extend `Shape`. This made it impossible to reason exhaustively
> about all possible subtypes — you could never be sure you'd handled
> every case. Libraries had to use `final` (prevent all extension) or
> accept unknown subtypes.
>
> With sealed classes:
> - The compiler knows every permitted subtype at compile time
> - Pattern matching can be exhaustive — no default needed for enums-like dispatch
> - Adding a new subtype requires updating the `permits` list — intentional breaking change
> - Enables algebraic data types — sum types with a fixed set of variants
>
> Permitted subtypes must be `final` (no extension), `sealed` (controlled
> extension), or `non-sealed` (open to anyone). All must be in the same
> package or module.

---

**Q5. What is the difference between `Stream.toList()` and `collect(Collectors.toList())`?**

> Three key differences:
>
> **Mutability:**
> ```
> stream.toList()                          // unmodifiable — add/remove throws
> stream.collect(Collectors.toList())      // mutable — backed by ArrayList
> stream.collect(toUnmodifiableList())     // unmodifiable
> ```
>
> **Null handling:**
> ```
> // toList() — nulls preserved
> Stream.of("a", null, "b").toList();       // ["a", null, "b"] ✓
>
> // toUnmodifiableList() — throws on null
> Stream.of("a", null).collect(toUnmodifiableList()); // NullPointerException
> ```
>
> **Verbosity:**
> ```
> .collect(Collectors.toList()) // requires Collectors import
> .toList()                     // no import, shorter
> ```
>
> In production: use `toList()` when you want a read-only result and may
> have nulls. Use `collect(toUnmodifiableList())` when nulls should be
> rejected. Use `collect(toList())` only when you genuinely need to mutate
> the result after collection.

---

**Q6. How do sealed classes work with pattern matching? Why is this combination powerful?**

> Sealed classes give the compiler complete knowledge of all subtypes.
> Pattern matching uses this to enable exhaustive dispatch without a
> default case:
>
> ```
> sealed interface Shape permits Circle, Rectangle, Triangle {}
>
> double area(Shape s) {
>     if (s instanceof Circle c)    return Math.PI * c.radius() * c.radius();
>     if (s instanceof Rectangle r) return r.width() * r.height();
>     if (s instanceof Triangle t)  return 0.5 * t.base() * t.height();
>     throw new AssertionError("unreachable");
>     // Java 21 pattern switch: no throw needed — compiler verifies exhaustiveness
> }
> ```
>
> The power: if you add a new permitted subtype (`Square`), every
> if-instanceof chain in your codebase that handles `Shape` becomes
> incomplete — a clear signal to update them. This is similar to adding
> a new enum value and getting compile warnings on non-exhaustive switches.
>
> Combined with records:
> ```
> sealed interface Event permits UserCreated, OrderPlaced {}
> record UserCreated(String id, String name) implements Event {}
> record OrderPlaced(String id, double amount) implements Event {}
> ```
>
> This is Java's answer to algebraic data types — a closed set of
> variants, each carrying typed data, with exhaustive pattern dispatch.
> Common in domain modeling, error types, state machines, and ASTs.