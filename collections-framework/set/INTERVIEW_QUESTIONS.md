# 🎯 Interview Questions — Set

---

> As we gain experience, Set questions go well beyond "HashSet uses hashCode".
> Interviewers expect you to explain the hashCode/equals contract deeply,
> discuss what silently breaks without it, reason about TreeSet's
> compareTo-based uniqueness, and relate answers to production bugs.

---

## Internals

**Q1. How does `HashSet` determine if an element is a duplicate?**

> `HashSet` is backed by a `HashMap` where your element is the key and a dummy constant (`PRESENT`) is the value. When you call `add(element)`:
> 1. `hashCode()` is called to find the bucket index.
> 2. If the bucket is empty → not a duplicate, add it.
> 3. If the bucket has entries → walk the chain, calling `equals()` on each entry.
> 4. If any `equals()` returns true → duplicate, reject it.
>
> This is why both methods must be overridden together. If `hashCode()` returns different values for logically equal objects, they land in different buckets and `equals()` is never even called — the duplicate slips through silently.

---

**Q2. What exactly breaks if you override `equals()` but not `hashCode()`?**

> Three things break silently — no exceptions, just wrong behaviour:
>
> ```
> Set<Employee> set = new HashSet<>();
> set.add(new Employee(1, "Alice"));
> set.add(new Employee(1, "Alice")); // duplicate — NOT detected
> System.out.println(set.size()); // 2 — should be 1
>
> set.contains(new Employee(1, "Alice")); // false — should be true
> ```
>
> Because `hashCode()` falls back to `Object.hashCode()` which uses memory address — every `new Employee(...)` gets a different hash. They land in different buckets. `equals()` is never invoked. The set grows unboundedly with what appear to be duplicates, and `contains()` always returns false for a logically equivalent object.
>
> The reverse — overriding `hashCode()` but not `equals()` — also breaks `contains()` and `remove()`. Both must always be overridden together.

---

**Q3. You mutate a field of an object after adding it to a `HashSet`. What happens?**

> Silent data loss — one of the nastiest bugs in Java.
>
> ```
> Employee e = new Employee(1, "Alice");
> set.add(e);
> e.id = 999; // mutate after adding
>
> set.contains(e);      // false — hash changed, wrong bucket
> set.remove(e);        // false — cannot find it
> set.size();           // 1 — it's still there, just unreachable
> ```
>
> The object is still physically in the set but is now in the wrong bucket based on its original hash. It is effectively lost — neither findable nor removable without iterating the entire set.
>
> This is why **HashMap keys and HashSet elements must be immutable**, or at minimum, the fields used in `hashCode()` and `equals()` must not change after insertion. `String`, `Integer`, and other wrapper types are safe precisely because they are immutable.

---

**Q4. How does `TreeSet` determine uniqueness — and how is it different from `HashSet`?**

> `TreeSet` uses `compareTo()` (or the provided `Comparator`) — not `hashCode()`/`equals()`. If `compareTo()` returns 0, the element is considered a duplicate and is not stored, even if `equals()` would return false.
>
> This creates a subtle trap:
> ```
> // Comparator only on price — same price → compareTo returns 0 → duplicate!
> TreeSet<Product> set = new TreeSet<>(
>     Comparator.comparingDouble(Product::getPrice));
>
> set.add(new Product("Phone",  45_000));
> set.add(new Product("Tablet", 45_000)); // rejected — compareTo returns 0
> System.out.println(set.size()); // 1 — Tablet was silently dropped!
> ```
>
> Fix: always include a tiebreaker field so compareTo only returns 0 for truly identical objects:
> ```
> TreeSet<Product> set = new TreeSet<>(
>     Comparator.comparingDouble(Product::getPrice)
>               .thenComparing(Product::getName));
> ```

---

**Q5. Why does `TreeSet` not allow null elements?**

> Because `TreeSet` calls `compareTo()` on the element during insertion to find the correct position in the tree. Calling `compareTo()` on null throws `NullPointerException`.
>
> `HashSet` allows one null because `hashCode()` on null is defined as 0 by convention, and `equals()` handles the null case. `TreeSet` has no equivalent null-safe path in its comparison logic.

---

## NavigableSet

**Q6. Explain `floor()`, `ceiling()`, `lower()`, and `higher()` — and give a real use case for each.**

