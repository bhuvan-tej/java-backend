# 🎯 Interview Questions — Collections Framework

---

> These questions go beyond syntax and basic definitions.
> As we gain experience, interviewers expect you to explain internals,
> justify design decisions, discuss tradeoffs, and relate answers to
> production scenarios you have actually dealt with.

---

## Internals & Design

**Q1. Walk me through exactly what happens internally when you call `HashMap.put(key, value)` in Java 8+.**

> 1. `hash(key)` is computed: `key.hashCode() ^ (h >>> 16)` — XOR with upper bits spreads hash values more uniformly across buckets, reducing collisions in the lower bits used for indexing.
> 2. `index = hash & (capacity - 1)` — picks the bucket. Bitwise AND works because capacity is always a power of 2.
> 3. If bucket is empty → create a new `Node` and place it there.
> 4. If bucket has entries → walk the linked list, comparing `hash` first (cheap), then `equals()` (expensive). If key matches → overwrite value. If no match → append new node.
> 5. If the linked list in that bucket grows beyond **8 entries** → it is converted to a **Red-Black TreeNode** (`treeifyBin`). This brings worst-case lookup from O(n) to O(log n) for that bucket.
> 6. After insertion, if `size > capacity × loadFactor` → `resize()` is called. New capacity = old × 2. All entries are rehashed and redistributed.
>
> Key insight: the treeification threshold (8) and the load factor (0.75) are carefully chosen constants — not arbitrary.

---

**Q2. Why is the HashMap capacity always a power of 2? What breaks if it isn't?**

> The bucket index is computed as `hash & (capacity - 1)`. This only works correctly as a modulo operation when capacity is a power of 2, because `(2^n - 1)` in binary is all 1s — making the AND operation equivalent to `hash % capacity` but much faster (no division).
>
> If capacity were not a power of 2, you would need `hash % capacity` (integer division), which is significantly slower and also unevenly distributes keys because many hash values would cluster in fewer buckets.

---

**Q3. You have a custom class used as a `HashMap` key. What exactly must you implement and why?**

> You must override both `hashCode()` and `equals()` following the contract:
> - `equals()` defines logical equality for your domain (e.g., two `Employee` objects with the same ID are equal).
> - `hashCode()` must return the same value for objects that `equals()` considers equal.
>
> If you only override `equals()` without `hashCode()`:
> - Two logically equal objects may produce different hash codes
> - They land in different buckets
> - `map.get(key)` returns `null` even though the key appears to be in the map
>
> If you only override `hashCode()` without `equals()`:
> - Two objects land in the same bucket correctly
> - But `equals()` falls back to reference comparison (`==`) and never matches
>
> Additionally, **keys must be immutable** or at least their `hashCode()` must not change after insertion. If you mutate a key's fields after putting it in the map, its hash changes, it lands in the wrong bucket, and `get()` can no longer find it — a silent data loss bug.

---

**Q4. What is the difference between `HashMap`, `Collections.synchronizedMap(map)`, and `ConcurrentHashMap`? When would you choose each?**

> `HashMap` — not thread-safe. Multiple threads writing concurrently can corrupt the internal structure (infinite loop in Java 7 during resize was a notorious bug). Use only in single-threaded or externally synchronised contexts.
>
> `Collections.synchronizedMap(map)` — wraps the entire map with a single mutex. Every operation (including reads) acquires the same lock. Simple to use but terrible throughput under concurrent load — threads queue up on a single lock. Also requires external synchronisation during iteration.
>
> `ConcurrentHashMap` — fine-grained locking at the bucket level (Java 8+: CAS + synchronized per bin). Reads are lock-free. Writes only lock the affected bucket. Significantly better throughput under contention. `compute()`, `merge()`, `putIfAbsent()` are all atomic. Iteration is weakly consistent — no `ConcurrentModificationException` but may not reflect all in-flight writes.
>
> **Choose:** `HashMap` for single-threaded, `ConcurrentHashMap` for concurrent, `synchronizedMap` almost never in new code.

---

**Q5. Explain the treeification threshold in `HashMap`. Why is it 8 and not something else?**

> When a bucket's linked list exceeds 8 entries, it is converted to a Red-Black Tree. This is based on statistical analysis of Poisson distribution — under a good hash function with load factor 0.75, the probability of any bucket having 8 or more entries is approximately 0.00000006 (6 in 100 million). So treeification almost never happens with well-distributed keys and signals either a very large map or a poor `hashCode()` implementation.
>
> The tree conversion back to linked list happens at 6 entries (not 8) to provide hysteresis and avoid thrashing between tree and list on repeated insert/remove near the threshold.

