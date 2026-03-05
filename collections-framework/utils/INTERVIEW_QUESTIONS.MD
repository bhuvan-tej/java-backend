# 🎯 Interview Questions — Utils (java.util.Collections)

---

> Utils questions as we gain experience we should go beyond listing methods. Interviewers
> expect you to know the binarySearch precondition, the unmodifiable
> view trap, synchronizedList's iteration gotcha, and the production
> pattern of returning empty instead of null.

---

## Core Concepts

**Q1. What is the difference between `Collection` (interface) and `Collections` (utility class)?**

> `Collection<E>` — an interface in `java.util`. The root of the collection
> hierarchy. Defines the contract that `List`, `Set`, and `Queue` extend.
> Has instance methods: `add()`, `remove()`, `contains()`, `size()`, `iterator()`.
>
> `Collections` — a utility class in `java.util`. All methods are static.
> Cannot be instantiated (private constructor). Provides algorithms and
> factory methods that operate on any `Collection` or `List`.
>
> ```
> // Collection — interface, used as type
> Collection<String> c = new ArrayList<>();
> c.add("hello");
>
> // Collections — utility class, all static
> Collections.sort(list);
> Collections.shuffle(list);
> Collections.unmodifiableList(list);
> ```
>
> Common interview trap: "implement Collection" vs "use Collections.sort()".
> They are completely unrelated despite similar names.

---

**Q2. What is the precondition for `Collections.binarySearch()` and what happens if it is violated?**

> The list **must be sorted** in ascending natural order (or by the provided
> Comparator) before calling `binarySearch()`. This is a hard precondition.
>
> If the list is not sorted, the result is **undefined** — not an exception.
> `binarySearch()` may return a wrong index, a negative value, or any
> arbitrary result. It does not detect that the list is unsorted.
>
> ```
> List<Integer> unsorted = Arrays.asList(5, 2, 8, 1, 9);
> Collections.binarySearch(unsorted, 5); // undefined — could return anything
>
> // Correct usage
> Collections.sort(list);
> int idx = Collections.binarySearch(list, 5); // now well-defined
> ```
>
> Return value: non-negative index if found, `-(insertionPoint) - 1` if not found.
> So `-1` means not found and would be inserted at index 0.
> `-(list.size()) - 1` means not found and would go at the end.
>
> This is why `binarySearch()` returning a negative value has a specific meaning
> — you can recover the insertion point: `insertionPoint = -result - 1`.

---

**Q3. `Collections.unmodifiableList()` returns a read-only view. What is the trap and how do you fix it?**

> The trap: it is a **view**, not a copy. The unmodifiable list still holds a
> reference to the original mutable list. If the original is modified, the
> view reflects those changes:
>
> ```
> List<String> original = new ArrayList<>(Arrays.asList("A", "B", "C"));
> List<String> readOnly = Collections.unmodifiableList(original);
>
> original.add("D");
> System.out.println(readOnly); // [A, B, C, D] — D visible through the view!
> ```
>
> This means `unmodifiableList()` only prevents the *caller* from modifying
> through the returned reference. It does not prevent the owner from modifying
> the backing list.
>
> Fix — copy first for true isolation:
> ```
> return Collections.unmodifiableList(new ArrayList<>(internalList));
> ```
> Or in Java 10+:
> ```
> return List.copyOf(internalList); // unmodifiable + independent copy
> ```

---

**Q4. Why should service methods return `Collections.emptyList()` instead of `null`?**

