# 🎯 Interview Questions — Iterator

---

> Iterator questions as we gain experience, cover the modCount mechanism in depth,
> the difference between fail-fast and fail-safe, custom Iterable design,
> and the BST iterator pattern — a classic senior interview problem.

---

## Core Concepts

**Q1. How does Java's for-each loop actually work under the hood?**

> The for-each loop is syntactic sugar for the Iterator pattern. The compiler
> transforms it at compile time:
>
> ```
> // Source code
> for (String s : list) {
>     System.out.println(s);
> }
>
> // Compiled equivalent
> Iterator<String> it = list.iterator();
> while (it.hasNext()) {
>     String s = it.next();
>     System.out.println(s);
> }
> ```
>
> This is why for-each works on any class that implements `Iterable<T>` —
> the compiler just calls `iterator()` on it. It also explains why you get
> `ConcurrentModificationException` inside a for-each when you call
> `list.remove()` — the underlying Iterator's `next()` detects the
> structural modification.
>
> Arrays are a special case — the compiler transforms array for-each into
> a classic index loop, not an Iterator.

---

**Q2. Explain the `modCount` mechanism. How does fail-fast detection work?**

> Every structurally modifiable collection (`ArrayList`, `HashMap`, `HashSet`,
> etc.) maintains an `int modCount` field. It increments on every structural
> modification — `add()`, `remove()`, `clear()`, `set()` on some collections.
>
> When an Iterator is created, it captures:
> ```
> int expectedModCount = modCount; // snapshot at iterator creation time
> ```
>
> On every `next()` call:
> ```
> if (modCount != expectedModCount)
>     throw new ConcurrentModificationException();
> ```
>
> So if anything modifies the collection between iterator creation and
> iteration completion, the next `next()` call throws CME.
>
> Critical detail: this is **best-effort only** — not a guaranteed thread-safety
> mechanism. The Javadoc explicitly says it should not be relied upon for
> program correctness. In multi-threaded scenarios, the modCount check is not
> atomic — you might corrupt data without ever seeing a CME.

---

**Q3. What is the difference between fail-fast and fail-safe iterators?**

> **Fail-fast** (ArrayList, HashMap, HashSet, TreeMap):
> - Iterates directly over the live collection
> - Tracks `modCount` — throws CME if collection modified outside iterator
> - No memory overhead
> - CME is best-effort, not guaranteed in concurrent scenarios
>
> **Fail-safe** (CopyOnWriteArrayList, ConcurrentHashMap):
> - Iterator takes a **snapshot** of the internal array at creation time
> - Iterates over the snapshot — live modifications to the collection are invisible
> - No CME ever
> - Memory overhead — full copy made on each write to the live collection
> - May not see elements added or removed after iterator was created
>
> ```
> List<String> cow = new CopyOnWriteArrayList<>(Arrays.asList("A","B","C"));
> Iterator<String> it = cow.iterator(); // snapshot: [A, B, C]
> cow.add("D");                         // modifies live list, not snapshot
> cow.remove("A");                      // also modifies live list only
>
> // Iterator sees: A B C — the snapshot, not the current live state
> while (it.hasNext()) System.out.print(it.next() + " ");
> // prints: A B C
>
> System.out.println(cow); // live list: [B, C, D]
> ```

---

**Q4. You have a `List<String>` and need to remove elements matching a condition while iterating. What are all the correct approaches?**

> Three correct approaches, in order of preference for modern Java:
>
> **Option 1 — `removeIf()` (Java 8+, cleanest):**
> ```
> list.removeIf(s -> s.startsWith("ERROR"));
> ```
> Single pass, O(n), uses internal iterator safely. Preferred.
>
> **Option 2 — `iterator.remove()`:**
> ```
> Iterator<String> it = list.iterator();
> while (it.hasNext()) {
>     if (it.next().startsWith("ERROR")) it.remove();
> }
> ```
> Classic, always correct, works on Java 5+.
>
> **Option 3 — collect into new list:**
> ```
> List<String> result = list.stream()
>     .filter(s -> !s.startsWith("ERROR"))
>     .collect(Collectors.toList());
> ```
> Creates a new list — original unchanged.
>
> **Never do this:**
> ```
> for (String s : list) {
>     if (s.startsWith("ERROR")) list.remove(s); // CME!
> }
> ```

