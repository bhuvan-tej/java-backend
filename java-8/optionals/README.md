# 🎁 Optional

> `Optional<T>` is a container that may or may not hold a value.
> Its purpose: make absence explicit in the API — no more silent nulls.

---

## 🧠 Mental Model

```
Optional<User>
  ├── present → holds a User value
  └── empty   → holds nothing (not null — absent)

Think of it as a box:
  Optional.of("Alice")       → sealed box with "Alice" inside
  Optional.empty()           → empty sealed box
  Optional.ofNullable(x)     → seal whatever x is (null → empty box)

NOT a replacement for null everywhere.
ONLY for return types of methods that may not find a result.
```

---

## 📄 Classes in this Module

### `OptionalSamples.java`

| Example         | What it covers |
|-----------------|----------------|
| Creating        | `of`, `ofNullable`, `empty`, `isEmpty` |
| Retrieving      | `orElse`, `orElseGet`, `orElseThrow`, `get` |
| Conditional ops | `ifPresent`, `ifPresentOrElse`, `filter`, `map`, `flatMap`, `or` |
| Anti-patterns   | `isPresent+get`, `orElse` with expensive call, `Optional.of(null)` |
| Adv level       | Chaining, service layer pattern, `Optional::stream` in pipelines |

---

## ⚡ Key Methods

```
// ── Creating ──────────────────────────────────────────────────
Optional.of(value)              // non-null value — NPE if null passed
Optional.ofNullable(value)      // safe — null becomes Optional.empty()
Optional.empty()                // explicitly absent

// ── Checking ──────────────────────────────────────────────────
opt.isPresent()                 // true if value exists
opt.isEmpty()                   // true if empty (Java 11+)

// ── Retrieving ────────────────────────────────────────────────
opt.get()                       // value or NoSuchElementException — avoid
opt.orElse("default")           // value or default — default ALWAYS evaluated
opt.orElseGet(() -> compute())  // value or supplier — supplier only if absent
opt.orElseThrow()               // value or NoSuchElementException (Java 10+)
opt.orElseThrow(Ex::new)        // value or custom exception

// ── Transforming ──────────────────────────────────────────────
opt.map(u -> u.name)            // transform value, stay Optional
opt.flatMap(u -> findEmail(u))  // when mapping returns Optional — avoids Optional<Optional<T>>
opt.filter(u -> u.active)       // keep value only if predicate passes

// ── Side effects ──────────────────────────────────────────────
opt.ifPresent(u -> log(u))      // run only if present
opt.ifPresentOrElse(            // run one of two actions (Java 9+)
    u -> log(u),
    () -> logMissing())
opt.or(() -> findFallback())    // supply fallback Optional if empty (Java 9+)

// ── Stream integration ────────────────────────────────────────
opt.stream()                    // Optional → Stream of 0 or 1 elements (Java 9+)
```

---

## 🔑 orElse vs orElseGet

```
// orElse — default is ALWAYS evaluated, even if Optional is present
String name = findUser("U1")
    .map(u -> u.name)
    .orElse(computeExpensive()); // computeExpensive() runs even when user found!

// orElseGet — supplier only runs if Optional is empty
String name = findUser("U1")
    .map(u -> u.name)
    .orElseGet(() -> computeExpensive()); // only runs if user not found

// Rule: if default is a literal or already-computed value → orElse
//       if default requires computation                  → orElseGet
```

---

## 🔑 map vs flatMap

```
// map — use when transformation returns a plain value
Optional<String> email = user.map(u -> u.email); // String, not Optional<String>

// flatMap — use when transformation returns Optional (avoids Optional<Optional<T>>)
Optional<String> domain = user.flatMap(u -> extractDomain(u.email));
// extractDomain returns Optional<String> — flatMap unwraps it

// ❌ map when result is Optional — gives Optional<Optional<String>>
Optional<Optional<String>> bad = user.map(u -> extractDomain(u.email));
```

---

## ✅ DO / ❌ DON'T

```
// ✅ Return type for methods that may not find a result
Optional<User> findById(String id) { ... }

// ❌ Field in a class — use null or a sentinel instead
class User {
    Optional<String> nickname; // don't do this
}

// ❌ Method parameter — use overloading or null instead
void process(Optional<String> name) { ... } // don't do this

// ❌ Collection element
List<Optional<User>> users; // don't do this — just filter nulls

// ❌ isPresent + get — same as null check, defeats Optional
if (opt.isPresent()) opt.get().doSomething();

// ✅ ifPresent, map, orElse instead
opt.ifPresent(u -> u.doSomething());
```

---