---

## Concurrency

**Q6. Why does `ConcurrentHashMap` not allow null keys or values, while `HashMap` allows them?**

> In a concurrent context, `null` creates an ambiguity. If `map.get(key)` returns `null`, you cannot tell whether:
> - The key is not present in the map, OR
> - The key is present and mapped to `null`
>
> In a single-threaded `HashMap`, you can use `containsKey()` to distinguish these. But in a concurrent map, another thread could remove the key between your `get()` and `containsKey()` calls — making the check unreliable anyway.
>
> Doug Lea (author of `ConcurrentHashMap`) explicitly designed it this way to force callers to use sentinel values instead of null, resulting in cleaner concurrent code.

---

**Q7. In a high-throughput microservice, you need a shared in-memory counter per endpoint. How would you implement it correctly?**

> Use `ConcurrentHashMap` with `AtomicInteger` values:
> ```
> ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();
>
> // Thread-safe increment — computeIfAbsent + incrementAndGet are both atomic
> counters.computeIfAbsent(endpoint, k -> new AtomicInteger(0))
>         .incrementAndGet();
> ```
> Alternatively, use `merge()`:
> ```
> counters.merge(endpoint, 1, Integer::sum);
> ```
> Do NOT use `counters.put(key, counters.get(key) + 1)` — classic read-modify-write race condition even with `ConcurrentHashMap` because the get and put are two separate operations.

---

## Memory & Performance

**Q8. You are loading 10 million records into a `HashMap`. What steps would you take to optimise memory and performance?**

> 1. **Pre-size the map**: `new HashMap<>(expectedSize / 0.75 + 1)` — avoids repeated resize and rehash cycles. Each resize copies all entries.
> 2. **Ensure a good `hashCode()`**: Poor distribution causes bucket clustering, degrading to O(n) lookups. Test distribution with a load test.
> 3. **Consider load factor**: Lowering to 0.5 reduces collisions but uses more memory. Raising it saves memory but increases collisions. 0.75 is optimal for most cases.
> 4. **Use primitive-specialised maps** (`IntIntHashMap` from Eclipse Collections or Trove) if keys/values are primitives — avoids boxing overhead, significantly less memory.
> 5. **Profile GC**: Each `HashMap.Entry` is an object. 10M entries = 10M objects. Consider off-heap stores (`Chronicle Map`) for truly large datasets.

---

**Q9. When would `ArrayList` perform worse than `LinkedList` in practice? Give a concrete scenario.**

> When you are building a text editor or a stream processor that **only inserts and removes at the head** of a very large list. For example:
> ```
> // Simulating a log buffer — oldest entries removed from front
> list.remove(0);  // O(n) for ArrayList — shifts all elements left
>                  // O(1) for LinkedList/ArrayDeque — just move head pointer
> ```
> However, this scenario is rare and `ArrayDeque` is almost always a better choice than `LinkedList` even here because of cache locality. In most real-world code, `ArrayList` wins due to CPU cache friendliness.

---

## Design Decisions

**Q10. Why would you return `Collections.unmodifiableList()` from a service method instead of the raw list?**