---

**Q5. What is `IllegalStateException` in the context of Iterator and when is it thrown?**

> `Iterator.remove()` throws `IllegalStateException` in two cases:
>
> 1. `remove()` is called before any `next()` call — there is no "last returned
     >    element" to remove yet:
> ```
> Iterator<String> it = list.iterator();
> it.remove(); // IllegalStateException — no next() called first
> ```
>
> 2. `remove()` is called twice without an intervening `next()` call:
> ```
> Iterator<String> it = list.iterator();
> it.next();
> it.remove(); // OK
> it.remove(); // IllegalStateException — already removed, no new next()
> ```
>
> The same rule applies to `ListIterator.set()` and `ListIterator.add()` —
> `set()` requires a preceding `next()` or `previous()`. `add()` can be called
> at any time but moves the cursor, so a subsequent `remove()` without `next()`
> would also fail.

---

## ListIterator

**Q6. What can `ListIterator` do that `Iterator` cannot? Give a real use case for each capability.**

> Four additional capabilities:
>
> **1. Backward traversal** — `hasPrevious()` / `previous()`:
> ```
> // Use case: find last occurrence of a value
> ListIterator<String> it = list.listIterator(list.size());
> while (it.hasPrevious()) {
>     if (it.previous().equals("ERROR")) { // found last occurrence
>         System.out.println("Last ERROR at index: " + it.nextIndex());
>         break;
>     }
> }
> ```
>
> **2. In-place replacement** — `set(e)`:
> ```
> // Use case: normalise all strings to uppercase without creating new list
> ListIterator<String> it = list.listIterator();
> while (it.hasNext()) it.set(it.next().toUpperCase());
> ```
>
> **3. Insert at cursor** — `add(e)`:
> ```
> // Use case: insert separator between adjacent elements
> ListIterator<String> it = list.listIterator();
> while (it.hasNext()) {
>     it.next();
>     if (it.hasNext()) it.add("---"); // insert after current element
> }
> ```
>
> **4. Index awareness** — `nextIndex()` / `previousIndex()`:
> ```
> // Use case: find index of first element matching predicate
> ListIterator<String> it = list.listIterator();
> while (it.hasNext()) {
>     int idx = it.nextIndex();
>     if (it.next().startsWith("X")) {
>         System.out.println("First X at index: " + idx);
>         break;
>     }
> }
> ```

---

## Custom Iterable

**Q7. How do you make a custom class work in a for-each loop? What is the minimum you need to implement?**

> Implement `Iterable<T>` and return an `Iterator<T>` from `iterator()`.
> The Iterator needs `hasNext()` and `next()` at minimum:
>
> ```
> class NumberRange implements Iterable<Integer> {
>     private final int start, end;
>
>     NumberRange(int start, int end) {
>         this.start = start;
>         this.end = end;
>     }
>
>     @Override
>     public Iterator<Integer> iterator() {
>         return new Iterator<Integer>() {
>             int current = start;
>
>             @Override
>             public boolean hasNext() { return current <= end; }
>
>             @Override
>             public Integer next() {
>                 if (!hasNext()) throw new NoSuchElementException();
>                 return current++;
>             }
>         };
>     }
> }
>
> // Now works in for-each
> for (int n : new NumberRange(1, 10)) System.out.print(n + " ");
> ```
>
> Key design rules:
> - `iterator()` must return a **fresh** iterator each time — not the same instance
> - `next()` must throw `NoSuchElementException` when exhausted — not return null
> - `hasNext()` must be idempotent — calling it multiple times without `next()` must be safe

---

**Q8. Design a `PaginatedIterator` that lazily pages through a large database result. What interface do you implement and why lazy?**