> All four are O(log n) and are the primary reason to choose `TreeSet` over `HashSet`.
>
> - `floor(x)` — largest element **≤ x**. Use case: find the tax bracket that applies to an income.
> - `ceiling(x)` — smallest element **≥ x**. Use case: find the next available appointment slot after a given time.
> - `lower(x)` — largest element **strictly < x**. Use case: find the previous trading price before a threshold.
> - `higher(x)` — smallest element **strictly > x**. Use case: find the next price level above current market price.
>
> ```
> // Tax bracket lookup
> TreeSet<Integer> brackets = new TreeSet<>(
>     Arrays.asList(0, 250_000, 500_000, 1_000_000));
> System.out.println(brackets.floor(350_000)); // 250_000 — correct bracket
> ```

---

**Q7. What is the difference between `subSet()`, `headSet()`, and `tailSet()`? Are they copies or views?**

> All three return **views** — they are backed by the original `TreeSet`. Modifications to the view are reflected in the original and vice versa.
>
> - `headSet(to)` — all elements strictly less than `to`
> - `tailSet(from)` — all elements ≥ `from`
> - `subSet(from, fromInclusive, to, toInclusive)` — range between from and to with inclusive flags
>
> Since they are views, inserting an element outside the range into the view throws `IllegalArgumentException`:
> ```
> TreeSet<Integer> set = new TreeSet<>(Arrays.asList(1,2,3,4,5));
> SortedSet<Integer> view = set.subSet(2, 4);
> view.add(10); // throws IllegalArgumentException — 10 outside [2,4)
> ```
> To get an independent copy: `new TreeSet<>(set.subSet(2, 4))`.

---

## Design & Production

**Q8. In your service, you have a `List<String>` of user roles and you check `list.contains(role)` on every API request. What is the problem and how do you fix it?**

> `List.contains()` is O(n) — every request scans the entire list. For a user with 50 roles and 1000 requests/second, that is 50,000 comparisons per second for a single user's role check alone.
>
> Fix: convert to a `HashSet` once, then check membership in O(1):
> ```
> // BAD — O(n) on every request
> List<String> roles = user.getRoles(); // ["ADMIN", "MANAGER", "USER", ...]
> if (roles.contains("ADMIN")) { ... }
>
> // GOOD — O(1) on every request
> Set<String> roleSet = new HashSet<>(user.getRoles()); // build once
> if (roleSet.contains("ADMIN")) { ... }                // O(1) lookup
> ```
> In practice, build the `HashSet` when the user session is created, store it in the session/cache, and reuse it for the entire request lifecycle.

---

**Q9. You need to find all orders placed within a date range. You have a `Set<LocalDate>` of order dates. Which Set do you use and why?**

> `TreeSet<LocalDate>` — `LocalDate` implements `Comparable` and `TreeSet` gives you O(log n) range queries:
> ```java
> TreeSet<LocalDate> orderDates = new TreeSet<>(allDates);
>
> // All dates in Q1 2024 — O(log n) to find start + O(k) to traverse
> NavigableSet<LocalDate> q1 = orderDates.subSet(
>     LocalDate.of(2024, 1, 1),  true,
>     LocalDate.of(2024, 3, 31), true
> );
> ```
> With `HashSet` you would iterate all dates and filter — O(n). `TreeSet` turns this into O(log n + k) where k is result size. For any range or time-window query on a set, `TreeSet` is the right choice.

---

**Q10. What is the difference between `LinkedHashSet` and maintaining a `List` for ordered uniqueness?**

> Both preserve insertion order and allow unique elements, but they differ in performance:
>
> | Operation | LinkedHashSet | List + manual dedup |
> |-----------|--------------|---------------------|
> | `contains()` | O(1) | O(n) |
> | `add()` (check + insert) | O(1) | O(n) for contains check |
> | `remove()` | O(1) | O(n) |
> | Iteration | O(n) | O(n) |
>
> `LinkedHashSet` is strictly superior when you need ordered uniqueness. The `List` approach is O(n) for every dedup check and O(n) for removal. `LinkedHashSet` is O(1) for all three.
>
> Common mistake in production: developers use `List.contains()` in a loop to deduplicate → O(n²) overall. Replace with `LinkedHashSet` for O(n).

---

**Q11. How would you implement a "recently viewed items" feature where the most recently viewed item always appears last and the list is capped at N items?**

