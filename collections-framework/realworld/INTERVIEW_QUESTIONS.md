# 🎯 Interview Questions — Real World Problems

---

> These are scenario-based questions — the kind asked in senior interviews
> where you must design a solution, justify your collection choices,
> and reason about edge cases and performance tradeoffs.

---

## Scenario Questions

**Q1. Design a word frequency counter for a large document. It must return the top-K words by frequency. Walk through your collection choices.**

> Two-phase approach:
>
> **Phase 1 — Count frequencies:**
> ```
> Map<String, Integer> freq = new HashMap<>();
> for (String word : words) {
>     freq.merge(word, 1, Integer::sum); // atomic, no null check
> }
> ```
> `HashMap` for O(1) per word. `merge()` avoids the verbose check-then-put pattern.
>
> **Phase 2 — Top-K:**
> ```
> // Option A — Stream sort: O(n log n), simple
> freq.entrySet().stream()
>     .sorted(Map.Entry.<String,Integer>comparingByValue().reversed())
>     .limit(k)
>     .collect(Collectors.toList());
>
> // Option B — Min-heap: O(n log k), optimal for large n, small k
> PriorityQueue<Map.Entry<String,Integer>> topK =
>     new PriorityQueue<>(Comparator.comparingByValue());
> for (Map.Entry<String,Integer> e : freq.entrySet()) {
>     topK.offer(e);
>     if (topK.size() > k) topK.poll();
> }
> ```
>
> For a 10M-word document with k=10: stream sort is O(n log n) ≈ 230M ops.
> Min-heap is O(n log k) ≈ 33M ops — 7× faster. At scale, the heap approach
> is the right answer.

---

**Q2. Design an LRU Cache. What is the time complexity of get and put? What are its production limitations?**

> ```
> class LRUCache<K, V> extends LinkedHashMap<K, V> {
>     private final int capacity;
>     LRUCache(int cap) { super(16, 0.75f, true); this.capacity = cap; }
>     protected boolean removeEldestEntry(Map.Entry<K,V> e) {
>         return size() > capacity;
>     }
> }
> ```
>
> Both `get()` and `put()` are **O(1)**:
> - `get()` — O(1) HashMap lookup + O(1) linked list move-to-tail
> - `put()` — O(1) HashMap insert + O(1) linked list append + O(1) head eviction
>
> **Production limitations:**
> 1. Not thread-safe — concurrent reads/writes corrupt the linked list
> 2. No TTL — entries never expire by time, only by access order
> 3. No async loading — cache miss blocks the calling thread
> 4. No metrics — no hit rate, miss rate tracking
> 5. Memory-only — no persistence, warm-up required after restart
>
> Production replacement: **Caffeine** — concurrent, TTL support, async loading,
> built-in stats, used as Spring Boot's default cache manager.

---

**Q3. You need a rate limiter allowing N requests per user per minute. Design it using a `TreeMap`. Explain why TreeMap beats a List here.**

> ```
> class RateLimiter {
>     private final int maxRequests;
>     private final long windowMs;
>     private final TreeMap<Long, Integer> requests = new TreeMap<>();
>
>     boolean isAllowed() {
>         long now   = System.currentTimeMillis();
>         long start = now - windowMs;
>
>         requests.headMap(start).clear();           // O(log n) — evict expired
>
>         int total = requests.values().stream()
>                             .mapToInt(i -> i).sum();
>         if (total >= maxRequests) return false;
>
>         requests.merge(now, 1, Integer::sum);      // record request
>         return true;
>     }
> }
> ```
>
> **Why TreeMap beats a List:**
> - `headMap(start).clear()` removes all expired timestamps in O(log n) — finds
    >   the split point in the tree, then removes everything before it
> - With a `List`, you must iterate from the front until you find a non-expired
    >   entry — O(n) per eviction
> - For a high-traffic endpoint, the window fills and empties constantly.
    >   TreeMap's range eviction is significantly faster at scale.
>
> **Production note:** this implementation is not thread-safe. For concurrent use
> replace with a `ConcurrentSkipListMap` (sorted + thread-safe) or use
> Redis with a sorted set for distributed rate limiting.

---

**Q4. Walk through your BFS implementation for shortest path. Why does BFS guarantee shortest path but DFS does not?**

