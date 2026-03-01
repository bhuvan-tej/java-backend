# 🎯 Interview Questions — Lists
### Targeted at 5 Years of Java Experience

---

> At 5 years experience, interviewers expect you to go beyond "ArrayList uses
> an array internally". They want to hear about resize mechanics, production
> bugs you have debugged, design tradeoffs, and real-world scenarios.

---

## Internals

**Q1. Walk me through what happens internally when `ArrayList` runs out of capacity.**

> When `size == capacity`, a new array is allocated with capacity `oldCapacity + (oldCapacity >> 1)` — that is roughly 1.5× the old size (not 2×, which is a common misconception). All existing elements are copied to the new array via `Arrays.copyOf()` — an O(n) operation.
>
> This is why `add()` is O(1) **amortized**, not always O(1). Across n insertions, the total copy work is n/1.5 + n/1.5² + ... which converges to O(n). So per-element cost is O(1) averaged.
>
> To avoid this entirely when you know the expected size:
> ```
> List<Order> orders = new ArrayList<>(10_000); // pre-size
> ```

---

**Q2. What is the exact difference between `remove(int index)` and `remove(Object o)` in `ArrayList`? What bug does this cause?**

> Java resolves method overloads at compile time based on argument type.
> - `remove(int index)` → removes element at that position
> - `remove(Object o)` → removes first occurrence of that object
>
> The bug appears with `List<Integer>`:
> ```
> List<Integer> list = new ArrayList<>(Arrays.asList(10, 20, 30));
> list.remove(1);                  // removes INDEX 1 → [10, 30]
> list.remove(Integer.valueOf(10)); // removes VALUE 10 → [20, 30]
> ```
> At first glance both look identical. This is a classic interview question precisely because it silently produces wrong results with no exception. Always use `Integer.valueOf()` when removing by value from a `List<Integer>`.

---

**Q3. `subList()` returns a view, not a copy. What problems can this cause in production and how do you handle them?**

> Three specific issues:
>
> 1. **Mutation propagates back** — modifying the subList modifies the original. Unintended for most callers expecting a copy.
> ```
> List<String> sub = list.subList(1, 4);
> sub.clear(); // removes elements 1-3 from the original list!
> ```
>
> 2. **`ConcurrentModificationException`** — if the backing list is structurally modified after `subList()` is called, iterating the subList throws CME.
> ```
> List<String> sub = list.subList(0, 3);
> list.add("X");       // structural modification
> sub.get(0);          // throws ConcurrentModificationException
> ```
>
> 3. **Memory leak** — a small subList holds a reference to the entire backing array. Even if the original list goes out of scope, the full array stays in memory.
>
> **Fix:** Always copy when exposing to callers:
> ```
> return new ArrayList<>(list.subList(from, to));
> ```

---

**Q4. You have a `List<String>` with 1 million elements. You need to remove all elements that match a condition. What is the most efficient approach and why?**

> Use `removeIf()`:
> ```
> list.removeIf(s -> s.startsWith("ERROR"));
> ```
> It is O(n) with a single pass through the array. Internally it uses a bitmask to identify removals, then compacts the array in place — avoiding the repeated O(n) shifts that would occur with `list.remove(element)` in a loop.
>
> `iterator.remove()` is also O(n) total but has more overhead per removal.
>
> Never use indexed `list.remove(i)` in a loop going forward — each call shifts all subsequent elements, giving O(n²) overall.

---

**Q5. When does `Arrays.asList()` throw `UnsupportedOperationException` and why?**

> `Arrays.asList()` returns a **fixed-size** `List` backed directly by the original array. It has no backing `ArrayList` — it is a thin adapter. `add()` and `remove()` are not supported because the underlying array cannot change size.
>
> `set()` **does work** because it only replaces a value without changing the array size.
>
> ```
> List<String> list = Arrays.asList("A", "B", "C");
> list.set(0, "X");  // works — just replaces
> list.add("D");     // throws UnsupportedOperationException
> ```
>
> Also: changes to the list mutate the original array and vice versa.
>
> Use `new ArrayList<>(Arrays.asList(...))` when you need a truly mutable list, or `List.of(...)` (Java 9+) for a truly immutable one.

---

## Iteration & Modification

**Q6. Explain `ConcurrentModificationException` — how is it detected, and is it guaranteed to be thrown every time?**

