# 🌊 Streams

> Covers the Java 8 Stream API — a pipeline for processing sequences
> of elements. Seven focused files covering every stream topic asked.

---

## 🧠 Mental Model

```
Every stream pipeline has exactly 3 parts:

SOURCE           INTERMEDIATE OPS (lazy)          TERMINAL OP
──────────       ────────────────────────         ───────────
list.stream()     .filter(e -> e.salary > 80k)     .collect()
Arrays.stream()   .map(e -> e.name)                .count()
Stream.of()       .flatMap(...)                    .reduce()
Stream.iterate()  .sorted()                        .findFirst()
IntStream.range() .distinct()                      .anyMatch()
                  .limit(10)                       .forEach()
                  .skip(5)                         .toArray()
                  .peek(...)                       .min() / max()

KEY PROPERTIES
  Lazy      — intermediate ops do NOTHING until terminal op fires
  Once-use  — consumed after terminal op, cannot be reused
  No store  — does not hold data, reads from source
  Parallel  — .parallel() for multi-threaded processing
```

---

## 📄 Files in this Module

| File | Topics |
|------|--------|
| `StreamBasics.java` | Sources, lazy evaluation proof, single-use rule, Stream vs Collection |
| `StreamIntermediateOps.java` | filter, map, flatMap, distinct, sorted, peek, limit, skip, mapToInt |
| `StreamTerminalOps.java` | collect, reduce, count, min/max, findFirst/Any, anyMatch/allMatch/noneMatch, toArray |
| `StreamCollectors.java` | toList/Set/Map, groupingBy, partitioningBy, joining, summarizingInt, maxBy, mapping |
| `StreamNumeric.java` | IntStream, LongStream, DoubleStream, range, summaryStatistics, boxing cost, conversions |
| `StreamParallel.java` | parallel vs sequential, ForkJoinPool, ordering, thread safety, when to use/avoid |
| `StreamRealWorld.java` | Sales report, log analyser, grade report, order pipeline, word count, flat invoice lines |

---

## ⚡ Quick Reference

```
// ── Sources ───────────────────────────────────────────────────
list.stream()                              // from Collection
Arrays.stream(arr)                         // from array
Stream.of("a", "b", "c")                  // from values
Stream.empty()                             // empty stream
Stream.generate(() -> "x").limit(n)        // infinite repeated
Stream.iterate(1, n -> n * 2).limit(10)   // infinite sequence
IntStream.range(1, 5)                      // 1 2 3 4 (exclusive end)
IntStream.rangeClosed(1, 5)               // 1 2 3 4 5 (inclusive end)
Stream.concat(s1, s2)                      // combine two streams

// ── Intermediate ops (lazy, return Stream) ────────────────────
.filter(predicate)                         // keep matching elements
.map(function)                             // transform each element
.flatMap(e -> stream)                      // flatten nested streams
.distinct()                                // remove duplicates
.sorted()                                  // natural order
.sorted(comparator)                        // custom order
.limit(n)                                  // take first n
.skip(n)                                   // discard first n
.peek(consumer)                            // debug only
.mapToInt(e -> e.salary)                   // avoid boxing

// ── Terminal ops (trigger execution) ─────────────────────────
.collect(Collectors.toList())              // to List
.collect(Collectors.toSet())               // to Set
.collect(Collectors.toMap(k, v))           // to Map
.collect(Collectors.groupingBy(fn))        // group into Map<K, List<V>>
.collect(Collectors.joining(", ","[","]")) // string concatenation
.count()                                   // number of elements
.reduce(0, Integer::sum)                   // fold to single value
.findFirst()                               // Optional<T>, short-circuits
.findAny()                                 // Optional<T>, better for parallel
.anyMatch(predicate)                       // boolean, short-circuits
.allMatch(predicate)                       // boolean, short-circuits
.noneMatch(predicate)                      // boolean, short-circuits
.min(comparator)                           // Optional<T>
.max(comparator)                           // Optional<T>
.forEach(consumer)                         // side effect per element
.toArray(String[]::new)                    // to typed array
```

---

## ⚡ Collectors Cheat Sheet

```
// Basic
Collectors.toList()
Collectors.toSet()
Collectors.toUnmodifiableList()                    // Java 10+
Collectors.toMap(keyFn, valueFn)
Collectors.toMap(keyFn, valueFn, mergeFn)          // handle duplicates
Collectors.toMap(keyFn, valueFn, mergeFn, LinkedHashMap::new)

// Grouping
Collectors.groupingBy(classifier)                  // Map<K, List<V>>
Collectors.groupingBy(classifier, downstream)      // Map<K, R>
Collectors.partitioningBy(predicate)               // Map<Boolean, List<V>>

// Downstream collectors
Collectors.counting()                              // Long count
Collectors.summingInt(fn)                          // int sum
Collectors.averagingInt(fn)                        // Double avg
Collectors.summarizingInt(fn)                      // IntSummaryStatistics
Collectors.maxBy(comparator)                       // Optional<T>
Collectors.minBy(comparator)                       // Optional<T>
Collectors.mapping(fn, downstream)                 // transform then collect
Collectors.joining(delim, prefix, suffix)          // String
Collectors.collectingAndThen(collector, finisher)  // post-process result
```

---

## ⚡ Vertical (Depth-First) Processing

```
Stream processes elements vertically — filter+map per element,
NOT: filter all, THEN map all.

Source:  [Alice, Bob, Charlie, Diana]
         ↓
filter   Alice ✓ → map → ALICE → collect
         Bob   ✗
         Charlie ✓ → map → CHARLIE → collect
         Diana ✓ → map → DIANA → collect

This means findFirst() stops after the FIRST match —
it does NOT process remaining elements.
```

---

## 🔑 Common Mistakes

```
// ❌ Reusing a stream
Stream<String> s = list.stream();
s.count();   // OK
s.count();   // IllegalStateException — already consumed

// ✅ Use Supplier<Stream> for reuse
Supplier<Stream<String>> ss = list::stream;
ss.get().count();
ss.get().count(); // fresh stream each time

// ❌ Side effects in parallel stream
List<String> result = new ArrayList<>();
list.parallelStream().forEach(result::add); // race condition!

// ✅ Use collect() — always thread-safe
List<String> result = list.parallelStream().collect(Collectors.toList());

// ❌ toMap with duplicate keys — throws IllegalStateException
Map<String, Integer> map = list.stream()
    .collect(Collectors.toMap(e -> e.dept, e -> e.salary)); // duplicate dept!

// ✅ Provide merge function
Map<String, Integer> map = list.stream()
    .collect(Collectors.toMap(
        e -> e.dept, e -> e.salary,
        Integer::max)); // keep highest salary per dept

// ❌ Stream<Integer> when IntStream is better
list.stream().mapToInt(Integer::intValue).sum(); // unnecessary unboxing

// ✅ Primitive stream — zero boxing
list.stream().mapToInt(e -> e.salary).sum();
```

---