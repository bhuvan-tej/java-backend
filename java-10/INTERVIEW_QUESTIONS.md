# 🎯 Interview Questions — Java 10

---

**Q1. What is `var` in Java 10? Is it dynamically typed?**

> `var` is local variable type inference. The compiler infers the type
> from the right-hand side at compile time. It is NOT dynamic — the type
> is resolved once and locked in, exactly like an explicit declaration:
>
> ```
> var name = "Alice"; // compiler sees String on RHS → name is String
> name = 42;          // compile error — name is String, not int
> ```
>
> `var` is purely a compile-time convenience. The bytecode is identical
> to an explicit type declaration — no runtime overhead, no reflection,
> no dynamic dispatch. It is syntactic sugar that removes redundancy when
> the type is already obvious from the RHS.

---

**Q2. Where can `var` be used and where is it not allowed?**

> `var` is restricted to **local variables with an initialiser**:
>
> ```
> var list = new ArrayList<String>(); // ✅ local variable
> for (var i = 0; i < 10; i++) {}     // ✅ for loop
> for (var item : collection) {}      // ✅ enhanced for
> try (var r = new BufferedReader(...)){ } // ✅ try-with-resources
> ```
>
> Not allowed:
> - Instance or static fields
> - Method parameters
> - Return types
> - Local variable without initialiser (`var x;`)
> - Local variable initialised with `null` — null has no type to infer
> - Array initialiser shorthand (`var arr = {1, 2, 3}`)
>
> The restriction to local variables is intentional — fields and method
> signatures form a public API. Hiding their types with `var` would make
> APIs unreadable and break binary compatibility.

---

**Q3. What type does `var` infer — the interface or the concrete class?**

> `var` infers the **most specific type from the right-hand side** —
> the concrete class, not the interface:
>
> ```
> var list = new ArrayList<String>(); // inferred as ArrayList<String>
>                                     // NOT as List<String>
> list.trimToSize(); // ArrayList-specific method — compiles because var is ArrayList
>
> List<String> list2 = new ArrayList<>(); // explicit interface type
> list2.trimToSize(); // compile error — List has no trimToSize()
> ```
>
> This is a subtle difference. When you declare `List<String> list = new ArrayList<>()`,
> you are deliberately programming to the interface and restricting yourself
> to `List` methods. When you use `var`, you get the full concrete type.
>
> In practice: `var` is best when the concrete type is obvious from the RHS
> and you don't need to enforce interface-based programming at that variable.
> For method parameters and return types, explicit types are still required.

---

**Q4. Why is `Optional.orElseThrow()` preferred over `Optional.get()`?**

> Both throw `NoSuchElementException` when the Optional is empty, but
> `get()` has a misleading name — it reads like a safe accessor when
> it actually throws. This caused widespread misuse and bugs:
>
> ```
> // get() looks safe — isn't
> String name = optional.get(); // throws if empty — surprising
>
> // orElseThrow() — name makes the contract explicit
> String name = optional.orElseThrow(); // obviously throws if empty
> ```
>
> `get()` was effectively deprecated in spirit with Java 10. The Java API
> note even states: "if the Optional has no value, prefer orElseThrow()".
>
> `orElseThrow(Supplier)` from Java 8 is still the best choice when you
> want a meaningful exception message:
> ```
> User user = findUser(id)
>     .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
> ```
>
> Use `orElseThrow()` for "this must be present" assertions in internal
> code. Use `orElseThrow(Supplier)` when the exception needs context.

---

**Q5. What is the difference between `Collectors.toUnmodifiableList()` and wrapping with `Collections.unmodifiableList()`?**

> Both produce an unmodifiable list but differ in how they get there:
>
> `Collections.unmodifiableList(list)` — wraps an existing mutable list
> in an unmodifiable view. The underlying list is still mutable — if
> someone holds a reference to the original, they can still modify it,
> and those changes are visible through the wrapper:
>
> ```
> List<String> mutable = new ArrayList<>(List.of("a", "b"));
> List<String> view    = Collections.unmodifiableList(mutable);
> mutable.add("c");
> view.contains("c"); // true — view reflects changes to original
> ```
>
> `Collectors.toUnmodifiableList()` — collects stream results directly
> into a truly immutable list. No backing mutable list exists:
>
> ```
> List<String> immutable = stream.collect(Collectors.toUnmodifiableList());
> // No mutable backing — nobody can sneak modifications in
> ```
>
> In production: prefer `toUnmodifiableList()` when collecting stream
> results — it is more direct, has no mutable backing, and clearly
> communicates intent. Use `Collections.unmodifiableList` only when
> wrapping an existing collection you don't control.