> `ArrayList` maintains a `modCount` field that increments on every structural modification (add, remove, clear). When an iterator is created it copies `modCount` into `expectedModCount`. On every `next()` call: `if (modCount != expectedModCount) throw new ConcurrentModificationException`.
>
> It is **not guaranteed** to fire every time — the Javadoc explicitly says it is "best-effort" and should not be relied upon for program correctness. In a single-threaded scenario it is very consistent. In multi-threaded scenarios you might not always see it, or you might see corrupted data instead.
>
> The right fix is always to use proper synchronisation or concurrent collections — not to rely on CME as a safety net.

---

**Q7. What is `ListIterator` and when would you actually use it over a regular `Iterator` or for-each loop?**

> `ListIterator` provides bidirectional traversal and in-place modification during iteration:
> - `hasPrevious()` / `previous()` — traverse backwards
> - `set(e)` — replace the element last returned by `next()` or `previous()`
> - `add(e)` — insert element at current cursor position
> - `nextIndex()` / `previousIndex()` — current position in the list
>
> Real use cases:
> ```
> // Uppercase all elements in-place without creating a new list
> ListIterator<String> it = list.listIterator();
> while (it.hasNext()) it.set(it.next().toUpperCase());
>
> // Insert after a specific element
> while (it.hasNext()) {
>     if (it.next().equals("DELHI")) it.add("JAIPUR"); // inserts after DELHI
> }
>
> // Reverse traversal to build reverse string
> ListIterator<String> rev = list.listIterator(list.size());
> while (rev.hasPrevious()) System.out.print(rev.previous());
> ```

---

## Design & Production

**Q8. In a REST API, a service method returns a `List<Order>`. Should it return the internal list or a copy? Why?**

> Never return the internal list directly. It violates encapsulation and creates subtle bugs:
> ```
> // BAD — caller can corrupt service state
> public List<Order> getOrders() { return this.orders; }
>
> // Caller does this and mutates your service internals
> orderService.getOrders().clear(); // orders gone!
> ```
>
> Options in order of preference:
> 1. `return Collections.unmodifiableList(this.orders)` — zero-copy read-only view. Mutations throw `UnsupportedOperationException`. Note: underlying list changes are still visible to the caller.
> 2. `return new ArrayList<>(this.orders)` — full defensive copy. Mutations are isolated. Higher memory cost.
> 3. `return List.copyOf(this.orders)` (Java 10+) — immutable copy, cleanest API.
>
> In a Spring service returning data for serialisation (Jackson), a read-only view is sufficient. For domain services where the list might be stored by the caller, always return a copy.

---

**Q9. `ArrayList` vs `ArrayDeque` — you need a Stack. Which do you use and why?**

> `ArrayDeque` for Stack behaviour. Three reasons:
>
> 1. **Semantics** — `ArrayDeque` has explicit `push()`/`pop()`/`peek()` that communicate Stack intent clearly. `ArrayList` requires `add(size-1)` and `remove(size-1)` which is unclear.
> 2. **Performance** — `ArrayDeque` is a circular buffer optimised for head/tail ops. `ArrayList.remove(0)` is O(n) due to shifts — though `ArrayList.remove(size-1)` is O(1).
> 3. **No legacy baggage** — the `Stack` class extends `Vector` which synchronises every method. `ArrayDeque` has no synchronisation overhead.
>
> Never use `java.util.Stack` in new code. Use `Deque<T> stack = new ArrayDeque<>()`.

---

**Q10. You discover that a background thread is getting `ConcurrentModificationException` on an `ArrayList` that a request thread is writing to. How do you fix it?**

> Several options depending on access pattern:
>
> **Option 1 — `CopyOnWriteArrayList`** — best when reads heavily outnumber writes:
> ```
> List<String> list = new CopyOnWriteArrayList<>();
> ```
> Every write creates a full copy of the array. Reads and iterations always see a consistent snapshot — no CME ever. Expensive for write-heavy scenarios.
>
> **Option 2 — `Collections.synchronizedList()`** — wraps with a mutex:
> ```
> List<String> list = Collections.synchronizedList(new ArrayList<>());
> // Must manually synchronise on iteration
> synchronized (list) {
>     for (String s : list) { ... }
> }
> ```
> Simpler but coarse-grained — bottleneck under high concurrency.
>
> **Option 3 — Queue-based separation** — request thread writes to a `ConcurrentLinkedQueue`, background thread drains it periodically. Decouples producers from consumers entirely.
>
> At 5 years experience, you should know all three and be able to explain which fits each scenario.