> Returning null forces every caller to add a null check. Forget one check
> anywhere and you get a NullPointerException at runtime:
> ```
> // BAD — null return
> List<Order> orders = orderService.getOrders(userId);
> for (Order o : orders) { ... } // NPE if orders is null!
>
> // Every caller must do this defensively:
> if (orders != null) { for (Order o : orders) { ... } }
> ```
>
> `Collections.emptyList()` returns an immutable empty list that is safe to
> iterate, pass to other methods, and call `size()` on — all without null checks:
> ```
> // GOOD — empty list return
> List<Order> orders = orderService.getOrders(userId);
> for (Order o : orders) { ... } // safe — iterates zero times
> orders.size();                  // 0 — no NPE
> ```
>
> Additional benefit: `emptyList()` returns a **cached singleton** — no memory
> allocation on every "not found" return. One instance is reused for the entire
> JVM lifetime.

---

**Q5. What is the difference between `Collections.synchronizedList()` and `CopyOnWriteArrayList`?**

> `Collections.synchronizedList(list)` — wraps with a single mutex. Every
> individual operation (`add`, `get`, `remove`, `size`) acquires the same lock.
> Thread-safe for individual operations but **iteration still requires manual
> synchronisation**:
> ```
> List<String> synced = Collections.synchronizedList(new ArrayList<>());
> // BAD — CME possible if another thread modifies during iteration
> for (String s : synced) { process(s); }
>
> // GOOD — manually lock the list itself
> synchronized (synced) {
>     for (String s : synced) { process(s); }
> }
> ```
>
> `CopyOnWriteArrayList` — every write creates a full copy of the internal array.
> Reads and iterations are lock-free — no CME ever, no manual synchronisation
> needed:
> ```
> List<String> cow = new CopyOnWriteArrayList<>();
> for (String s : cow) { process(s); } // always safe, no lock needed
> ```
>
> Choose `synchronizedList` when writes are frequent and the list is large.
> Choose `CopyOnWriteArrayList` when reads vastly outnumber writes and the
> list is small.

---

## Specific Methods

**Q6. How does `Collections.rotate()` work and what is a real production use case?**

> `rotate(list, distance)` shifts elements to the right by `distance` positions.
> Elements that fall off the right end wrap around to the left. Negative distance
> shifts left.
>
> ```
> List<String> list = Arrays.asList("A", "B", "C", "D", "E");
> Collections.rotate(list,  2); // [D, E, A, B, C]
> Collections.rotate(list, -1); // [E, A, B, C, D]
> ```
>
> Production use case — round-robin on-call rotation:
> ```
> List<String> oncall = new ArrayList<>(
>         Arrays.asList("Alice", "Bob", "Charlie", "Diana"));
>
> System.out.println("This week: " + oncall.get(0)); // Alice
> Collections.rotate(oncall, -1); // advance
> System.out.println("Next week: " + oncall.get(0)); // Bob
> ```
>
> Also useful for circular buffer simulation, rotating a matrix row, or
> implementing a token ring scheduling algorithm.

---

**Q7. What is `Collections.disjoint()` and how would you use it in an access control system?**

> `disjoint(c1, c2)` returns `true` if the two collections share no common
> elements. O(n) — iterates one collection checking membership in the other
> (which is O(1) if it is a `Set`).
>
> Access control pattern:
> ```
> Set<String> userRoles    = Set.of("VIEWER", "COMMENTER");
> Set<String> requiredRoles = Set.of("EDITOR", "ADMIN");
>
> // User needs at least ONE of the required roles
> boolean hasAccess = !Collections.disjoint(userRoles, requiredRoles);
> System.out.println("Access granted: " + hasAccess); // false
>
> Set<String> editorRoles = Set.of("EDITOR", "COMMENTER");
> boolean editorAccess = !Collections.disjoint(editorRoles, requiredRoles);
> System.out.println("Editor access: " + editorAccess); // true
> ```
>
> For best performance, pass the smaller collection first and the HashSet second
> — `disjoint` iterates the first collection and calls `contains()` on the
> second, so HashSet's O(1) `contains()` makes the whole operation O(n) where
> n is the size of the first collection.

---

**Q8. What does `Collections.copy()` do and what is its most common pitfall?**