> ```
> class PaginatedIterator<T> implements Iterator<T> {
>     private final Function<Integer, List<T>> pageLoader; // page number → data
>     private final int pageSize;
>     private List<T> currentPage = Collections.emptyList();
>     private int pageNumber = 0;
>     private int indexInPage = 0;
>     private boolean exhausted = false;
>
>     PaginatedIterator(Function<Integer, List<T>> pageLoader, int pageSize) {
>         this.pageLoader = pageLoader;
>         this.pageSize = pageSize;
>         loadNextPage();
>     }
>
>     @Override
>     public boolean hasNext() {
>         return !exhausted && indexInPage < currentPage.size();
>     }
>
>     @Override
>     public T next() {
>         if (!hasNext()) throw new NoSuchElementException();
>         T item = currentPage.get(indexInPage++);
>         if (indexInPage >= currentPage.size()) loadNextPage();
>         return item;
>     }
>
>     private void loadNextPage() {
>         currentPage = pageLoader.apply(pageNumber++);
>         indexInPage = 0;
>         if (currentPage.size() < pageSize) exhausted = true;
>     }
> }
> ```
>
> Why lazy: loading all rows upfront for a 10M-row table would exhaust memory.
> The lazy iterator loads one page at a time, processes it, discards it, then
> loads the next. Memory usage stays constant at one page size regardless of
> total dataset size. This is the pattern used by JPA's `ScrollableResults` and
> Spring Data's `Stream<T>` query methods.

---

## Fail-Fast vs Fail-Safe

**Q9. Is `ConcurrentModificationException` guaranteed to be thrown when you modify a collection during iteration?**

> No — the Javadoc explicitly states it is thrown on a **best-effort basis**.
> It should not be relied upon for correctness.
>
> In a single-threaded context, CME is very consistent because the modCount
> check runs on every `next()`. But:
>
> 1. **Not all modifications trigger it** — calling `iterator.remove()` is
     >    allowed and updates `expectedModCount` internally.
>
> 2. **In multi-threaded scenarios**, the modCount check is not atomic. Thread A
     >    can modify the collection between Thread B's `hasNext()` and `next()` calls
     >    without CME — resulting in corrupted data, wrong elements, or `ArrayIndexOutOfBoundsException`
     >    instead of CME.
>
> 3. **Timing-dependent** — in concurrent scenarios, you may or may not see CME
     >    depending on thread scheduling.
>
> The correct solution for concurrent access is `ConcurrentHashMap`,
> `CopyOnWriteArrayList`, or external synchronisation — not catching CME.

---

**Q10. `CopyOnWriteArrayList` — when would you use it and when would you not?**

> **Use when:**
> - Reads vastly outnumber writes (e.g., a list of event listeners)
> - You need lock-free iteration — no CME ever
> - The list is small (copy cost is low)
> - Write frequency is very low (config, feature flags, subscriber lists)
>
> **Do not use when:**
> - Writes are frequent — every `add()` or `remove()` creates a full array copy
    >   → O(n) per write, heavy GC pressure
> - The list is large — copying 100k elements on every write is prohibitive
> - You need to see in-progress writes during iteration — iterator sees snapshot only
>
> Production example where it fits perfectly: a list of `ApplicationListener`
> objects in Spring. Listeners are registered at startup and rarely change,
> but events are published thousands of times per second requiring lock-free
> iteration.

---

## Senior Level

**Q11. Design a BST Iterator that returns elements in sorted order. It should use O(h) space, not O(n).**

> The naive approach stores all elements in a list during construction — O(n)
> space. The optimal approach uses an explicit stack to simulate inorder
> traversal lazily:
>
> ```
> class BSTIterator implements Iterator<Integer> {
>     private final Deque<TreeNode> stack = new ArrayDeque<>();
>
>     BSTIterator(TreeNode root) {
>         pushLeft(root); // push entire left spine onto stack
>     }
>
>     @Override
>     public boolean hasNext() { return !stack.isEmpty(); }
>
>     @Override
>     public Integer next() {
>         TreeNode node = stack.pop();       // next inorder node
>         pushLeft(node.right);              // prepare right subtree
>         return node.val;
>     }
>
>     private void pushLeft(TreeNode node) {
>         while (node != null) {
>             stack.push(node);
>             node = node.left;
>         }
>     }
> }
> ```
>
> Space: O(h) where h = tree height — only the left spine is on the stack
> at any time. For a balanced BST: O(log n). For a skewed tree: O(n) worst case.
>
> Time: O(h) amortized per `next()` — each node is pushed and popped exactly once
> across all calls, so O(n) total for n calls = O(1) amortized.
>
> This pattern — lazy tree traversal using an explicit stack — is used in
> database index scans (B-tree range scan), file system traversal, and
> XML/JSON streaming parsers.

