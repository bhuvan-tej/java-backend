# 🔵 Set

> Covers `HashSet`, `LinkedHashSet`, and `TreeSet` — the three Set
> implementations you'll use in production. No duplicates. Each trades
> order and speed differently.

---

## 🧠 Mental Model

```
HashSet                      LinkedHashSet                TreeSet
──────────────────────────   ──────────────────────────   ──────────────────────────
[ bucket 0 ] → "java"        "banana" → "apple" →            1 ← root
[ bucket 1 ] → "docker"      "cherry"                       / \
[ bucket 3 ] → "spring"                                    0   3
                             Insertion order              / \   \
No order guarantee           always preserved            -1  2   5
hashCode → bucket
equals  → uniqueness         hashCode + linked list      compareTo → position
                                                         compareTo → uniqueness
O(1) add/remove/contains     O(1) add/remove/contains    O(log n) all ops
```

---

## 📄 Classes in this Module

### `HashSetSamples.java`

| Example       | What it covers |
|---------------|----------------|
| Foundational  | Blog tags — add, contains, union, intersection, difference, deduplication |
| Adv Level     | Role-based permission system — O(1) permission checks, containsAll, set algebra |
| Contract Demo | Broken vs correct hashCode/equals — what fails silently without it |

**Key methods:**
```
set.add(element)             // O(1) — returns false if already present
set.remove(element)          // O(1)
set.contains(element)        // O(1) — HashSet's biggest strength
set.addAll(other)            // UNION — adds all elements from other
set.retainAll(other)         // INTERSECTION — keeps only elements in both
set.removeAll(other)         // DIFFERENCE — removes all elements in other
set.containsAll(other)       // true if this set contains every element of other
newHashSet<>(list)          // deduplicate a List in one line
```

**When to use HashSet:**
```
✅ O(1) membership check — "does this set contain X?"
✅ Eliminate duplicates from a collection
✅ Set algebra — union, intersection, difference
✅ Replacing a List<String> used only for contains() checks

❌ Need sorted iteration                 → TreeSet
❌ Need insertion order preserved        → LinkedHashSet
❌ Thread safety needed                  → CopyOnWriteArraySet
```

---

### `LinkedHashSetSamples.java`

| Example      | What it covers |
|--------------|----------------|
| Foundational | Order comparison vs HashSet, event deduplication preserving first-seen order |
| Adv Level    | Recently viewed tracker — remove + re-add pattern for recency ordering |

**Key methods:**
```
// All HashSet methods — plus guaranteed insertion order on iteration
set.iterator().next()        // always returns the FIRST inserted element
```

**When to use LinkedHashSet:**
```
✅ Need uniqueness AND insertion order preserved
✅ Deduplication while keeping first-seen order
✅ Recently-viewed / recently-used tracking

❌ Need sorted order                     → TreeSet
❌ Don't care about order               → HashSet (slightly less memory)
```

---

### `TreeSetSamples.java`

| Example      | What it covers |
|--------------|----------------|
| Foundational | Leaderboard — first, last, floor, ceiling, lower, higher, subSet, headSet, tailSet, descendingSet |
| Adv Level    | Stock price range finder + Custom Comparator TreeSet (multi-field sort) |

**Key methods:**
```
set.first()                  // smallest element — O(log n)
set.last()                   // largest element — O(log n)
set.floor(x)                 // largest element ≤ x
set.ceiling(x)               // smallest element ≥ x
set.lower(x)                 // largest element strictly < x
set.higher(x)                // smallest element strictly > x
set.subSet(from, fInc, to, tInc)  // range view — backed by original
set.headSet(to)              // all elements strictly less than to
set.tailSet(from)            // all elements ≥ from
set.descendingSet()          // reverse-order view
set.pollFirst()              // remove and return smallest — O(log n)
set.pollLast()               // remove and return largest — O(log n)
```

**When to use TreeSet:**
```
✅ Need sorted iteration at all times
✅ Range queries — "all elements between X and Y"
✅ Nearest-element lookups — floor / ceiling / lower / higher
✅ Leaderboards, price tiers, time-window queries

❌ Order doesn't matter                  → HashSet (O(1) vs O(log n))
❌ Only insertion order needed           → LinkedHashSet
```

---

## ⚡ HashSet vs LinkedHashSet vs TreeSet

| | HashSet | LinkedHashSet | TreeSet |
|---|---|---|---|
| Internal structure | HashMap | HashMap + LinkedList | Red-Black Tree |
| Order | None | Insertion | Sorted |
| `add` / `remove` / `contains` | O(1) | O(1) | O(log n) |
| Uniqueness via | hashCode + equals | hashCode + equals | compareTo / Comparator |
| Null allowed | ✅ one null | ✅ one null | ❌ throws NPE |
| NavigableSet ops | ❌ | ❌ | ✅ floor, ceiling, range |
| Memory | Less | More (linked list) | More (tree nodes) |

---

## 🔑 Common Mistakes

```
// ❌ WRONG — missing hashCode/equals → duplicates slip through
class Employee {
    int id; String name;
    // No override → uses Object identity → always "different"
}
Set set = new HashSet<>();
set.add(new Employee(1, "Alice"));
set.add(new Employee(1, "Alice")); // NOT detected as duplicate!
System.out.println(set.size()); // 2 — BUG

// ✅ CORRECT — implement both
@Override public boolean equals(Object o) {
    return o instanceof Employee && this.id == ((Employee) o).id;
}
@Override public int hashCode() {
    return Objects.hash(id); // same field as equals!
}

// ❌ WRONG — mutating an object after adding to HashSet
Employee e = new Employee(1, "Alice");
set.add(e);
e.id = 999; // hashCode changes → lost in wrong bucket → get() returns null!

// ❌ WRONG — TreeSet with Comparator returning 0 for different objects
TreeSet products = new TreeSet<>(
    Comparator.comparingDouble(Product::getPrice)); // price only
products.add(new Product("Phone",  45_000));
products.add(new Product("Tablet", 45_000)); // compareTo returns 0 → rejected!
// Fix: add secondary sort field so same-price items are kept
TreeSet products = new TreeSet<>(
    Comparator.comparingDouble(Product::getPrice)
              .thenComparing(Product::getName));
```

---