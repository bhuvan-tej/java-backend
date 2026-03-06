# 🌍 Real World Problems

> Combines everything from the collections framework into 10
> production-grade scenarios. No toy examples — every problem
> is something you would actually encounter in a Java backend service.

---

## 🗺️ Problem Index

| # | Problem | Collections Used | Key Pattern |
|---|---------|-----------------|-------------|
| 1 | Word Frequency Analyser | HashMap | `merge()` + stream sort |
| 2 | LRU Cache Service | LinkedHashMap | `accessOrder=true` + `removeEldestEntry` |
| 3 | Task Scheduler | PriorityQueue | Multi-field Comparator chaining |
| 4 | Sliding Window Rate Limiter | TreeMap | `headMap().clear()` evicts expired |
| 5 | Graph BFS / DFS | ArrayDeque, HashSet | Queue for BFS, Stack for DFS |
| 6 | Top-K Products by Sales | PriorityQueue | Min-heap of size K |
| 7 | Anagram Grouper | HashMap | Sorted chars as canonical key |
| 8 | Stock Price Window Stats | TreeMap | `tailMap()` + `floorEntry()` |
| 9 | Multi-threaded Hit Counter | ConcurrentHashMap | `computeIfAbsent` + `AtomicInteger` |
| 10 | Shopping Cart + Discounts | LinkedHashMap, TreeMap | Insertion order + `floorKey()` bracket lookup |

---

## 📄 What Each Problem Demonstrates

### Problem 1 — Word Frequency Analyser
```
HashMap.merge(word, 1, Integer::sum)   ← cleanest frequency counter
stream sorted by value descending      ← top-N pattern
filter by value == 1                   ← find unique elements
```

### Problem 2 — LRU Cache Service
```
LinkedHashMap(cap, 0.75f, true)        ← accessOrder=true is the key flag
removeEldestEntry → return size > cap  ← auto-eviction hook
get() moves entry to MRU tail         ← LRU head auto-evicted on next put
```

### Problem 3 — Task Scheduler
```
PriorityQueue with Comparator chain:
  .comparingInt(priority)              ← 1 = highest priority first
  .thenComparingLong(deadline)         ← ties broken by deadline
  .thenComparing(name)                 ← stable alphabetic tiebreaker
```

### Problem 4 — Sliding Window Rate Limiter
```
TreeMap<timestamp, count>
requests.headMap(windowStart).clear()  ← O(log n) eviction of expired entries
requests.values().stream().sum()       ← count current window
requests.merge(now, 1, Integer::sum)   ← record this request
```

### Problem 5 — Graph BFS / DFS
```
BFS: ArrayDeque as Queue (offer/poll)
     LinkedHashSet visited             ← preserves visit order
     parent map for path reconstruction

DFS: ArrayDeque as Stack (push/pop)
     push neighbours in reverse        ← left-to-right processing order
     skip already-visited on pop
```

### Problem 6 — Top-K Products
```
Min-heap of size K (PriorityQueue, natural order)
  offer(product)
  if size > K → poll()                 ← evicts lowest sales
After all: heap contains K highest     ← heap.peek() = Kth highest
```

### Problem 7 — Anagram Grouper
```
Key insight: anagrams have identical sorted characters
  char[] sorted = word.toCharArray(); Arrays.sort(sorted)
  key = new String(sorted)            ← "eat","tea","ate" all → "aet"
  computeIfAbsent(key, k -> new ArrayList<>()).add(word)
```

### Problem 8 — Stock Price Window Stats
```
TreeMap<timestamp, price>
  tailMap(now - window)               ← all entries in last N seconds
  stream().summaryStatistics()        ← min/max/avg in one pass
  floorEntry(timestamp)               ← last price at or before point in time
  ceilingEntry(timestamp)             ← first price at or after point in time
```

### Problem 9 — Multi-threaded Hit Counter
```
ConcurrentHashMap<endpoint, AtomicInteger>
  computeIfAbsent(ep, k -> new AtomicInteger())
  .incrementAndGet()                  ← CPU-level CAS, no lock
CountDownLatch                        ← wait for all threads to finish
total verified == threads × requests  ← zero lost increments
```

### Problem 10 — Shopping Cart + Discount Brackets
```
LinkedHashMap<sku, CartItem>          ← insertion order = display order
TreeMap<minCartValue, discountRate>   ← sorted discount tiers
  floorKey(subtotal)                  ← largest bracket ≤ subtotal
  get(bracket)                        ← apply correct discount rate
```

---

## 🔑 Patterns Worth Memorising

```
// ── Frequency count ───────────────────────────────────────────
map.merge(word, 1, Integer::sum);

// ── Group into lists ──────────────────────────────────────────
map.computeIfAbsent(key, k -> new ArrayList<>()).add(value);

// ── Top-K largest (min-heap) ──────────────────────────────────
PriorityQueue<T> topK = new PriorityQueue<>(comparator);
for (T item : all) {
    topK.offer(item);
    if (topK.size() > k) topK.poll(); // evict smallest
}

// ── Sliding window eviction ───────────────────────────────────
TreeMap<Long, Integer> window = new TreeMap<>();
window.headMap(now - windowSize).clear(); // O(log n)

// ── Bracket / tier lookup ─────────────────────────────────────
Integer bracket = treeMap.floorKey(value); // largest key ≤ value
Object  tier    = treeMap.get(bracket);

// ── Anagram canonical key ─────────────────────────────────────
char[] c = word.toCharArray();
Arrays.sort(c);
String key = new String(c);

// ── BFS shortest path ─────────────────────────────────────────
Map<Node, Node> parent = new HashMap<>();
Queue<Node> q = new ArrayDeque<>();
q.offer(src); parent.put(src, null);
while (!q.isEmpty()) {
    Node n = q.poll();
    if (n == dst) break;
    for (Node nb : graph.get(n))
        if (!parent.containsKey(nb)) { parent.put(nb, n); q.offer(nb); }
}

// ── Thread-safe counter ───────────────────────────────────────
chm.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
```

---

## ⚡ Collections Used Per Problem

```
Problem                    HashMap  LinkedHashMap  TreeMap  PriorityQueue  ArrayDeque  CHM
─────────────────────────────────────────────────────────────────────────────────────────
Word Frequency              ✅
LRU Cache                                ✅
Task Scheduler                                             ✅
Rate Limiter                                       ✅
Graph BFS/DFS                                                              ✅
Top-K Products                                             ✅
Anagram Grouper             ✅
Stock Price Stats                                  ✅
Hit Counter                                                                            ✅
Shopping Cart                            ✅         ✅
```

---