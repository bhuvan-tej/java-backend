# ☕ Java 15-16 Features

> Java 15 (September 2020), Java 16 (March 2021) — both non-LTS.
> Key theme: Records and Sealed Classes stable, pattern matching stable,
> Stream.toList() convenience.

---

## 🧠 What Changed

- **Records** — immutable data carriers, auto-generated boilerplate (stable Java 16)
- **Sealed Classes** — restricted class hierarchies, exhaustive subtyping (preview)
- **Stream.toList()** — shorthand for unmodifiable list (Java 16)
- **String.formatted()** — instance version of String.format (Java 15)
- **Pattern matching instanceof** — stable in Java 16

---

## 📄 Classes in this Module

### `Records.java`

| Example | What it covers |
|---------|----------------|
| Record Basics | Declaration, accessors, equals, hashCode, toString, immutability |
| Compact Constructor | Validation, normalisation, param reassignment |
| Custom Methods | Instance methods, static methods, custom accessors |
| Records with Interfaces | implements, polymorphism, stream operations |
| Records as DTOs | DTOs, value objects, Money, map keys |

### `SealedClasses.java`

| Example | What it covers |
|---------|----------------|
| Sealed Basics | sealed, permits, final, non-sealed subtypes |
| Sealed Interfaces | Interface hierarchy, algebraic data types |
| With Pattern Matching | Exhaustive dispatch, instanceof on sealed types |
| Result Type | Type-safe error handling without exceptions |
| Payment Domain | Real-world sealed hierarchy, fee calculation |

### `MiscFeatures.java`

| Example | What it covers |
|---------|----------------|
| Stream.toList() | Unmodifiable, vs collect(toList()), null behaviour |
| String.formatted() | Instance format, with text blocks, chaining |
| Pattern Matching Stable | Negation pattern, in equals(), Java 16 stable |
| Records + Pattern Matching | Sealed + records + pattern = algebraic types |

---

## ⚡ Records

```
// Declaration — compiler generates constructor, accessors, equals, hashCode, toString
record Point(int x, int y) {}

Point p = new Point(3, 4);
p.x()           // accessor — component name, not getX()
p.y()
p.toString()    // Point[x=3, y=4]
p.equals(...)   // value-based — compares all components
p.hashCode()    // consistent with equals

// Compact constructor — validation and normalisation
record Person(String name, int age) {
    Person {
        name = name.strip();                              // normalise
        if (age < 0) throw new IllegalArgumentException("negative age");
        // this.name = name assigned automatically after block
    }
}

// Custom methods allowed
record Circle(double radius) {
    double area() { return Math.PI * radius * radius; }
    static Circle unit() { return new Circle(1.0); }
}

// Records can implement interfaces
record Rectangle(double w, double h) implements Shape {
    @Override public double area() { return w * h; }
}
```

**What records CANNOT do:**
- Add instance fields beyond components
- Extend any class (implicitly extends `Record`)
- Be abstract

**What records CAN do:**
- Implement interfaces
- Add instance and static methods
- Override generated methods
- Have static fields and initialisers

---

## ⚡ Sealed Classes

```
// sealed + permits — only listed classes can extend
sealed abstract class Shape permits Circle, Rectangle, Triangle {}

// Permitted subtypes must be final, sealed, or non-sealed
final class Circle    extends Shape { ... } // no further extension
final class Rectangle extends Shape { ... }
non-sealed class Triangle extends Shape { ... } // anyone can extend Triangle

// Sealed interfaces
sealed interface Result<T> permits Result.Success, Result.Failure {
    record Success<T>(T value)      implements Result<T> {}
    record Failure<T>(String error) implements Result<T> {}
}

// Rules
// 1. Permitted subclass must be in same package or module
// 2. Must directly extend/implement the sealed type
// 3. Must be final, sealed, or non-sealed
```

---

## ⚡ Stream.toList()

```
// Java 16 — shorthand, returns unmodifiable list
List<String> result = stream.filter(...).map(...).toList();

// vs collect(Collectors.toList()) — returns mutable list
List<String> mutable = stream.collect(Collectors.toList());

// vs collect(toUnmodifiableList()) — unmodifiable, throws on null
List<String> immutable = stream.collect(Collectors.toUnmodifiableList());

// Key difference: toList() allows nulls, toUnmodifiableList() throws NPE on nulls
```

| Method | Modifiable | Nulls allowed |
|--------|-----------|---------------|
| `collect(toList())` | ✅ | ✅ |
| `collect(toUnmodifiableList())` | ❌ | ❌ |
| `toList()` | ❌ | ✅ |

---

## 🔑 Records — Common Mistakes

```
// ❌ Trying to add instance fields
record Point(int x, int y) {
    int z; // compile error — only components allowed as instance fields
}

// ❌ Expecting setters
Point p = new Point(1, 2);
p.x = 5; // compile error — fields are final

// ❌ Wrong accessor name
p.getX(); // compile error — accessor is p.x(), not getX()

// ❌ Forgetting compact constructor assigns automatically
record Name(String value) {
    Name {
        value = value.strip();
        this.value = value; // redundant — assigned automatically after block
    }
}
```

---

## 🔑 Sealed Classes — Common Mistakes

```
// ❌ Permitted subclass in different package (without modules)
// package com.other;
// class Triangle extends Shape {} // compile error

// ❌ Forgetting to mark permitted subclass
sealed class Shape permits Circle {}
class Rectangle extends Shape {} // compile error — not in permits list

// ❌ Non-exhaustive pattern match — no compiler error yet (Java 17+ switch gets this)
if (shape instanceof Circle c) { ... }
// missing Rectangle case — silent bug in Java 15-16
// Java 21 pattern switch enforces exhaustiveness
```

---