> ```
> List<Integer> shortestPath(Map<Integer, List<Integer>> graph, int src, int dst) {
>     Map<Integer, Integer> parent = new HashMap<>();
>     Queue<Integer> queue = new ArrayDeque<>();
>     queue.offer(src);
>     parent.put(src, -1);
>
>     while (!queue.isEmpty()) {
>         int node = queue.poll();
>         if (node == dst) break;
>         for (int nb : graph.getOrDefault(node, Collections.emptyList())) {
>             if (!parent.containsKey(nb)) {
>                 parent.put(nb, node);
>                 queue.offer(nb);
>             }
>         }
>     }
>     // Reconstruct path by following parent pointers
>     List<Integer> path = new ArrayList<>();
>     for (int at = dst; at != -1; at = parent.get(at)) path.add(at);
>     Collections.reverse(path);
>     return path;
> }
> ```
>
> **Why BFS guarantees shortest path:**
> BFS explores nodes level by level — all nodes at distance 1 first, then
> distance 2, then distance 3. The first time BFS reaches the destination,
> it has done so via the fewest hops. This is a mathematical property of
> the FIFO queue — no shorter path can exist because all shorter distances
> were already exhausted.
>
> **Why DFS does not:**
> DFS goes as deep as possible before backtracking. It may find a path of
> length 10 before discovering a path of length 2. DFS finds *a* path, not
> the *shortest* path. DFS is used for cycle detection, topological sort,
> and connected components — not shortest path.

---

**Q5. The anagram grouper uses sorted characters as a map key. What is the time complexity and are there more efficient approaches?**

> **Current approach — sorted key:**
> ```
> char[] c = word.toCharArray();
> Arrays.sort(c);                     // O(L log L) where L = word length
> String key = new String(c);
> map.computeIfAbsent(key, k -> new ArrayList<>()).add(word);
> ```
> Total: O(n × L log L) where n = number of words, L = average word length.
>
> **Alternative — frequency signature key:**
> ```
> // Build a 26-char frequency string: "a2b0c1..." instead of sorting
> int[] freq = new int[26];
> for (char c : word.toCharArray()) freq[c - 'a']++;
> String key = Arrays.toString(freq); // e.g. "[1,0,0,0,1,0,...,1,...]"
> ```
> Total: O(n × L) — linear in total characters, no sorting.
>
> For short English words (L ≤ 20), the difference is negligible. For long
> strings or Unicode text, the frequency approach scales better. The sorted
> approach is cleaner to read and sufficient for interviews.

---

**Q6. Your `ConcurrentHashMap` + `AtomicInteger` hit counter shows the correct total. Explain why `merge()` alone would also work but is slightly different.**

> **With AtomicInteger:**
> ```
> chm.computeIfAbsent(endpoint, k -> new AtomicInteger()).incrementAndGet();
> ```
> `computeIfAbsent` is atomic — only one thread creates the `AtomicInteger`.
> `incrementAndGet()` uses CPU-level CAS (Compare-And-Swap) — no lock, no
> blocking. Under very high contention (thousands of threads hitting the same
> key), this is the fastest approach.
>
> **With merge():**
> ```
> chm.merge(endpoint, 1, Integer::sum);
> ```
> Also atomic per key in `ConcurrentHashMap`. Simpler code. Slightly more
> overhead per call — boxes int to Integer, applies the lambda.
>
> **Key difference:** `AtomicInteger` avoids boxing on every increment. For
> an endpoint hit 10,000 times per second, `AtomicInteger` produces zero
> Integer objects. `merge()` produces 10,000 Integer objects per second —
> real GC pressure at scale.
>
> For production counters: `AtomicInteger` values.
> For simple aggregation (not extreme throughput): `merge()` is cleaner.

---

**Q7. The shopping cart uses `TreeMap.floorKey()` for discount lookup. What happens if no bracket is found and how do you make it production-safe?**

> `floorKey(subtotal)` returns null if there is no key ≤ subtotal — i.e.,
> the map is empty or all keys are greater than subtotal.
>
> Current code would throw `NullPointerException`:
> ```
> Integer bracket = discounts.floorKey(subtotal);
> double rate = discounts.get(bracket); // NPE if bracket is null!
> ```
>
> Production-safe version:
> ```
> Integer bracket = discounts.floorKey(subtotal);
> double rate = (bracket != null) ? discounts.get(bracket) : 0.0;
> ```
>
> Better — always add a zero-value entry at key 0 to guarantee a floor always exists:
> ```
> discounts.put(0, 0.00); // guaranteed floor for any non-negative subtotal
> ```
> Now `floorKey(subtotal)` always returns at least 0, never null.
>
> This is a defensive data design pattern — when using TreeMap for bracket
> lookup, always include a sentinel minimum key to eliminate the null case.

---

**Q8. How would you extend the graph BFS to find ALL shortest paths, not just one?**