> Defensive programming. If a service returns a raw `List`, callers can mutate the internal state of your service without going through your business logic — bypassing validation, event publishing, audit logging, etc.
>
> ```java
> // BAD — caller can mutate internal state
> public List<Order> getOrders() { return this.orders; }
>
> // GOOD — read-only view, mutations throw UnsupportedOperationException
> public List<Order> getOrders() {
>     return Collections.unmodifiableList(this.orders);
> }
> ```
>
> For true immutability (where even the original cannot change under the caller's feet), copy first: `Collections.unmodifiableList(new ArrayList<>(this.orders))`.
> In Java 10+, `List.copyOf(orders)` does this in one call.

---

**Q11. What is fail-fast iteration and have you ever encountered `ConcurrentModificationException` in production? How did you fix it?**

> Fail-fast iterators track an internal `modCount` on the collection. On each `next()` call, the iterator checks if `modCount` has changed since iteration started. If yes → `ConcurrentModificationException`.
>
> Common production scenario: a service that filters and removes elements from a list while iterating in a for-each loop. Fix:
> ```
> // Before fix — CME in production
> for (Session s : activeSessions) {
>     if (s.isExpired()) activeSessions.remove(s);
> }
>
> // Fix 1 — removeIf (cleanest)
> activeSessions.removeIf(Session::isExpired);
>
> // Fix 2 — iterator.remove()
> Iterator<Session> it = activeSessions.iterator();
> while (it.hasNext()) {
>     if (it.next().isExpired()) it.remove();
> }
> ```
> In concurrent scenarios where multiple threads access the list, switch to `CopyOnWriteArrayList`.

---

**Q12. TreeMap vs HashMap — you need to find all users who registered between two dates. Which do you use and why?**

> `TreeMap<LocalDate, List<User>>` — keys are dates, naturally sorted.
> ```
> TreeMap<LocalDate, List<User>> registrations = new TreeMap<>();
>
> // O(log n) range query — returns a view of all entries between the dates
> Map<LocalDate, List<User>> range = registrations.subMap(
>     LocalDate.of(2024, 1, 1),  true,
>     LocalDate.of(2024, 3, 31), true
> );
> ```
> With `HashMap` you would have to iterate all entries and filter — O(n). With `TreeMap` the range query is O(log n) to find start + O(k) to traverse results where k is the result size. For any range or bracket query on keys, `TreeMap` is the right tool.

---

**Q13. What is the difference between `Comparable` and `Comparator`? When have you used each in a real project?**

> `Comparable` — the object defines its own natural ordering. Implement when your class has one obvious sort order that always makes sense (e.g., `LocalDate` ordered by date, `Employee` ordered by ID). It is part of the class's contract.
>
> `Comparator` — external ordering strategy. Use when:
> - You need multiple orderings (by name, by salary, by department)
> - You do not own the class (sorting `String` by length, not alphabetically)
> - The ordering is context-specific (ascending in one screen, descending in another)
>
> Production example — a report service that sorts the same `Order` list differently based on user preference:
> ```
> Map<String, Comparator<Order>> sortStrategies = new HashMap<>();
> sortStrategies.put("date",   Comparator.comparing(Order::getDate).reversed());
> sortStrategies.put("amount", Comparator.comparingDouble(Order::getAmount).reversed());
> sortStrategies.put("status", Comparator.comparing(Order::getStatus));
>
> orders.sort(sortStrategies.getOrDefault(userPreference,
>             Comparator.comparing(Order::getDate)));
> ```

---

**Q14. You see `LinkedHashMap` used as an LRU Cache in your codebase. What are its limitations and when would you replace it with something else?**

> `LinkedHashMap`-based LRU is fine for single-threaded or low-concurrency use. Limitations:
>
> 1. **Not thread-safe** — concurrent access requires external synchronisation (`Collections.synchronizedMap()`), which turns it into a single-lock bottleneck.
> 2. **No TTL (time-to-live)** — entries never expire by time, only by access count. Stale data stays forever if it keeps getting accessed.
> 3. **No async loading** — cache miss blocks the calling thread while loading.
> 4. **No metrics** — no hit rate, miss rate, or eviction tracking out of the box.
>
> For production: replace with **Caffeine** (`CaffeineCache`) which offers O(1) concurrent LRU/LFU, TTL, async loading, and built-in metrics. Used as the default cache in Spring Boot.

---

**Q15. Explain the `compute()`, `computeIfAbsent()`, `computeIfPresent()`, and `merge()` methods on `Map`. When would you use each?**

> All four avoid the non-atomic `get()` → check → `put()` pattern:
>
> `computeIfAbsent(key, mappingFn)` — only called if key is absent. Returns existing value if present, otherwise calls the function and stores result. Use for lazy initialisation:
> ```
> map.computeIfAbsent("orders", k -> new ArrayList<>()).add(order);
> ```
>
> `computeIfPresent(key, remappingFn)` — only called if key exists. If function returns null, key is removed. Use for conditional updates:
> ```
> map.computeIfPresent("stock", (k, qty) -> qty > 1 ? qty - 1 : null);
> ```
>
> `compute(key, remappingFn)` — always called, whether key exists or not. Null return removes the key. Use for atomic read-modify-write:
> ```
> map.compute("count", (k, v) -> v == null ? 1 : v + 1);
> ```
>
> `merge(key, value, mergeFn)` — if key absent → store value; if present → apply function to old and new value. Cleanest for aggregation:
> ```
> map.merge(word, 1, Integer::sum);  // frequency counter
> ```
>
> In `ConcurrentHashMap` all four are **atomic per key** — safe for concurrent use without external locking.