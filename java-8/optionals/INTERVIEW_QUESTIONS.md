# 🎯 Interview Questions — Optional

---

**Q1. What is `Optional` and why was it introduced?**

> `Optional<T>` is a container object that may or may not hold a non-null value.
> It was introduced in Java 8 to make the possibility of absence explicit in
> method signatures — eliminating the need for callers to guess whether a
> method might return null.
>
> Before Optional:
> ```
> User findById(String id); // might return null — caller must guess
> ```
> After Optional:
> ```
> Optional<User> findById(String id); // absence is explicit in the contract
> ```
>
> The goal is not to eliminate all nulls from the codebase — it is specifically
> for return types of methods that may not find a result. Optional signals
> "this method may not have an answer" at the API level.

---

**Q2. What is the difference between `Optional.of()` and `Optional.ofNullable()`?**

> `Optional.of(value)` — expects a non-null value. Throws `NullPointerException`
> immediately if null is passed. Use when you are certain the value is non-null:
> ```
> Optional.of("Alice");   // OK
> Optional.of(null);      // NullPointerException!
> ```
>
> `Optional.ofNullable(value)` — handles null safely. Returns `Optional.empty()`
> if null is passed, otherwise wraps the value:
> ```
> Optional.ofNullable("Alice"); // Optional[Alice]
> Optional.ofNullable(null);    // Optional.empty
> ```
>
> In practice: use `ofNullable` when wrapping values from external sources
> (DB results, map lookups, legacy APIs). Use `of` when you have already
> verified the value is non-null and want to fail fast if your assumption
> is wrong.

---

**Q3. What is the difference between `orElse()` and `orElseGet()`?**

> `orElse(default)` — the default value is **always evaluated**, even if the
> Optional is present. It takes a value, not a supplier.
>
> `orElseGet(supplier)` — the supplier is **only called if the Optional is empty**.
> Lazy evaluation.
>
> ```
> // orElse — computeExpensive() called even when user IS found
> String name = findUser("U1")
>     .map(u -> u.name)
>     .orElse(computeExpensive()); // always runs!
>
> // orElseGet — computeExpensive() only called when user NOT found
> String name = findUser("U1")
>     .map(u -> u.name)
>     .orElseGet(() -> computeExpensive()); // lazy
> ```
>
> Rule: if the default is a literal or a pre-computed value, `orElse` is fine.
> If the default requires a DB call, network request, or any non-trivial
> computation, always use `orElseGet`.

---

**Q4. When would you use `flatMap()` instead of `map()` on an Optional?**

> Use `flatMap` when the mapping function itself returns an `Optional`.
> `map` would produce `Optional<Optional<T>>` — a nested Optional that is
> awkward to work with. `flatMap` flattens it to `Optional<T>`:
>
> ```
> // extractDomain returns Optional<String>
> static Optional<String> extractDomain(String email) {
>     if (!email.contains("@")) return Optional.empty();
>     return Optional.of(email.substring(email.indexOf('@') + 1));
> }
>
> Optional<User> user = findUser("U1");
>
> // map — gives Optional<Optional<String>> — wrong
> Optional<Optional<String>> bad = user.map(u -> extractDomain(u.email));
>
> // flatMap — gives Optional<String> — correct
> Optional<String> domain = user.flatMap(u -> extractDomain(u.email));
> ```
>
> The rule is the same as `Stream.flatMap` — use it when your mapping
> function returns a wrapped type and you want the result unwrapped.

---

**Q5. What are the most common Optional anti-patterns?**

> **1. `isPresent()` + `get()` — same as a null check, defeats the purpose:**
> ```
> // BAD — this is just a null check with extra steps
> if (opt.isPresent()) {
>     process(opt.get());
> }
> // GOOD
> opt.ifPresent(this::process);
> ```
>
> **2. `Optional` as a field — not serialisable, not designed for this:**
> ```
> class User {
>     Optional<String> nickname; // BAD — Optional doesn't implement Serializable
> }
> // GOOD — use null for optional fields, Optional only for return types
> ```
>
> **3. `Optional` as a method parameter — forces caller to wrap:**
> ```
> void send(Optional<String> email) { ... } // BAD
> // GOOD — use overloading or nullable parameter
> void send(String email) { ... }
> void send() { ... } // overload for absent case
> ```
>
> **4. `orElse` with expensive computation:**
> ```
> user.orElse(loadFromDatabase()); // loadFromDatabase() always called
> user.orElseGet(() -> loadFromDatabase()); // only called if empty
> ```
>
> **5. Wrapping a collection in Optional:**
> ```
> Optional<List<User>> users = findAll(); // BAD
> List<User> users = findAll();           // GOOD — return empty list instead
> ```

