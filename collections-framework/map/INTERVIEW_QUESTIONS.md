# 🎯 Interview Questions — Map

---

> As you get experience, go deep into HashMap internals, thread safety
> tradeoffs, atomic operation design, LRU Cache implementation, and
> production bugs you should have already encountered and fixed.

---

## HashMap Internals

**Q1. Walk me through exactly what happens when you call `HashMap.put("key", value)` in Java 8+.**

> 1. `hash(key)` computed: `key.hashCode() ^ (h >>> 16)` — XOR with upper 16 bits spreads hash distribution, reducing collisions in lower bits used for indexing.
> 2. `index = hash & (capacity - 1)` — bitwise AND works because capacity is always a power of 2.
> 3. Bucket empty → insert new Node.
> 4. Bucket occupied → walk the chain comparing hash first, then key.equals(). Match → overwrite value. No match → append to chain.
> 5. Chain length exceeds 8 → treeifyBin() converts to Red-Black TreeNode. Worst-case lookup drops from O(n) to O(log n).
> 6. After insert: if size > threshold (capacity × 0.75) → resize(). New capacity = old × 2. All entries rehashed — O(n).

---

**Q2. Why is HashMap capacity always a power of 2?**

> Bucket index is `hash & (capacity - 1)`. Works as true modulo only when capacity is power of 2, because `2^n - 1` in binary is all 1s.
>
> If capacity were 10: `hash & 9` (1001) only uses bits 0 and 3 — uneven distribution, many collisions.
>
> Actual modulo `hash % capacity` would distribute correctly but integer division is ~5x slower than bitwise AND. Power-of-2 gives both correctness and speed.

---

**Q3. What is the treeification threshold and why is it 8?**

> When bucket list exceeds 8 nodes → converts to Red-Black Tree. Based on Poisson distribution — with load factor 0.75 and good hashCode(), probability of any bucket having 8+ entries is ~0.00000006. So treeification almost never happens with well-distributed keys and signals a poor hashCode().
>
> Converts back to list at 6 entries (not 8) — hysteresis prevents thrashing.

---

**Q4. You pre-size `new HashMap<>(1000)`. What is the actual capacity and is it correct?**

> Actual capacity: **1024** (rounds up to next power of 2).
>
> Pre-sizing itself is wrong. HashMap resizes at 75% full — it will resize at 750 entries, not 1000.
>
> ```
> // WRONG — resizes at 750 entries
> Map<K,V> map = new HashMap<>(1000);
>
> // CORRECT — no resize until 1334+ entries
> Map<K,V> map = new HashMap<>((int)(1000 / 0.75) + 1);
> ```
> Guava: `Maps.newHashMapWithExpectedSize(1000)` handles this correctly.

---

## Atomic Operations

**Q5. What is the difference between `compute()`, `computeIfAbsent()`, `computeIfPresent()`, and `merge()`?**

> `computeIfAbsent(k, fn)` — only if key absent. Best for lazy init:
> ```
> map.computeIfAbsent(dept, k -> new ArrayList<>()).add(emp);
> ```
>
> `computeIfPresent(k, fn)` — only if key present. Null return removes key:
> ```
> map.computeIfPresent("stock", (k, qty) -> qty > 1 ? qty - 1 : null);
> ```
>
> `compute(k, fn)` — always called. Null return removes key. Best for read-modify-write:
> ```
> map.compute("count", (k, v) -> v == null ? 1 : v + 1);
> ```
>
> `merge(k, v, fn)` — absent → store v; present → apply fn. Best for aggregation:
> ```
> map.merge(word, 1, Integer::sum);  // cleanest frequency counter
> ```
>
> In ConcurrentHashMap all four are **atomic per key**.

---

**Q6. Why is `map.put(k, map.get(k) + 1)` a bug in ConcurrentHashMap?**

> `get()` and `put()` are individually atomic but not atomic together — classic read-modify-write race:
> ```
> Thread A: get("key") → 5
> Thread B: get("key") → 5
> Thread A: put("key", 6)   ← writes 6
> Thread B: put("key", 6)   ← also writes 6 — increment lost!
> ```
> Fix:
> ```
> map.merge("key", 1, Integer::sum);                         // atomic
> map.computeIfAbsent("key", k -> new AtomicInteger())
>    .incrementAndGet();                                      // lock-free CAS
> ```

---

## Thread Safety

**Q7. Compare HashMap, Collections.synchronizedMap(), Hashtable, and ConcurrentHashMap.**

> `HashMap` — not thread-safe. Concurrent writes corrupt structure. Use single-threaded only.
>
> `Hashtable` — legacy, synchronises every method on the whole object. Thread-safe but completely serialised. Never use in new code.
>
> `Collections.synchronizedMap()` — single mutex on every operation. Same throughput bottleneck. Must manually synchronise on iteration:
> ```
> synchronized(syncMap) { for (Map.Entry e : syncMap.entrySet()) { ... } }
> ```
>
> `ConcurrentHashMap` — fine-grained per-bucket locking (Java 8+: mostly CAS). Reads lock-free. Far better throughput under concurrent load. Iteration weakly consistent — no CME. **Always prefer for concurrent use.**

---

**Q8. Why does ConcurrentHashMap not allow null keys or values?**

> Null creates unresolvable ambiguity. If `get(key)` returns null you cannot tell: key absent, or key → null?
>
> In single-threaded HashMap you resolve with `containsKey()`. In ConcurrentHashMap another thread can remove the key between those two calls — making the check unreliable.
>
> Doug Lea deliberately excluded null. Use sentinel values instead:
> ```
> map.put("key", Optional.empty());
> map.put("key", "NONE");
> ```