> `LinkedHashSet` with remove-then-add pattern:
> ```java
> class RecentlyViewed {
>     private final int maxSize;
>     private final LinkedHashSet<String> items = new LinkedHashSet<>();
>
>     void view(String item) {
>         items.remove(item);   // O(1) — remove from current position
>         items.add(item);      // O(1) — re-insert at tail (most recent)
>         if (items.size() > maxSize) {
>             items.remove(items.iterator().next()); // O(1) — evict oldest (head)
>         }
>     }
> }
> ```
> Why not a `LinkedList`? Because `LinkedList.contains()` is O(n) — you'd need to check if the item already exists before moving it. `LinkedHashSet` gives O(1) existence check and O(1) remove in one structure.
>
> Why not `LinkedHashMap` with access order? That would also work and is slightly more idiomatic for LRU, but `LinkedHashSet` is cleaner when you only store keys with no associated value.

---

**Q12. You need to store a set of `Employee` objects. Two employees are equal if they have the same `id`. You implement `equals()` by id but your tech lead says your `hashCode()` implementation is wrong. What did you do?**

> Classic mistake — using a mutable or non-id field in `hashCode()`:
>
> ```java
> // WRONG — uses name which is not part of equals()
> @Override public int hashCode() {
>     return Objects.hash(name); // name != equality field
> }
>
> // ALSO WRONG — returns constant (valid but terrible performance)
> @Override public int hashCode() { return 42; }
> // All employees hash to same bucket → O(n) lookup (effectively a linked list)
>
> // CORRECT — use exactly the same field(s) as equals()
> @Override public int hashCode() {
>     return Objects.hash(id); // id is the equality field
> }
> ```
>
> The contract: if `a.equals(b)` then `a.hashCode() == b.hashCode()`. So `hashCode()` must use at least the same fields as `equals()`. Using fewer fields is OK (multiple objects can share a hash), but using different fields breaks the contract entirely.

---

**Q13. What is `CopyOnWriteArraySet` and when would you use it over `HashSet` or `Collections.synchronizedSet()`?**

> `CopyOnWriteArraySet` is backed by a `CopyOnWriteArrayList`. On every write operation it creates a full copy of the internal array. This means:
> - Reads and iterations are completely lock-free — no `ConcurrentModificationException` ever
> - Writes are expensive — O(n) copy on every add/remove
> - Best for read-heavy, write-rare scenarios (e.g., a set of active listeners, feature flags)
>
> `Collections.synchronizedSet()` wraps with a single mutex — every operation (including reads) locks. Simpler but bottlenecks under concurrent reads.
>
> Use `CopyOnWriteArraySet` when: reads vastly outnumber writes, the set is small, and you need lock-free iteration.
> Use `ConcurrentHashMap.newKeySet()` when: writes are frequent and you need true O(1) concurrent performance.

---

**Q14. `TreeSet` vs sorting an `ArrayList` — when does `TreeSet` win?**

> `ArrayList` + `Collections.sort()` is O(n log n) and only sorted at the moment you call it. Any subsequent add requires another sort.
>
> `TreeSet` maintains sorted order at all times — every `add()` inserts in the correct position in O(log n). It wins when:
> - Elements are added and queried frequently (not in a single batch)
> - You need range queries — `subSet()`, `floor()`, `ceiling()` — which have no equivalent on a List without scanning
> - You need the min or max frequently — `first()` and `last()` are O(log n), whereas `Collections.min()` on a List is O(n)
>
> `ArrayList` + sort wins when: you collect all elements first and then need one sorted iteration — the TimSort is cache-friendly and very fast in practice.

---

**Q15. What happens when you call `TreeSet.add()` with a `Comparator` that is inconsistent with `equals()`?**

> "Inconsistent with equals" means `compareTo()` returns non-zero for objects where `equals()` returns true, or `compareTo()` returns 0 where `equals()` returns false.
>
> `TreeSet` uses `compareTo()` for uniqueness — not `equals()`. So:
> ```
> // Comparator returns 0 for same price (even different names)
> // → TreeSet treats them as duplicates regardless of equals()
> TreeSet<Product> set = new TreeSet<>(Comparator.comparingDouble(Product::getPrice));
> set.add(new Product("Phone",  50_000));
> set.add(new Product("Tablet", 50_000)); // dropped — compareTo() = 0
>
> // set.contains(new Product("Phone", 50_000))  → true
> // set.contains(new Product("Tablet", 50_000)) → also true (same bucket position)
> // But set.size() = 1 — Tablet was never stored
> ```
> The Javadoc calls this "inconsistent with equals" and states that `TreeSet` "behaves correctly" but in a way that violates the general `Set` contract. The fix is always to ensure your `Comparator` returns 0 only for objects that are genuinely interchangeable in your domain.