---

**Q11. What is `trimToSize()` and in what real-world scenario would you actually call it?**

> `((ArrayList<T>) list).trimToSize()` shrinks the internal array to exactly `size()`, freeing the unused capacity slots.
>
> Real scenario: batch processing where you build a large `ArrayList` (pre-sized to 100,000), fill it, filter it down to ~5,000 results, then cache those results in memory for a long time. Without `trimToSize()`, the internal array still holds space for 100,000 elements even though only 5,000 are used — wasting ~95,000 object references in memory.
>
> ```
> List<Product> results = new ArrayList<>(100_000);
> // ... fill and filter down to ~5000 elements
> results.removeIf(p -> !p.isActive());
> ((ArrayList<Product>) results).trimToSize(); // free the wasted 95k slots
> cache.put("active_products", results);
> ```
> Not needed in everyday code. Only relevant when large lists live long in memory.

---

**Q12. How would you implement a circular buffer using `ArrayList`? What is a better alternative?**

> `ArrayList` is a poor fit for a circular buffer because removing from the front is O(n) (shifts elements). A `LinkedList` is also poor due to node allocation overhead.
>
> `ArrayDeque` is the right choice — it is already a circular array buffer internally:
> ```java
> Deque<LogEntry> circularBuffer = new ArrayDeque<>(maxSize);
>
> void add(LogEntry entry) {
>     if (circularBuffer.size() >= maxSize) {
>         circularBuffer.pollFirst(); // remove oldest
>     }
>     circularBuffer.offerLast(entry); // add newest
> }
> ```
> O(1) add and O(1) eviction. For concurrent access, `LinkedBlockingDeque` or a proper `RingBuffer` (Disruptor) for ultra-high throughput.

---

**Q13. `List.of()` vs `Arrays.asList()` vs `Collections.unmodifiableList()` — what is the difference between these three "read-only" list approaches?**

> | | `Arrays.asList()` | `Collections.unmodifiableList()` | `List.of()` (Java 9+) |
> |---|---|---|---|
> | `add()`/`remove()` | ❌ throws | ❌ throws | ❌ throws |
> | `set()` | ✅ allowed | ❌ throws | ❌ throws |
> | Null elements | ✅ allowed | depends on backing list | ❌ throws NPE |
> | Backed by original | ✅ yes | ✅ yes | ❌ independent copy |
> | Truly immutable | ❌ no | ❌ no (backing list can change) | ✅ yes |
>
> `List.of()` is the only truly immutable option — use it for constants, config, and method return values where you want to guarantee immutability.

---

**Q14. In a performance-critical path, you have a `List<String>` and need to check if a value exists. What is the problem and how do you fix it?**

> `list.contains(value)` is O(n) — linear scan from index 0. In a hot code path with a large list this is a significant bottleneck.
>
> Fix: if membership checks are frequent, use a `HashSet` in parallel:
> ```
> Set<String> lookupSet = new HashSet<>(list); // O(n) once
> if (lookupSet.contains(value)) { ... }        // O(1) every time
> ```
> If the list never changes, build the set once and reuse. If the list changes frequently, maintain both structures in sync.
>
> This is a classic data structure tradeoff: `ArrayList` is excellent for ordered storage and iteration; `HashSet` for membership checks. Use the right tool for the right operation.

---

**Q15. How does Java's `Collections.sort()` work internally and why is it called TimSort?**

> Java uses **TimSort** — a hybrid of MergeSort and InsertionSort developed by Tim Peters. It exploits the fact that real-world data often contains naturally sorted subsequences (called "runs").
>
> Algorithm:
> 1. Scan the array for existing sorted runs (ascending or descending sequences).
> 2. If a run is shorter than `minRun` (typically 32-64), extend it using InsertionSort — which is very fast on nearly-sorted small arrays.
> 3. Push runs onto a stack and merge adjacent runs using MergeSort when they satisfy balance criteria.
>
> Complexity: O(n log n) worst case, O(n) best case (already sorted). Stable sort — equal elements maintain their original order.
>
> Why it matters in practice: sorting a `List<Order>` where most orders are already in date order will be much faster than O(n log n) because TimSort detects the existing sorted runs. This is why `Collections.sort()` often feels "fast" on real data.