---

## LRU Cache

**Q9. Implement an LRU Cache using LinkedHashMap. Explain every line.**

> ```
> class LRUCache<K, V> extends LinkedHashMap<K, V> {
>     private final int capacity;
>
>     // accessOrder=true → every get() moves entry to TAIL (MRU end)
>     // HEAD = Least Recently Used → auto-removed when full
>     LRUCache(int capacity) {
>         super(16, 0.75f, true);
>         this.capacity = capacity;
>     }
>
>     // Called after every put()
>     // Return true → LinkedHashMap removes eldest (LRU head)
>     @Override
>     protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
>         return size() > capacity;
>     }
> }
> ```
>
> `accessOrder=false` (default): insertion order, eldest = first-inserted. Good for FIFO.
> `accessOrder=true`: every get()/put() moves to tail. Head = LRU. Perfect for LRU cache.
>
> Limitation: not thread-safe. For concurrent LRU use Caffeine.

---

**Q10. When would you replace a LinkedHashMap LRU with Caffeine in production?**

> Replace when you need:
> 1. **Thread safety** — LinkedHashMap is not thread-safe. synchronizedMap wrapping creates single-lock bottleneck.
> 2. **TTL expiry** — no time-based eviction. Stale data stays forever if accessed.
> 3. **Async loading** — no cache loader. Cache miss blocks calling thread.
> 4. **Metrics** — no hit rate, miss rate, or eviction counts.
> 5. **Weight-based eviction** — evict by size in bytes, not just entry count.
>
> Spring Boot uses Caffeine as default CacheManager backend for these exact reasons.

---

## TreeMap

**Q11. You need a rate limiter — 100 requests per minute. How does TreeMap help?**

> ```
> TreeMap<Long, Integer> window = new TreeMap<>();
>
> boolean isAllowed(long now) {
>     long start = now - 60_000;
>     window.headMap(start).clear();        // O(log n) — remove expired
>     int total = window.values().stream().mapToInt(i -> i).sum();
>     if (total >= 100) return false;
>     window.merge(now, 1, Integer::sum);
>     return true;
> }
> ```
> `headMap(start).clear()` removes all timestamps before the window in O(log n). With a List you scan all entries — O(n). TreeMap range operations make sliding-window algorithms clean and efficient.

---

**Q12. TreeMap vs HashMap for a date-range query — walk me through both.**

> **HashMap — O(n) full scan:**
> ```
> for (Map.Entry<LocalDate, List<User>> e : map.entrySet()) {
>     if (!e.getKey().isBefore(start) && !e.getKey().isAfter(end))
>         result.addAll(e.getValue());
> }
> ```
>
> **TreeMap — O(log n + k):**
> ```
> map.subMap(start, true, end, true)
>    .values()
>    .forEach(result::addAll);
> ```
>
> For k results out of n total, TreeMap is O(log n + k) vs HashMap's O(n). Advantage grows as map gets larger and result set stays small.

---

## Design & Production

**Q13. Multiple threads increment endpoint counters in a ConcurrentHashMap. What is correct?**

> **merge() — simplest:**
> ```
> ConcurrentHashMap<String, Integer> hits = new ConcurrentHashMap<>();
> hits.merge(endpoint, 1, Integer::sum);  // atomic per key
> ```
>
> **AtomicInteger values — best for high-frequency:**
> ```
> ConcurrentHashMap<String, AtomicInteger> hits = new ConcurrentHashMap<>();
> hits.computeIfAbsent(endpoint, k -> new AtomicInteger())
>     .incrementAndGet();  // CPU-level CAS, no lock
> ```
>
> AtomicInteger uses hardware CAS — significantly faster than merge() under high contention.
>
> **Never do this:**
> ```
> hits.put(ep, hits.getOrDefault(ep, 0) + 1);  // race condition!
> ```

---

**Q14. You return a Map<String, List<Order>> from a service method. What defensive measures do you take?**

> **Layer 1 — unmodifiable outer map:**
> ```
> return Collections.unmodifiableMap(result);
> ```
>
> **Layer 2 — unmodifiable outer map + unmodifiable inner lists:**
> ```
> result.replaceAll((k, v) -> Collections.unmodifiableList(v));
> return Collections.unmodifiableMap(result);
> ```
>
> **Layer 3 — full defensive copy:**
> ```
> Map<String, List<Order>> copy = new HashMap<>();
> result.forEach((k, v) -> copy.put(k, new ArrayList<>(v)));
> return Collections.unmodifiableMap(copy);
> ```
>
> Java 10+: `Map.copyOf()` — unmodifiable shallow copy in one call, but inner lists remain mutable.

---

**Q15. Walk me through a production bug caused by using a mutable object as a HashMap key.**

> Scenario: `Order` used as map key with `hashCode()` based on `orderId`. After inserting, `orderId` updated by JPA syncing with database.
>
> ```
> Order order = new Order();    // orderId=0 → hash → bucket 0
> cache.put(order, response);
>
> order.setOrderId(12345);      // hash changes → bucket 47
>
> cache.get(order);             // looks in bucket 47 → null
> cache.remove(order);          // also fails
> cache.size();                 // still 1 — stranded in bucket 0 forever
> ```
>
> Entry permanently orphaned. Never findable, never removable, never GC'd while map lives.
>
> **Fix:** Map keys must be immutable. Use String, Integer, UUID, or a dedicated immutable key record. Never use a mutable JPA entity as a map key.