---

**Q12. What happens if you call `next()` on an exhausted iterator? How should a correct custom iterator handle this?**

> On standard Java iterators (`ArrayList`, `HashMap` etc.), calling `next()`
> when `hasNext()` is false throws `NoSuchElementException`.
>
> A correct custom iterator must do the same:
> ```
> @Override
> public T next() {
>     if (!hasNext()) throw new NoSuchElementException(); // mandatory
>     // ... return element
> }
> ```
>
> Never return `null` from `next()` when exhausted — callers cannot distinguish
> a legitimate null element from "no more elements". Never return a sentinel
> value. The Iterator contract requires `NoSuchElementException`.
>
> Also: `hasNext()` must be **idempotent** — calling it 5 times in a row without
> calling `next()` must return the same result each time and must not advance
> the cursor. This is frequently violated in naive implementations that load
> the next element inside `hasNext()`.

---

**Q13. What is the difference between `Iterable` and `Iterator`? Why are they separate interfaces?**

> `Iterable<T>` — represents a collection that **can be iterated**. Has one
> method: `iterator()`. Returns a fresh `Iterator` each time it is called.
> A class that implements `Iterable` can appear in a for-each loop.
>
> `Iterator<T>` — represents an **active traversal in progress**. Stateful —
> tracks current position. Has `hasNext()`, `next()`, `remove()`.
>
> They are separate because:
> 1. **Multiple concurrent iterations** — a single `Iterable` can produce
     >    multiple independent `Iterator` instances simultaneously, each with
     >    their own cursor:
     >    ```java
>    // Two independent iterations over the same list
>    Iterator<String> it1 = list.iterator(); // cursor at 0
>    Iterator<String> it2 = list.iterator(); // cursor at 0 independently
>    it1.next(); // cursor 1 at 1
>    it2.next(); // cursor 2 still at 0
>    ```
> 2. **Separation of concerns** — `Iterable` is about the data source,
     >    `Iterator` is about traversal state. Merging them would prevent
     >    re-iteration and concurrent iteration.

---

**Q14. You implement `remove()` in a custom Iterator. What state must you maintain and what must you reset?**

> You need to track:
> 1. Whether `next()` has been called since the last `remove()` — throw
     >    `IllegalStateException` if `remove()` is called without a preceding `next()`
> 2. The position of the last returned element — to know what to remove
>
> ```
> class SafeIterator<T> implements Iterator<T> {
>     private final List<T> list;
>     private int cursor = 0;
>     private int lastReturned = -1; // -1 = no element returned yet
>
>     SafeIterator(List<T> list) { this.list = list; }
>
>     @Override
>     public boolean hasNext() { return cursor < list.size(); }
>
>     @Override
>     public T next() {
>         if (!hasNext()) throw new NoSuchElementException();
>         lastReturned = cursor;
>         return list.get(cursor++);
>     }
>
>     @Override
>     public void remove() {
>         if (lastReturned < 0)
>             throw new IllegalStateException("Call next() before remove()");
>         list.remove(lastReturned);
>         cursor = lastReturned;  // adjust cursor — list shifted left
>         lastReturned = -1;      // reset — prevent double remove
>     }
> }
> ```

---

**Q15. How does `Iterator.remove()` differ from `Collection.remove()` during iteration, and what is the performance difference for ArrayList?**

> `Collection.remove(element)` — called on the collection directly during
> iteration. Triggers a structural modification, increments `modCount`,
> and causes CME on the next `Iterator.next()` call. Also O(n) to find the
> element + O(n) to shift the array.
>
> `Iterator.remove()` — removes the last element returned by `next()`.
> Does NOT increment `modCount` in the way that triggers CME — instead it
> updates both `modCount` and `expectedModCount` together, keeping them
> in sync. Internally uses the cursor position — no linear scan needed.
>
> Performance for ArrayList:
> - Both are O(n) for the array shift (elements after removal must move left)
> - `Iterator.remove()` skips the O(n) element search since it already knows
    >   the index from the cursor
> - `Collection.remove(element)` does O(n) scan + O(n) shift = effectively 2×
>
> For LinkedList:
> - `Iterator.remove()` is O(1) — just re-link pointers at current cursor position
> - `Collection.remove(element)` is O(n) to find the node first