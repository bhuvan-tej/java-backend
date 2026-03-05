# 🛠️ Utils

> Covers `java.util.Collections` — the utility class with static methods
> for sorting, searching, modifying, and wrapping collections.
> Not to be confused with `Collection` (the interface).

---

## 🧠 Mental Model

```
java.util.Collections  (capital C, utility class — all static methods)
│
├── Sorting & Searching
│     sort()         binarySearch()      min()       max()
│     shuffle()      frequency()         reverseOrder()
│
├── Modification
│     reverse()      rotate()            swap()
│     fill()         copy()              nCopies()
│     replaceAll()   disjoint()
│
└── Wrapping
      unmodifiableList/Set/Map()         → read-only VIEW
      synchronizedList/Set/Map()         → thread-safe wrapper
      singletonList/Set/Map()            → immutable single-element
      emptyList/Set/Map()                → immutable empty (cached)
      checkedList/Set/Map()              → runtime type safety
```

---

## 📄 Classes in this Module

### `CollectionsUtilSamples.java`

| Example | What it covers |
|---------|----------------|
| Sorting & Searching | sort, reverseOrder, binarySearch, min, max, frequency, shuffle |
| Modification | reverse, rotate, swap, fill, copy, nCopies, disjoint |
| Wrapping | unmodifiableList, singletonList, emptyList, synchronizedList |
| Senior Level | Return empty not null, defensive unmodifiable return, nCopies board init, rotate for scheduling, disjoint for access control |

---

## ⚡ Key Methods

```
// ── Sorting ───────────────────────────────────────────────────
Collections.sort(list)                        // natural order, stable TimSort O(n log n)
Collections.sort(list, comparator)            // custom order
list.sort(comparator)                         // preferred in Java 8+
Collections.shuffle(list)                     // random permutation O(n)
Collections.shuffle(list, random)             // seeded shuffle — reproducible

// ── Searching ─────────────────────────────────────────────────
Collections.binarySearch(list, key)           // O(log n) — list MUST be sorted first
Collections.binarySearch(list, key, comp)     // with Comparator
Collections.min(collection)                   // O(n) linear scan
Collections.max(collection)                   // O(n) linear scan
Collections.min(collection, comparator)       // custom comparison
Collections.frequency(collection, element)    // count occurrences O(n)

// ── Modification ──────────────────────────────────────────────
Collections.reverse(list)                     // in-place O(n)
Collections.rotate(list, distance)            // shift right by distance O(n)
Collections.swap(list, i, j)                  // swap two indices O(1)
Collections.fill(list, element)               // replace all elements O(n)
Collections.copy(dest, src)                   // copy src into dest (dest.size >= src.size)
Collections.nCopies(n, element)               // immutable list of n copies
Collections.disjoint(c1, c2)                  // true if no common elements O(n)

// ── Wrapping ──────────────────────────────────────────────────
Collections.unmodifiableList(list)            // read-only VIEW of list
Collections.unmodifiableSet(set)              // read-only VIEW of set
Collections.unmodifiableMap(map)              // read-only VIEW of map
Collections.synchronizedList(list)            // thread-safe wrapper (single lock)
Collections.singletonList(element)            // immutable 1-element list
Collections.singleton(element)               // immutable 1-element set
Collections.singletonMap(key, value)          // immutable 1-entry map
Collections.emptyList()                       // immutable empty list (cached)
Collections.emptySet()                        // immutable empty set (cached)
Collections.emptyMap()                        // immutable empty map (cached)
```

---

## 🔑 Production Patterns

```
// ── Return empty, never null ──────────────────────────────────
// BAD — caller must null-check every time
public List<Order> getOrders(String userId) {
    if (!exists(userId)) return null; // forces NPE if caller forgets to check
}

// GOOD — callers can always iterate safely
public List<Order> getOrders(String userId) {
    if (!exists(userId)) return Collections.emptyList(); // zero allocation
}

// ── Defensive unmodifiable return ────────────────────────────
public List<String> getConfig() {
    return Collections.unmodifiableList(internalConfig);
}
// Caller gets UnsupportedOperationException instead of silently corrupting state

// ── nCopies for initialisation ───────────────────────────────
// Pre-fill a list with default values
List<String> statuses = new ArrayList<>(Collections.nCopies(100, "PENDING"));

// 2D board initialised to "."
List<List<String>> board = new ArrayList<>();
for (int i = 0; i < 3; i++)
    board.add(new ArrayList<>(Collections.nCopies(3, ".")));

// ── rotate for round-robin scheduling ────────────────────────
List<String> rota = new ArrayList<>(Arrays.asList("Alice","Bob","Charlie"));
Collections.rotate(rota, -1); // advance — Alice goes to end, Bob is now first

// ── disjoint for permission checks ───────────────────────────
boolean hasAccess = !Collections.disjoint(userPermissions, requiredPermissions);
```

---

## ⚡ Common Gotchas

```
// ❌ GOTCHA 1 — binarySearch on unsorted list → undefined result (not exception)
List<Integer> list = Arrays.asList(5, 2, 8, 1);
Collections.binarySearch(list, 5); // could return wrong index or -1!
// Always sort before binarySearch

// ❌ GOTCHA 2 — unmodifiableList is a VIEW, not a copy
List<String> mutable  = new ArrayList<>(Arrays.asList("A","B"));
List<String> readOnly = Collections.unmodifiableList(mutable);
mutable.add("C");
System.out.println(readOnly); // [A, B, C] — change reflected!
// Fix: copy first if you need true isolation
List<String> isolated = Collections.unmodifiableList(new ArrayList<>(mutable));

// ❌ GOTCHA 3 — synchronizedList still needs external lock for iteration
List<String> synced = Collections.synchronizedList(new ArrayList<>());
// BAD — CME possible if another thread modifies during iteration
for (String s : synced) { ... }
// GOOD — manually lock the list object
synchronized (synced) {
    for (String s : synced) { ... }
}

// ❌ GOTCHA 4 — copy() requires dest to be at least as large as src
List<String> src  = Arrays.asList("X","Y","Z");
List<String> dest = new ArrayList<>();  // size 0
Collections.copy(dest, src); // IndexOutOfBoundsException!
// Fix: pre-size dest
List<String> dest = new ArrayList<>(Collections.nCopies(src.size(), null));
Collections.copy(dest, src);

// ❌ GOTCHA 5 — nCopies returns an immutable list
List<String> list = Collections.nCopies(5, "X");
list.set(0, "Y"); // UnsupportedOperationException!
// Fix: wrap in ArrayList
List<String> list = new ArrayList<>(Collections.nCopies(5, "X"));
list.set(0, "Y"); // works
```

---

## ⚡ Collections vs Arrays vs List.of (Java 9+)

| | `Collections.emptyList()` | `Arrays.asList()` | `List.of()` (Java 9+) |
|---|---|---|---|
| Mutable? | ❌ | set() only | ❌ |
| Null elements | ✅ | ✅ | ❌ NPE |
| Backed by array | No | Yes | No |
| Use case | Return empty safely | Fixed-size mutable wrapper | Immutable constants |

---