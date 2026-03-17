# ☕ Java 21 Features (LTS)

> Java 21 (September 2023) — LTS release, supported until 2031.
> Key theme: virtual threads stable, pattern matching switch stable,
> sequenced collections, record patterns.

---

## 🧠 What Changed

- **Virtual threads** — stable, lightweight threads for I/O-bound workloads
- **Pattern matching switch** — type patterns, guarded patterns, exhaustiveness (stable)
- **Record patterns** — destructure records inline in patterns (stable)
- **Sequenced collections** — unified first/last API across List, Set, Map
- **Math.clamp** — constrain value within range
- **String Templates** — preview (stable in Java 23)
- **Structured Concurrency** — preview (stable in Java 23)

---

## 📄 Classes in this Module

### `VirtualThreads.java`

| Example | What it covers |
|---------|----------------|
| Creating Virtual Threads | ofVirtual, startVirtualThread, isDaemon, 100k threads |
| Virtual Thread Executor | newVirtualThreadPerTaskExecutor, named factory, Spring Boot |
| Throughput Comparison | Platform pool vs virtual threads, I/O-bound benchmark |
| Structured Concurrency | Manual vs StructuredTaskScope, ShutdownOnFailure |
| Pinning and Best Practices | synchronized pinning, ReentrantLock fix, ThreadLocal warning |

### `PatternMatching.java`

| Example | What it covers |
|---------|----------------|
| Pattern Matching Switch | Type patterns, sealed exhaustiveness, no default needed |
| Guarded Patterns | when clause, ordering, HTTP status categorisation |
| Record Patterns | Destructuring, nested records, switch with records |
| Null Handling | Explicit null case, null + default combined |
| Domain Dispatch | Sealed + records + pattern switch = algebraic dispatch |

### `MiscFeatures.java`

| Example | What it covers |
|---------|----------------|
| Sequenced Collections | getFirst/getLast, addFirst/Last, reversed(), LinkedHashSet |
| SequencedMap | firstEntry/lastEntry, putFirst/Last, reversed map view |
| String Templates | Preview concept, STR/FMT/RAW processors, security benefit |
| Math.clamp and More | clamp, Character.isEmoji, StringBuilder.repeat |
| Java 21 as LTS | What stabilised, preview items, migration checklist |

---

## ⚡ Virtual Threads

```
// Create
Thread vt = Thread.ofVirtual().name("worker").start(task);
Thread vt = Thread.startVirtualThread(task);          // shorthand

// Key properties
vt.isVirtual()   // true
vt.isDaemon()    // always true — JVM won't wait for virtual threads

// Production pattern — one virtual thread per task, no pooling
try (ExecutorService ex = Executors.newVirtualThreadPerTaskExecutor()) {
    Future<String> f = ex.submit(() -> {
        Thread.sleep(Duration.ofMillis(50)); // unmounts → carrier freed
        return "result";
    });
    String result = f.get();
} // close() waits for all tasks

// Spring Boot 3.2+
// spring.threads.virtual.enabled=true
```

**Platform thread vs Virtual thread:**

| | Platform Thread | Virtual Thread |
|--|--|--|
| Backed by | OS thread | JVM scheduler |
| Memory | ~1MB stack | ~few KB |
| Max practical | ~10,000 | Millions |
| Blocking I/O | Blocks OS thread | Unmounts, carrier freed |
| Best for | CPU-bound | I/O-bound |

**Pinning — avoid:**
```
// ❌ synchronized with blocking inside — pins carrier thread
synchronized (lock) {
    Thread.sleep(1000); // carrier blocked — can't run other virtual threads
}

// ✅ ReentrantLock — virtual thread can unmount during lock wait
lock.lock();
try { Thread.sleep(1000); } // carrier freed during sleep
finally { lock.unlock(); }
```

---

## ⚡ Pattern Matching Switch

```
// Type patterns — compiler checks exhaustiveness
String result = switch (obj) {
    case Integer i  -> "int: " + i;
    case String  s  -> "str: " + s.toUpperCase();
    case null       -> "null";
    default         -> "other";
};

// Guarded patterns — 'when' adds extra condition
String category = switch (value) {
    case Integer i when i < 0    -> "negative";
    case Integer i when i == 0   -> "zero";
    case Integer i               -> "positive: " + i;
    default                      -> "not an int";
};

// Sealed types — no default needed
String desc = switch (shape) {       // Shape is sealed — Circle | Rectangle
    case Circle c    -> "circle r=" + c.radius();
    case Rectangle r -> "rect " + r.w() + "x" + r.h();
    // compiler error if any permitted subtype is missing
};

// Record patterns — destructure inline
if (obj instanceof Point(int x, int y)) {
    System.out.println(x + ", " + y); // x and y directly in scope
}

// Nested record patterns
if (obj instanceof Line(Point(int x1, int y1), Point(int x2, int y2))) {
    // all four components in scope
}
```

---

## ⚡ Sequenced Collections

```
// Unified first/last API — works on List, Deque, LinkedHashSet, SortedSet
list.getFirst()     // first element — NoSuchElementException if empty
list.getLast()      // last element
list.addFirst(e)    // insert at front
list.addLast(e)     // insert at end
list.removeFirst()  // remove and return first
list.removeLast()   // remove and return last
list.reversed()     // reversed VIEW — live, reflects changes to original

// SequencedMap — LinkedHashMap, TreeMap
map.firstEntry()              // Map.Entry — first by encounter order
map.lastEntry()               // Map.Entry — last by encounter order
map.putFirst(key, value)      // insert at front
map.putLast(key, value)       // insert at end
map.reversed()                // reversed map view
map.sequencedKeySet()         // SequencedSet of keys
map.sequencedValues()         // SequencedCollection of values
```

---

## 🔑 Common Mistakes

```
// ❌ Pooling virtual threads — defeats the purpose
Executors.newFixedThreadPool(100, Thread.ofVirtual().factory()); // wrong

// ✅ One per task
Executors.newVirtualThreadPerTaskExecutor();

// ❌ synchronized with I/O inside — causes pinning
synchronized (this) { httpClient.send(...); } // pins carrier

// ✅ ReentrantLock
lock.lock(); try { httpClient.send(...); } finally { lock.unlock(); }

// ❌ ThreadLocal<HeavyObject> on virtual threads
// Millions of virtual threads × heavy object = OOM
ThreadLocal<HeavyConnection> conn = ...; // dangerous at scale

// ❌ Using virtual threads for CPU-bound work
// No benefit — CPU never freed during computation
executor.submit(() -> computeHash(data)); // no improvement over platform threads

// ❌ Pattern switch — wrong ordering of guarded cases
case Integer i               -> "any int";   // catches everything first
case Integer i when i < 0    -> "negative";  // unreachable — compiler warns
```

---