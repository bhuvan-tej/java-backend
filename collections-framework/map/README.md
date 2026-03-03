# 🗂️ Map

> Covers `HashMap`, `LinkedHashMap`, `TreeMap`, and `ConcurrentHashMap` —
> the four Map implementations you'll use in production. Every one trades
> speed, order, and thread safety differently.

---

## 🧠 Mental Model

```
HashMap                    LinkedHashMap              TreeMap
──────────────────────     ──────────────────────     ──────────────────────
[ bucket 0 ] → k1,v1       k1 ⟷ k2 ⟷ k3 ⟷ k4           k2 ← root
[ bucket 1 ] → k2,v2                                   /      \
[ bucket 3 ] → k3,v3       Insertion order            k1       k4
                           always preserved          /  \
No order guarantee         HashMap speed +          k0   k3
hashCode → bucket          linked list overhead
equals  → key match                                  compareTo → position
                                                     O(log n) all ops
O(1) avg all ops            O(1) avg all ops

ConcurrentHashMap
──────────────────────────────────────────────────────
Same structure as HashMap BUT:
  • Reads  → lock-free (CAS)
  • Writes → lock per bucket only (not the whole map)
  • null keys / values → NOT allowed
  • compute / merge / putIfAbsent → atomic per key
```

---

## 📄 Classes in this Module

### `HashMapSamples.java`

| Example      | What it covers |
|--------------|----------------|
| Foundational | Word frequency — merge, getOrDefault, putIfAbsent, entrySet iteration |
| Adv Level    | computeIfAbsent grouping, computeIfPresent, compute, groupingBy, partitioningBy, replaceAll, pre-sizing |

**Key methods:**
```
map.put(k, v)                    // O(1) — returns OLD value or null
map.get(k)                       // O(1) — returns null if missing
map.getOrDefault(k, fallback)    // O(1) — safe, no NPE risk
map.putIfAbsent(k, v)            // atomic — only puts if key absent
map.merge(k, v, BiFunction)      // atomic — absent→store v, present→apply fn
map.compute(k, BiFunction)       // atomic — always called, null→removes key
map.computeIfAbsent(k, Function) // atomic — only if key absent
map.computeIfPresent(k, BiFunction) // atomic — only if key present
map.replace(k, oldV, newV)       // atomic CAS — only replaces if value matches
map.replaceAll(BiFunction)       // bulk in-place value transform
map.entrySet()                   // iterate both key and value
map.keySet()                     // iterate keys only
map.values()                     // iterate values only
map.forEach(BiConsumer)          // Java 8+ clean iteration
```

**When to use HashMap:**
```
✅ Fastest key-value lookup               → O(1) average
✅ Counting / frequency maps              → merge()
✅ Grouping / caching                     → computeIfAbsent()
✅ Single-threaded or externally synced

❌ Need sorted keys                        → TreeMap
❌ Need insertion order                    → LinkedHashMap
❌ Concurrent writes from multiple threads → ConcurrentHashMap
```

---

### `LinkedHashMapSamples.java`

| Example      | What it covers |
|--------------|----------------|
| Foundational | Config properties — insertion order vs HashMap randomness |
| Adv Level    | LRU Cache — accessOrder=true, removeEldestEntry, MRU/LRU eviction |

**Key methods:**
```
// Constructor flags
new LinkedHashMap<>()                     // insertion order (default)
new LinkedHashMap<>(cap, loadFactor, true) // access order — LRU mode

// LRU hook
protected boolean removeEldestEntry(Map.Entry eldest) {
    return size() > capacity; // return true → auto-evict LRU
}
```

**When to use LinkedHashMap:**
```
✅ Need HashMap speed + predictable iteration order
✅ Config / pipeline where insertion order matters
✅ LRU Cache — accessOrder=true + removeEldestEntry()
✅ Deduplication preserving first-seen order

❌ Need sorted keys                        → TreeMap
❌ Thread safety needed                    → ConcurrentHashMap
❌ Production LRU with TTL / metrics       → Caffeine cache
```

---

### `TreeMapSamples.java`

| Example      | What it covers |
|--------------|----------------|
| Foundational | Student grades — firstKey, lastKey, tailMap, headMap, subMap, floorKey, ceilingKey, descendingMap |
| Adv Level    | Tax bracket calculator (floorKey) + time-series event log (tailMap, floorEntry, higherEntry, pollFirstEntry) |