---

**Q6. How do you use `Optional` in a stream pipeline to unwrap a list of Optionals?**

> Pre-Java 9 — filter then get:
> ```
> List<Optional<User>> optionals = ids.stream()
>     .map(this::findUser)
>     .collect(Collectors.toList());
>
> List<User> users = optionals.stream()
>     .filter(Optional::isPresent)
>     .map(Optional::get)
>     .collect(Collectors.toList());
> ```
>
> Java 9+ — `Optional.stream()` + `flatMap` (cleanest):
> ```
> List<User> users = ids.stream()
>     .map(this::findUser)       // Stream<Optional<User>>
>     .flatMap(Optional::stream) // unwrap — only present values pass through
>     .collect(Collectors.toList());
> ```
>
> `Optional.stream()` returns a Stream of one element if present, or an
> empty Stream if absent. `flatMap` then flattens all of those into a
> single Stream containing only the present values.

---

**Q7. How would you chain multiple Optional operations in a service layer without null checks?**

> ```
> // Service layer — no null checks anywhere
> String greeting = findUser(userId)          // Optional<User>
>     .filter(u -> u.isActive())              // only active users
>     .map(u -> u.getEmail())                 // Optional<String>
>     .flatMap(email -> extractDomain(email)) // Optional<String>
>     .map(domain -> "Welcome from " + domain)
>     .orElse("Welcome, guest");              // terminal — always a String
> ```
>
> Each step either transforms the value or short-circuits to empty if any
> step produces empty. The final `orElse` ensures a non-null result is
> always returned to the caller. The entire chain has zero explicit null
> checks and zero `if` statements.
>
> This is the main value of Optional in a service layer — replacing nested
> null checks with a readable, linear chain of operations.

---

**Q8. What does `or()` do (Java 9+) and how is it different from `orElse()`?**

> `or(supplier)` returns the original Optional if present, or supplies a
> fallback **Optional** if empty. The result is always `Optional<T>`.
>
> `orElse(value)` returns the unwrapped value or a default value. The result
> is always `T`.
>
> ```
> // or — stays in Optional world, can chain further
> Optional<User> user = findInCache(id)
>     .or(() -> findInDatabase(id))  // try DB if cache misses
>     .or(() -> findInBackup(id));   // try backup if DB misses
>
> // orElse — exits Optional world, gives you a plain value
> User user = findInCache(id).orElse(defaultUser);
> ```
>
> Use `or()` when you have multiple fallback sources and want to keep
> chaining Optional operations. Use `orElse`/`orElseGet` when you are
> ready to unwrap the final value.

---

**Q9. Why should `Optional` not be used as a method parameter?**

> Using Optional as a parameter forces every caller to wrap their value,
> even when they already have a non-null value:
> ```
> // BAD — caller forced to wrap
> void sendEmail(Optional<String> address) { ... }
> sendEmail(Optional.of("alice@co.com")); // unnecessary wrapping
> sendEmail(Optional.empty());            // unnecessarily verbose
>
> // GOOD — use overloading or @Nullable
> void sendEmail(String address) { ... }     // required
> void sendEmailIfPresent(String address) { if (address != null) send(address); }
> ```
>
> Additionally, Optional as a parameter doesn't prevent the caller from
> passing `null` (instead of `Optional.empty()`) — so it doesn't actually
> solve the null-safety problem it appears to.
>
> The API designers of Optional explicitly stated it was designed for
> return types only. The Javadoc says: "Optional is primarily intended for
> use as a method return type."

---

**Q10. How does `Optional` interact with serialisation?**

> `Optional` does not implement `Serializable`. If a class has an Optional
> field and is serialised (e.g. sent over the network, stored in an HTTP
> session, used in a distributed cache), it will throw `NotSerializableException`.
>
> This is one of the primary reasons Optional should not be used as a field:
> ```
> class UserDTO implements Serializable {
>     Optional<String> nickname; // NotSerializableException at runtime!
> }
>
> // Fix — use nullable field, expose Optional only in the getter
> class UserDTO implements Serializable {
>     private String nickname; // nullable field — serialisable
>     public Optional<String> getNickname() {
>         return Optional.ofNullable(nickname); // Optional only at API boundary
>     }
> }
> ```
>
> This pattern — nullable field internally, Optional return type externally —
> is the standard approach in production code that needs both null-safety
> and serialisability.