> `copy(dest, src)` copies all elements from `src` into `dest`, overwriting
> dest's existing elements starting at index 0. It does NOT add to dest —
> it overwrites. The destination list must already have `size() >= src.size()`.
>
> Most common pitfall:
> ```
> List<String> src  = Arrays.asList("X", "Y", "Z");
> List<String> dest = new ArrayList<>(); // size 0!
> Collections.copy(dest, src); // IndexOutOfBoundsException!
> ```
>
> Fix — pre-size dest:
> ```
> List<String> dest = new ArrayList<>(Collections.nCopies(src.size(), null));
> Collections.copy(dest, src); // now works
> ```
>
> Or simply use the ArrayList copy constructor which is cleaner:
> ```
> List<String> dest = new ArrayList<>(src); // preferred
> ```
>
> `Collections.copy()` is useful when you need to copy into an existing
> pre-allocated list at a specific position — which the constructor cannot do.

---

**Q9. What is `Collections.nCopies()` and what are its gotchas?**

> `nCopies(n, element)` returns an immutable `List` containing `n` copies
> of the same element reference.
>
> Two important gotchas:
>
> **Gotcha 1 — immutable list:**
> ```
> List<String> list = Collections.nCopies(5, "X");
> list.set(0, "Y"); // UnsupportedOperationException!
>
> // Fix: wrap in ArrayList
> List<String> list = new ArrayList<>(Collections.nCopies(5, "X"));
> list.set(0, "Y"); // works
> ```
>
> **Gotcha 2 — same reference for mutable objects:**
> ```
> // All 3 sublists are the SAME object!
> List<List<String>> matrix = new ArrayList<>(
>         Collections.nCopies(3, new ArrayList<>()));
> matrix.get(0).add("X");
> System.out.println(matrix); // [[X], [X], [X]] — all rows modified!
>
> // Fix: create independent instances in a loop
> List<List<String>> matrix = new ArrayList<>();
> for (int i = 0; i < 3; i++)
>     matrix.add(new ArrayList<>(Collections.nCopies(3, ".")));
> ```

---

## Design & Production

**Q10. When would you use `Collections.singleton()` vs `List.of()` vs `Collections.singletonList()`?**

> All three produce immutable single-element collections but differ in type and use:
>
> `Collections.singletonList(e)` — returns an immutable `List<E>`. Use when
> an API expects a `List` and you have a single element to pass:
> ```
> repository.saveAll(Collections.singletonList(newOrder));
> ```
>
> `Collections.singleton(e)` — returns an immutable `Set<E>`. Use when
> an API expects a `Set`:
> ```
> cache.invalidateAll(Collections.singleton(key));
> ```
>
> `List.of(e)` (Java 9+) — also returns an immutable `List<E>`. Preferred
> in modern Java because it reads more clearly and supports varargs:
> ```
> List.of(item)          // single element
> List.of(a, b, c)       // multiple elements
> ```
>
> Key difference: `Collections.singletonList(null)` is allowed.
> `List.of(null)` throws `NullPointerException`. Choose accordingly.

---

**Q11. How would you implement a thread-safe cache using `Collections.synchronizedMap()` — and what is the race condition that still exists?**

> ```
> Map<String, Data> cache = Collections.synchronizedMap(new HashMap<>());
>
> // Individual operations are thread-safe
> cache.put("key", data);
> cache.get("key");
> cache.remove("key");
>
> // BUT this check-then-act is NOT atomic — race condition!
> if (!cache.containsKey("key")) {      // Thread A checks: false
>     cache.put("key", loadData("key")); // Thread B also checked false, both load!
> }
> ```
>
> Fix: use `ConcurrentHashMap.computeIfAbsent()` which is atomic:
> ```
> ConcurrentHashMap<String, Data> cache = new ConcurrentHashMap<>();
> cache.computeIfAbsent("key", k -> loadData(k)); // atomic check-then-put
> ```
>
> Also: `synchronizedMap` iteration still requires external synchronisation:
> ```
> synchronized (cache) {
>     for (Map.Entry<String, Data> e : cache.entrySet()) { ... }
> }
> ```

