# 🗂️ Concurrent Collections

> Thread-safe collections built for concurrency — not just synchronized
> wrappers. Each is optimised for a specific access pattern.

---

## 🧠 Why Not Just Synchronize?

`Collections.synchronizedMap(map)` puts a single lock around every operation.
One thread at a time for reads AND writes. Iteration still requires external
synchronization or you get `ConcurrentModificationException`.

Concurrent collections solve this with fine-grained locking, lock-free
algorithms, or copy-on-write — much higher throughput under contention.

---

## 📄 Classes in this Module

### `ConcurrentCollectionSamples.java`

| Example | What it covers |
|---------|----------------|
| ConcurrentHashMap | putIfAbsent, compute, merge, bulk ops, word count |
| CopyOnWriteArrayList | snapshot iterators, lock-free reads, write cost |
| BlockingQueue | producer-consumer, backpressure, queue variants |
| ConcurrentLinkedQueue | non-blocking poll/offer, vs BlockingQueue |
| Senior Level | frequency counter, concurrent cache, pipeline pattern |

---

## ⚡ ConcurrentHashMap

```
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

// Atomic check-then-act — no external synchronization needed
map.putIfAbsent("key", 1);                      // insert only if absent
map.computeIfAbsent("key", k -> expensive(k));  // compute only if absent
map.compute("key", (k, v) -> v == null ? 1 : v + 1); // atomic read-modify-write
map.merge("key", 1, Integer::sum);              // combine existing + new value

// Bulk operations — parallel with threshold
map.forEach(1, (k, v) -> process(k, v));        // threshold=1 → always parallel
int total = map.reduceValues(1, Integer::sum);  // parallel reduce
String found = map.search(1, (k, v) -> v > 90 ? k : null); // first match
```

**How it works:**
- Java 8+: CAS on individual bins, synchronized only on first insert per bucket
- Reads are almost always lock-free
- Writes lock only the affected bucket — 16+ concurrent writers by default
- `size()` is approximate under concurrency — use `mappingCount()` for large maps

**NOT suitable for:**
- Compound check-then-act outside of `compute`/`merge` — those need external sync
- Iterating while guaranteeing a consistent snapshot — use `forEach` bulk op instead

---

## ⚡ CopyOnWriteArrayList

```
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();

// Reads are lock-free — multiple threads read simultaneously
list.get(0);
list.iterator(); // snapshot at creation time — never throws CME

// Writes copy the entire array — expensive
list.add("item");     // O(n) copy
list.remove("item");  // O(n) copy

// Iterator sees snapshot — not affected by concurrent modifications
Iterator<String> it = list.iterator();
list.add("new");       // doesn't affect 'it'
list.remove("old");    // doesn't affect 'it'
while (it.hasNext()) it.next(); // safe — sees original snapshot
```

**Best fit:** small lists that are read frequently and written rarely.
- Listener/observer registries
- Plugin or handler lists
- Feature flag lists

**Poor fit:** large lists or frequent writes — every write copies the whole array.

---

## ⚡ BlockingQueue

```
// put — blocks if queue is full (natural backpressure)
queue.put(item);

// take — blocks if queue is empty
Item item = queue.take();

// offer — non-blocking, returns false if full
boolean added = queue.offer(item);
boolean added = queue.offer(item, 100, MILLISECONDS); // with timeout

// poll — non-blocking, returns null if empty
Item item = queue.poll();
Item item = queue.poll(100, MILLISECONDS); // with timeout
```

**Variants:**

| Type | Bounded | Notes |
|------|---------|-------|
| `ArrayBlockingQueue` | ✅ | Single lock, optional fairness |
| `LinkedBlockingQueue` | Optional | Separate head/tail locks — higher throughput |
| `PriorityBlockingQueue` | ❌ | Tasks ordered by `Comparator` or `Comparable` |
| `SynchronousQueue` | 0 capacity | Direct hand-off — used in `newCachedThreadPool` |
| `DelayQueue` | ❌ | Tasks available only after delay expires |

---

## ⚡ ConcurrentLinkedQueue

```
ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();

queue.offer("item");    // non-blocking add — always returns true
queue.poll();           // non-blocking remove — returns null if empty
queue.peek();           // non-blocking read — no remove

// size() is O(n) — avoid in hot paths
// isEmpty() is O(1) — use this instead
```

**vs BlockingQueue:**
- `ConcurrentLinkedQueue` — never blocks, caller must handle empty/retry
- `BlockingQueue` — blocks caller until space or item is available

Use CLQ when producers/consumers must never block — event loops, reactive pipelines.
Use BQ when natural backpressure is desired — thread pools, processing pipelines.

---

## 🔑 Collection Comparison

| Collection | Reads | Writes | Blocks | Best For |
|-----------|-------|--------|--------|----------|
| `ConcurrentHashMap` | Lock-free | Fine-grained lock | ❌ | Shared maps, caches, counters |
| `CopyOnWriteArrayList` | Lock-free | Copy array | ❌ | Listener lists, rare writes |
| `LinkedBlockingQueue` | Locked | Locked | ✅ | Producer-consumer pipelines |
| `ConcurrentLinkedQueue` | Lock-free | CAS | ❌ | Non-blocking queues |
| `synchronizedMap` | Coarse lock | Coarse lock | ❌ | Avoid — use CHM instead |

---

## 🔑 Common Mistakes

```
// ❌ Compound operation outside compute — not atomic
if (!map.containsKey("key")) {      // check
    map.put("key", compute());      // act — another thread may insert between!
}
// ✅ Use computeIfAbsent — atomic
map.computeIfAbsent("key", k -> compute());

// ❌ Relying on size() being accurate under concurrency
if (map.size() == 0) { ... }       // may be stale
// ✅ Use isEmpty() or mappingCount()
if (map.isEmpty()) { ... }

// ❌ Modifying CopyOnWriteArrayList inside its own iterator
for (String s : list) {
    list.remove(s); // UnsupportedOperationException — iterator is read-only
}
// ✅ Collect then remove, or use removeIf
list.removeIf(s -> s.startsWith("x"));

// ❌ Using CopyOnWriteArrayList for large, write-heavy lists
// Every write copies the full array — O(n) per write → slow

// ❌ SynchronizedList iteration without external lock
List<String> synced = Collections.synchronizedList(new ArrayList<>());
for (String s : synced) { ... } // ConcurrentModificationException!
// ✅ External sync or use CopyOnWriteArrayList
synchronized (synced) {
    for (String s : synced) { ... }
}
```

---