> Track all parents, not just one:
> ```
> Map<Integer, List<Integer>> parents = new HashMap<>();
> Map<Integer, Integer> dist = new HashMap<>();
> Queue<Integer> queue = new ArrayDeque<>();
>
> queue.offer(src);
> dist.put(src, 0);
> parents.put(src, Collections.emptyList());
>
> while (!queue.isEmpty()) {
>     int node = queue.poll();
>     int d = dist.get(node);
>     for (int nb : graph.getOrDefault(node, Collections.emptyList())) {
>         if (!dist.containsKey(nb)) {
>             dist.put(nb, d + 1);
>             parents.put(nb, new ArrayList<>(List.of(node)));
>             queue.offer(nb);
>         } else if (dist.get(nb) == d + 1) {
>             // Same distance — another shortest path parent
>             parents.get(nb).add(node);
>         }
>     }
> }
> // Reconstruct all paths using DFS/backtracking from dst
> ```
>
> Key change: when a node is reached via a different path of the same distance,
> add it as an additional parent instead of discarding it. Then reconstruct
> all paths by following the parent lists recursively from destination to source.
> This is used in "number of shortest paths" problems and network routing.

---

**Q9. The stock price stats use `DoubleSummaryStatistics`. What does it compute and what is the alternative without streams?**

> `DoubleSummaryStatistics` computes count, sum, min, max, and average in a
> single O(n) pass:
> ```
> DoubleSummaryStatistics stats = recent.values().stream()
>     .mapToDouble(Double::doubleValue)
>     .summaryStatistics();
> // stats.getMin(), stats.getMax(), stats.getAverage(), stats.getSum()
> ```
>
> Without streams — manual single pass:
> ```
> double min = Double.MAX_VALUE, max = Double.MIN_VALUE, sum = 0;
> int count = 0;
> for (double price : recent.values()) {
>     min = Math.min(min, price);
>     max = Math.max(max, price);
>     sum += price;
>     count++;
> }
> double avg = count > 0 ? sum / count : 0;
> ```
>
> Both are O(n). The stream version is more readable and composes well with
> filtering. The manual version avoids boxing overhead — `recent.values()`
> returns `Double` objects which `mapToDouble` unboxes. For performance-critical
> stat computation over millions of values, use `DoubleStream` directly or
> a primitive double array.

---

**Q10. How would you make the rate limiter thread-safe for a multi-threaded web server without using Redis?**

> The current `TreeMap`-based implementation has two race conditions:
> 1. `headMap().clear()` + read + write is not atomic
> 2. Two threads can both read `total < max` and both be allowed when only one should
>
> **Option 1 — per-user synchronisation:**
> ```
> ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();
>
> boolean isAllowed(String userId) {
>     Object lock = locks.computeIfAbsent(userId, k -> new Object());
>     synchronized (lock) {
>         return checkAndRecord(userId);
>     }
> }
> ```
> One lock per user — different users don't block each other.
>
> **Option 2 — `ConcurrentSkipListMap` + atomic operations:**
> `ConcurrentSkipListMap` is the concurrent sorted map in Java. Replace
> `TreeMap` with it and use `subMap().clear()` which is weakly consistent
> but sufficient for rate limiting where occasional over-allowance is acceptable.
>
> **Option 3 — Token bucket with `AtomicLong`:**
> Simpler than timestamp tracking. Each user gets a token bucket refilled
> at a fixed rate. `AtomicLong` for token count, `System.nanoTime()` for
> last refill time. One CAS operation per request — lock-free and scalable.
>
> For truly distributed rate limiting across multiple JVMs, use Redis with
> Lua scripts for atomic check-and-increment.

---

**Q11. In the multi-threaded hit counter, why is `CountDownLatch` used? What would happen without it?**

> `CountDownLatch(n)` blocks the main thread until `n` threads call
> `countDown()`. In the hit counter, it ensures all 4 worker threads have
> finished incrementing before the main thread reads and prints the totals.
>
> Without `CountDownLatch`:
> ```
> // Threads are still running when main thread reads
> int total = hits.values().stream().mapToInt(AtomicInteger::get).sum();
> // total could be anywhere from 0 to 1000 — race condition!
> ```
>
> With `CountDownLatch`:
> ```
> latch.await(); // main thread blocks here until all 4 threads call latch.countDown()
> int total = hits.values().stream().mapToInt(AtomicInteger::get).sum();
> // guaranteed: all 1000 increments complete before this line
> ```
>
> `CountDownLatch` is a one-time synchronisation barrier — it cannot be reset.
> For reusable barriers use `CyclicBarrier`. For waiting for any-one-of-many
> completions use `Phaser`. For modern async code, `CompletableFuture.allOf()`
> is usually cleaner.

---

**Q12. The shopping cart preserves insertion order using `LinkedHashMap`. What happens if you use `HashMap` instead?**

