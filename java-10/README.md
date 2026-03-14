# ☕ Java 10 Features

> Java 10 (March 2018) — not an LTS release.
> Key theme: local variable type inference with `var`.

---

## 🧠 What Changed

- **`var`** — local variable type inference, compiler resolves type from RHS
- **`Optional.orElseThrow()`** — no-arg version, cleaner than `get()`
- **`Collectors.toUnmodifiableList/Set/Map`** — stream directly to immutable collection
- **`List/Set/Map.copyOf`** — defensive immutable copy of existing collection

---

## 📄 Classes in this Module

### `Java10Features.java`

| Example | What it covers |
|---------|----------------|
| var basics | Type inference, complex generics, concrete type inference |
| var in loops | Enhanced for, traditional for, streams, Map.Entry |
| var rules and limits | Where it works, where it doesn't, static typing |
| Optional.orElseThrow | No-arg version, vs get(), NoSuchElementException |
| Unmodifiable collectors | toUnmodifiableList/Set/Map, copyOf, defensive copies |

---

## ⚡ var

```
// Before Java 10 — type repeated on both sides
String name = "Alice";
Map<String, List<Integer>> grouped = new HashMap<String, List<Integer>>();

// Java 10 — type inferred from RHS
var name    = "Alice";                        // String
var grouped = new HashMap<String, List<Integer>>(); // biggest win — complex generics
var entry   = map.entrySet().iterator().next(); // Map.Entry<K,V> — no verbose type needed

// for loops
for (var i = 0; i < 10; i++) { ... }          // int
for (var item : list) { ... }                 // element type of list
```

**var is still statically typed — the type is resolved at compile time, not runtime.**

---

## ⚡ var — Where it works and where it doesn't

```
// ✅ Local variables with initialiser
var list = new ArrayList<String>();

// ✅ for loop index and enhanced for
for (var i = 0; i < 10; i++) {}
for (var item : collection) {}

// ✅ try-with-resources
try (var reader = new BufferedReader(...)) {}

// ❌ Fields — var is local variables only
// private var count = 0;

// ❌ Method parameters
// void process(var input) {}

// ❌ Return types
// var getResult() {}

// ❌ No initialiser — nothing to infer from
// var x;

// ❌ Null initialiser — null has no type
// var obj = null;

// ❌ Array shorthand
// var arr = {1, 2, 3}; — use new int[]{1, 2, 3}
```

**var infers the concrete type, not the interface:**
```
var list = new ArrayList<String>(); // inferred as ArrayList, not List
list.trimToSize();                  // ArrayList-specific method — compiles fine
```

---

## ⚡ Optional.orElseThrow()

```
// Java 8 — get() is bad practice, orElseThrow requires supplier
optional.get();                                          // discouraged
optional.orElseThrow(() -> new RuntimeException("...")) // verbose

// Java 10 — no-arg, throws NoSuchElementException
optional.orElseThrow(); // clean, explicit intent — "this must be present"

// Rule: never use get() — always use orElseThrow() or orElse/orElseGet
```

---

## ⚡ Unmodifiable Collectors

```
// Java 8 — collect to mutable list
var mutable = stream.collect(Collectors.toList()); // mutable

// Java 10 — collect directly to immutable
var immutable = stream.collect(Collectors.toUnmodifiableList());
var immutableSet = stream.collect(Collectors.toUnmodifiableSet());
var immutableMap = stream.collect(Collectors.toUnmodifiableMap(k -> k, v -> v));

// copyOf — immutable defensive copy
var copy = List.copyOf(existingList); // immutable, changes to source don't affect copy

// Optimisation — copyOf returns same instance if already immutable
var original = List.of("a", "b");
var copy2    = List.copyOf(original);
original == copy2; // true — no copy made
```

---

## 🔑 Common Mistakes

```
// ❌ Thinking var is dynamic like JavaScript
var x = 42;
x = "hello"; // compile error — x is int, not dynamic

// ❌ Using var when it hurts readability
var x = compute(); // what type is x? unclear — defeats the purpose
// ✅ Use var when type is obvious from RHS
var list = new ArrayList<String>(); // clearly ArrayList<String>

// ❌ Still using get() instead of orElseThrow()
optional.get(); // throws NoSuchElementException with no context
// ✅
optional.orElseThrow(); // same but expressive

// ❌ Expecting copyOf to stay in sync with source
var source = new ArrayList<>(List.of("a", "b"));
var copy   = List.copyOf(source);
source.add("c");
copy.contains("c"); // false — copy is independent snapshot
```

---