---

**Q12. `Collections.frequency()` vs a `HashMap` count — when would you use each?**

> `Collections.frequency(collection, element)` — O(n) linear scan, counts
> occurrences of one specific element. Use when you need the count of a
> single element and don't need counts for other elements:
> ```
> int javaCount = Collections.frequency(tags, "java");
> ```
>
> `HashMap` counter — O(n) to build, O(1) per lookup afterwards. Use when
> you need counts of multiple elements or will look up counts repeatedly:
> ```
> Map<String, Integer> counts = new HashMap<>();
> tags.forEach(t -> counts.merge(t, 1, Integer::sum));
> int javaCount   = counts.get("java");
> int springCount = counts.get("spring");
> ```
>
> If you call `Collections.frequency()` for k different elements, that is
> O(n·k). Building a HashMap once and querying is O(n + k). For more than
> one distinct element to count, HashMap always wins.

---

**Q13. What is `Collections.checkedList()` and when would you use it in production?**

> `Collections.checkedList(list, type)` wraps a list so that any attempt to
> insert an element of the wrong type throws `ClassCastException` immediately
> at the insertion point — not later when the element is retrieved.
>
> This is primarily useful for debugging type-safety issues caused by raw types
> and unchecked casts in legacy code:
> ```
> List<String> strings = new ArrayList<>();
> List checkedList = Collections.checkedList(strings, String.class);
>
> checkedList.add("hello");  // OK
> checkedList.add(42);       // ClassCastException immediately — not later!
> ```
>
> Without `checkedList`, inserting an Integer into a `List<String>` via raw
> type compiles without warning, and only fails with a confusing
> `ClassCastException` when the Integer is later read as a String —
> far from the actual bug site.
>
> Rarely needed in fully generic code. Useful when integrating with legacy
> APIs that use raw types.

---

**Q14. How does `Collections.shuffle()` work and how do you make it reproducible for testing?**

> `Collections.shuffle(list)` uses the Fisher-Yates algorithm (Knuth shuffle):
> iterates from the last element to the second, swapping each element with a
> randomly chosen element at or before it. O(n), uniform distribution.
>
> For reproducible shuffles in tests, pass a seeded `Random`:
> ```
> // Non-reproducible — different order every run
> Collections.shuffle(list);
>
> // Reproducible — same order every run with same seed
> Collections.shuffle(list, new Random(42));
> ```
>
> Production use cases:
> - Randomising question order in an exam
> - Card shuffling in a game
> - Random sampling without replacement (shuffle then take first n)
> - Load balancing — shuffle a list of servers to distribute initial assignments

---

**Q15. What is the difference between `Collections.unmodifiableList()`, `List.copyOf()`, and `List.of()` for defensive programming?**

> Three levels of defensiveness:
>
> `Collections.unmodifiableList(list)` — read-only **view** of the original.
> The original can still be mutated and changes are visible through this view.
> Zero allocation overhead:
> ```
> List<String> view = Collections.unmodifiableList(original);
> original.add("X"); // visible through view
> ```
>
> `List.copyOf(list)` (Java 10+) — unmodifiable **independent copy**.
> Mutations to original are NOT reflected. One-time copy cost:
> ```
> List<String> copy = List.copyOf(original);
> original.add("X"); // NOT visible through copy
> ```
>
> `List.of(a, b, c)` (Java 9+) — unmodifiable list created from **literals**.
> No backing list exists. Null elements not allowed:
> ```
> List<String> constants = List.of("READ", "WRITE", "DELETE");
> ```
>
> Choosing:
> - Exposing internal state safely with live updates → `unmodifiableList()`
> - Snapshot at a point in time, caller gets their own copy → `List.copyOf()`
> - Compile-time constants, config values → `List.of()`