> With `HashMap`, the display order is unpredictable — the cart items would
> appear in hash bucket order which changes between JVM runs and with map size.
>
> Practical impact:
> - Invoice/receipt line items appear in random order — confusing for customers
> - Diff-based change detection would show spurious differences
> - Serialisation to JSON (Jackson uses insertion order for Maps) would produce
    >   inconsistent field ordering
>
> `LinkedHashMap` guarantees insertion order with O(1) performance identical
> to HashMap for get/put. The overhead is just the doubly-linked list
> maintaining insertion sequence — negligible for a shopping cart.
>
> For a cart displayed to the user, insertion order is the correct semantic:
> items appear in the order they were added. This is a deliberate design
> choice, not an accident.

---

**Q13. How would you extend the Top-K products to support dynamic updates — new sales arrive in real time and the top-K list must stay current?**

> The batch min-heap approach breaks for real-time updates because we cannot
> efficiently update a product's sales count in the heap.
>
> Better approach: `TreeMap<Integer, Set<String>>` — reverse index from sales
> count to product names. Always sorted, always current:
> ```
> TreeMap<Integer, Set<String>> salesIndex = new TreeMap<>(Comparator.reverseOrder());
> Map<String, Integer> productSales = new HashMap<>();
>
> void recordSale(String product) {
>     int oldSales = productSales.getOrDefault(product, 0);
>     int newSales = oldSales + 1;
>
>     // Remove from old position
>     salesIndex.getOrDefault(oldSales, Collections.emptySet()).remove(product);
>     if (salesIndex.containsKey(oldSales) && salesIndex.get(oldSales).isEmpty())
>         salesIndex.remove(oldSales);
>
>     // Insert at new position
>     salesIndex.computeIfAbsent(newSales, k -> new HashSet<>()).add(product);
>     productSales.put(product, newSales);
> }
>
> List<String> topK(int k) {
>     List<String> result = new ArrayList<>();
>     for (Set<String> products : salesIndex.values()) {
>         result.addAll(products);
>         if (result.size() >= k) break;
>     }
>     return result.subList(0, Math.min(k, result.size()));
> }
> ```
> Each sale update: O(log n). topK query: O(k). This is the pattern used in
> real-time leaderboards.

---

**Q14. How would you detect if two documents are anagrams of each other (whole document, not word by word)?**

> Compare character frequency maps:
> ```
> boolean areAnagrams(String doc1, String doc2) {
>     // Quick length check first
>     if (doc1.length() != doc2.length()) return false;
>
>     // Count frequencies in doc1
>     Map<Character, Integer> freq = new HashMap<>();
>     for (char c : doc1.toCharArray())
>         freq.merge(c, 1, Integer::sum);
>
>     // Subtract frequencies for doc2
>     for (char c : doc2.toCharArray()) {
>         freq.merge(c, -1, Integer::sum);
>         if (freq.get(c) == 0) freq.remove(c); // clean up zeros
>     }
>
>     return freq.isEmpty(); // empty map = all counts balanced = anagram
> }
> ```
> O(n) time, O(k) space where k = distinct characters (at most 26 for
> lowercase English). Single pass through each document.
>
> Alternative for Unicode text: use int[26] array for ASCII, or
> `HashMap<Character, Integer>` for full Unicode. The map approach
> generalises to any character set without modification.

---

**Q15. You have all 10 real-world problems in front of you. An interviewer asks: which single collection is the most powerful and why?**

> The honest answer: **HashMap** — it is the foundation of more patterns
> than any other collection.
>
> - Frequency counting → `merge()`
> - Grouping / bucketing → `computeIfAbsent()`
> - Caching → key-value lookup in O(1)
> - Graph adjacency list → `Map<Node, List<Node>>`
> - Anagram grouping → sorted-chars key
> - Parent tracking in BFS → `Map<Node, Node>`
> - Memoisation in DP → `Map<State, Result>`
> - Deduplication → `Map` as a Set
>
> But the **most underused yet powerful** is `TreeMap`. Its NavigableMap
> operations — `floorKey()`, `ceilingKey()`, `headMap()`, `tailMap()`,
> `subMap()` — solve an entire class of range, bracket, and sliding-window
> problems that would otherwise require O(n) scans:
> - Tax brackets → `floorKey(income)`
> - Rate limiting → `headMap(windowStart).clear()`
> - Time-series queries → `tailMap(since)`
> - Stock price lookups → `floorEntry(timestamp)`
>
> Most developers reach for HashMap first and write O(n) loops. Recognising
> when the problem has a sorted-key shape and reaching for TreeMap instead
> is what separates a 3-year developer from a 5-year developer.