**Key methods:**
```
map.firstKey()                   // smallest key — O(log n)
map.lastKey()                    // largest key — O(log n)
map.floorKey(k)                  // largest key ≤ k
map.ceilingKey(k)                // smallest key ≥ k
map.lowerKey(k)                  // largest key strictly < k
map.higherKey(k)                 // smallest key strictly > k
map.floorEntry(k)                // Map.Entry with largest key ≤ k
map.higherEntry(k)               // Map.Entry with smallest key > k
map.headMap(to)                  // all entries with key < to (view)
map.tailMap(from)                // all entries with key ≥ from (view)
map.subMap(from, fInc, to, tInc) // range view with inclusive flags
map.descendingMap()              // reverse-order view
map.pollFirstEntry()             // remove + return smallest key entry
map.pollLastEntry()              // remove + return largest key entry
```

**When to use TreeMap:**
```
✅ Keys must always be in sorted order
✅ Range queries — "all entries between X and Y"
✅ Bracket lookups — tax tiers, price bands, rate tables
✅ Time-series data with timestamp keys
✅ Nearest-key lookups — floor / ceiling

❌ Order doesn't matter                    → HashMap (O(1) vs O(log n))
❌ Need insertion order                    → LinkedHashMap
❌ null keys needed                        → TreeMap throws NPE on null keys
```

---

### `ConcurrentHashMapSamples.java`

| Example      | What it covers |
|--------------|----------------|
| Foundational | Null rejection, putIfAbsent, replace (CAS), merge for atomic counting |
| Adv Level    | Multi-threaded endpoint counter, compute per key, search, reduceValues, newKeySet |

**Key methods:**
```
map.putIfAbsent(k, v)            // atomic — no race condition
map.replace(k, oldV, newV)       // atomic CAS — only replaces if matches
map.merge(k, v, fn)              // atomic — safe concurrent aggregation
map.compute(k, fn)               // atomic per-key read-modify-write
map.computeIfAbsent(k, fn)       // atomic — safe lazy initialisation
map.search(parallelism, fn)      // parallel search — first non-null result
map.reduceValues(parallelism, fn)// parallel reduce on values
map.forEach(parallelism, fn)     // parallel iteration
ConcurrentHashMap.newKeySet()    // thread-safe Set backed by CHM
```

**When to use ConcurrentHashMap:**
```
✅ Multiple threads reading and writing concurrently
✅ Concurrent counters and aggregations
✅ Shared caches in multi-threaded services
✅ Thread-safe Set via newKeySet()

❌ Need sorted keys                        → use external locking with TreeMap
❌ Need insertion order                    → no concurrent ordered map in JDK
❌ null keys or values needed              → use Optional or sentinel values
```

---

## ⚡ HashMap vs LinkedHashMap vs TreeMap vs ConcurrentHashMap

| | HashMap | LinkedHashMap | TreeMap | ConcurrentHashMap |
|---|---|---|---|---|
| Internal structure | Array + linked list / tree | HashMap + linked list | Red-Black Tree | Array + CAS + bucket locks |
| Order | None | Insertion / Access | Sorted (keys) | None |
| `get` / `put` | O(1) | O(1) | O(log n) | O(1) |
| Null keys | ✅ one | ✅ one | ❌ NPE | ❌ NPE |
| Null values | ✅ | ✅ | ✅ | ❌ NPE |
| Thread safe | ❌ | ❌ | ❌ | ✅ |
| NavigableMap ops | ❌ | ❌ | ✅ | ❌ |
| LRU Cache | ❌ | ✅ accessOrder | ❌ | ❌ |

---

## 🔑 Common Mistakes

```
// ❌ WRONG — non-atomic check-then-put (race condition)
if (!map.containsKey(k)) {
    map.put(k, new ArrayList<>());  // another thread may also be here!
}
map.get(k).add(value);

// ✅ CORRECT — one atomic call
map.computeIfAbsent(k, key -> new ArrayList<>()).add(value);

// ❌ WRONG — non-atomic read-modify-write
map.put(k, map.getOrDefault(k, 0) + 1); // race condition in CHM

// ✅ CORRECT — atomic
map.merge(k, 1, Integer::sum);

// ❌ WRONG — iterating and modifying HashMap simultaneously
for (Map.Entry e : map.entrySet()) {
    if (e.getValue() < 0) map.remove(e.getKey()); // CME!
}

// ✅ CORRECT — entrySet removeIf or iterator.remove()
map.entrySet().removeIf(e -> e.getValue() < 0);

// ❌ WRONG — null value in ConcurrentHashMap
chm.put("key", null); // NullPointerException!

// ✅ CORRECT — use Optional or sentinel
chm.put("key", Optional.empty());    // wrap null in Optional
chm.put("key", "NONE");              // or use a sentinel string
```

---