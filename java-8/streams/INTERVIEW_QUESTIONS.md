# 🎯 Interview Questions — Streams

---

> Stream questions as we gain experience focus on lazy evaluation internals,
> collector internals, parallel pitfalls, flatMap vs map, reduce vs
> collect, and designing stream pipelines for production scenarios.

---

## Core Concepts

**Q1. What is a Stream? How is it different from a Collection?**

> A Stream is a pipeline for processing a sequence of elements — it is NOT
> a data structure and does NOT store data. It is a view over a source
> (Collection, array, I/O channel) that processes elements lazily.
>
> Key differences:
>
> | | Collection | Stream |
> |---|---|---|
> | Stores data | ✅ | ❌ |
> | Reusable | ✅ | ❌ once consumed |
> | Lazy | ❌ | ✅ |
> | Infinite | ❌ | ✅ generate/iterate |
> | External iteration | for-each | internal (forEach) |
> | Parallel | manual | .parallel() |
>
> ```
> List<String> list = Arrays.asList("a","b","c"); // stores data
> list.size();   // iterate again — fine
> list.size();   // iterate again — fine
>
> Stream<String> s = list.stream(); // pipeline, no storage
> s.count();  // consumed
> s.count();  // IllegalStateException!
> ```

---

**Q2. Explain lazy evaluation in streams. Why does it matter?**

> Intermediate operations (`filter`, `map`, `flatMap`, etc.) do NOT execute
> when called. They build a pipeline description. Execution only happens when
> a terminal operation is called.
>
> ```
> Stream<String> pipeline = list.stream()
>     .filter(s -> { System.out.println("filter: " + s); return true; })
>     .map(s -> { System.out.println("map: " + s); return s.toUpperCase(); });
>
> System.out.println("pipeline built — nothing ran yet");
> pipeline.collect(Collectors.toList()); // NOW filter and map run
> ```
>
> Why it matters — two key benefits:
>
> 1. **Short-circuit optimisation**: `findFirst()` stops after the first match.
     >    With eager evaluation, filter would scan all elements first. With lazy
     >    evaluation, filter+map run only until the first match is found:
> ```
> // Only processes elements until first salary > 90k is found
> employees.stream()
>     .filter(e -> e.salary > 90_000)
>     .findFirst(); // stops immediately after first match
> ```
>
> 2. **Infinite streams**: `Stream.generate()` and `Stream.iterate()` are
     >    only possible because of laziness. `limit(10)` stops the infinite source
     >    after 10 elements — possible only because elements are pulled one at a time.

---

**Q3. Explain vertical (depth-first) processing. How does it differ from what most people assume?**

> Most people assume streams process horizontally — all elements through
> filter, then all through map. The actual execution is vertical — each
> element goes through the entire pipeline before the next element starts:
>
> ```
> Source: [Alice, Bob, Charlie]
>
> WRONG mental model (horizontal):
>   filter(Alice) filter(Bob) filter(Charlie)   ← filter all first
>   map(Alice)    map(Charlie)                  ← then map all
>
> CORRECT mental model (vertical):
>   filter(Alice) → map(Alice) → collect
>   filter(Bob)   → rejected, stop here
>   filter(Charlie) → map(Charlie) → collect
> ```
>
> This is why `findFirst()` is efficient — it stops the entire pipeline
> after the first element satisfies all intermediate operations. No remaining
> elements are ever touched.
>
> Production implication: put the most selective `filter()` first to
> eliminate as many elements as early as possible.

---

**Q4. What is the difference between `map()` and `flatMap()`?**

> `map(fn)` — applies a function to each element, wrapping the result.
> Returns `Stream<R>` where each element maps to exactly one result.
>
> `flatMap(fn)` — applies a function that returns a Stream per element,
> then flattens all those streams into one Stream. One element can produce
> zero, one, or many results.
>
> ```
> List<String> sentences = Arrays.asList("hello world", "java streams");
>
> // map → Stream<String[]> — each element is an array
> sentences.stream()
>     .map(s -> s.split(" "))   // [["hello","world"], ["java","streams"]]
>     .collect(Collectors.toList()); // List<String[]> — not useful
>
> // flatMap → Stream<String> — flat list of all words
> sentences.stream()
>     .flatMap(s -> Arrays.stream(s.split(" "))) // ["hello","world","java","streams"]
>     .collect(Collectors.toList()); // List<String> — correct
> ```
>
> Rule: use `flatMap` when your mapping function returns a Collection or
> Stream per element and you want the combined results, not a nested structure.
> Common cases: split strings, expand orders into line items, unwrap Optionals.

---

**Q5. Explain `reduce()`. What is the difference between `reduce(identity, accumulator)` and `reduce(accumulator)` with no identity?**

> `reduce` folds all elements into a single value using an accumulator function.
>
> **With identity** — always returns `T`, never Optional:
> ```
> int sum = Stream.of(1,2,3,4,5).reduce(0, Integer::sum); // 15
> // If stream is empty → returns identity (0), never null
> ```
>
> **Without identity** — returns `Optional<T>` because the stream may be empty:
> ```
> Optional<Integer> max = Stream.of(1,2,3,4,5).reduce(Integer::max); // Optional[5]
> Stream.<Integer>empty().reduce(Integer::max); // Optional.empty()
> ```
>
> **When to use reduce vs collect:**
> - `reduce` — for immutable aggregation into a single scalar value (sum, max, concatenation)
> - `collect` — for mutable accumulation into a container (List, Map, Set)
>
> ```
> // reduce — correct for summing
> int total = employees.stream().mapToInt(e -> e.salary).reduce(0, Integer::sum);
>
> // collect — correct for building a list
> List<String> names = employees.stream().map(e -> e.name).collect(Collectors.toList());
>
> // Never do this — building a list with reduce is wrong (creates new list per element)
> List<String> wrong = employees.stream().map(e -> e.name)
>     .reduce(new ArrayList<>(), (list, name) -> { list.add(name); return list; },
>             (a, b) -> { a.addAll(b); return a; }); // mutable container, wrong with parallel
> ```

---

## Collectors

**Q6. How does `groupingBy()` work? What downstream collectors can you chain with it?**

> `groupingBy(classifier)` groups stream elements into a `Map<K, List<V>>` where
> K is the result of the classifier function and V is the element.
>
> With a downstream collector, the value type changes from `List<V>` to whatever
> the downstream collector produces:
>
> ```
> // Basic — Map<String, List<Employee>>
> Map<String, List<Employee>> byDept =
>     employees.stream().collect(Collectors.groupingBy(e -> e.dept));
>
> // + counting → Map<String, Long>
> Map<String, Long> countByDept =
>     employees.stream().collect(
>         Collectors.groupingBy(e -> e.dept, Collectors.counting()));
>
> // + averagingInt → Map<String, Double>
> Map<String, Double> avgSalary =
>     employees.stream().collect(
>         Collectors.groupingBy(e -> e.dept, Collectors.averagingInt(e -> e.salary)));
>
> // + mapping + toList → Map<String, List<String>>
> Map<String, List<String>> namesByDept =
>     employees.stream().collect(
>         Collectors.groupingBy(e -> e.dept,
>             Collectors.mapping(e -> e.name, Collectors.toList())));
>
> // + maxBy → Map<String, Optional<Employee>>
> Map<String, Optional<Employee>> topPerDept =
>     employees.stream().collect(
>         Collectors.groupingBy(e -> e.dept,
>             Collectors.maxBy(Comparator.comparingInt(e -> e.salary))));
>
> // Multi-level grouping
> Map<String, Map<Boolean, List<Employee>>> nested =
>     employees.stream().collect(
>         Collectors.groupingBy(e -> e.dept,
>             Collectors.groupingBy(e -> e.remote)));
> ```

---

**Q7. What is `partitioningBy()` and when do you use it instead of `groupingBy()`?**

> `partitioningBy(predicate)` is a specialised form of `groupingBy` that
> always produces exactly two groups — `true` and `false`:
>
> ```
> Map<Boolean, List<Employee>> byRemote =
>     employees.stream().collect(Collectors.partitioningBy(e -> e.remote));
>
> List<Employee> remote  = byRemote.get(true);
> List<Employee> onsite  = byRemote.get(false);
> ```
>
> Use `partitioningBy` when:
> - The split is binary (pass/fail, remote/onsite, active/inactive)
> - You always need both groups (even if one is empty)
> - The condition is a predicate, not a category
>
> Use `groupingBy` when:
> - More than 2 categories exist (by department, by status, by region)
> - Categories are dynamic (not known at compile time)
>
> Performance: `partitioningBy` is slightly more efficient than `groupingBy`
> for binary splits because it uses a specialised two-bucket implementation
> rather than a general HashMap.

---

**Q8. `toMap()` throws `IllegalStateException` on duplicate keys. How do you handle it?**

> Without a merge function, duplicate keys throw immediately:
> ```
> // THROWS — two employees in same dept → duplicate key
> Map<String, Integer> map = employees.stream()
>     .collect(Collectors.toMap(e -> e.dept, e -> e.salary)); // IllegalStateException!
> ```
>
> Three approaches:
>
> **1. Merge function — resolve duplicates:**
> ```
> Map<String, Integer> maxSalByDept = employees.stream()
>     .collect(Collectors.toMap(
>         e -> e.dept,
>         e -> e.salary,
>         Integer::max)); // keep higher salary on duplicate dept
> ```
>
> **2. Use groupingBy when you want all values:**
> ```
> Map<String, List<Integer>> salsByDept = employees.stream()
>     .collect(Collectors.groupingBy(
>         e -> e.dept,
>         Collectors.mapping(e -> e.salary, Collectors.toList())));
> ```
>
> **3. LinkedHashMap for insertion-order preservation:**
> ```
> Map<String, Integer> ordered = employees.stream()
>     .collect(Collectors.toMap(
>         e -> e.name, e -> e.salary,
>         (a, b) -> a,           // keep first on duplicate
>         LinkedHashMap::new));  // insertion order
> ```

---

## Parallel

**Q9. How do parallel streams work internally? What pool do they use?**

> Parallel streams use the **ForkJoinPool.commonPool()** by default — a
> shared thread pool sized to `Runtime.getRuntime().availableProcessors() - 1`.
>
> The stream source is split into chunks using a `Spliterator`. ForkJoin
> recursively splits the work, processes chunks in parallel across worker
> threads, then merges (joins) the results.
>
> ```
> // Uses commonPool — shared across all parallel streams in JVM
> list.parallelStream().map(this::heavyComputation).collect(Collectors.toList());
>
> // Dedicated pool — isolate from other tasks (important in web servers)
> ForkJoinPool pool = new ForkJoinPool(4);
> List<Result> result = pool.submit(() ->
>     list.parallelStream().map(this::heavyComputation).collect(Collectors.toList())
> ).get();
> pool.shutdown();
> ```
>
> The commonPool is shared with CompletableFuture and other parallel operations.
> In a web server with heavy traffic, parallel streams in request handlers can
> starve the common pool, causing other async operations to queue. Use a
> dedicated pool for isolation.

---

**Q10. When should you NOT use parallel streams?**

> Parallel streams add overhead — thread coordination, splitting, merging.
> They only win when that overhead is smaller than the parallelism gain.
>
> **Do NOT use parallel when:**
>
> 1. Small data sets — thread overhead > computation time:
> ```
> // Slower than sequential for 5 elements
> Arrays.asList(1,2,3,4,5).parallelStream().sum();
> ```
>
> 2. I/O-bound operations — threads block on I/O, parallelism doesn't help:
> ```
> // HTTP calls block threads — use CompletableFuture instead
> urls.parallelStream().map(this::fetchUrl).collect(Collectors.toList());
> ```
>
> 3. Shared mutable state — race conditions:
> ```
> List<String> result = new ArrayList<>();
> list.parallelStream().forEach(result::add); // corrupts ArrayList!
> ```
>
> 4. Poor splitters — `LinkedList` splits in O(n), negating parallelism gains
>
> 5. Operations with ordering requirements — `forEachOrdered` reintroduces
     >    synchronisation, removing parallel benefit
>
> **Rule**: measure before enabling parallel. For pure CPU-bound work on
> ArrayList/arrays with 10k+ elements and no shared state — parallel wins.

---

**Q11. Why is `forEach` on a parallel stream unsafe for collecting results?**

> `ArrayList` is not thread-safe. Multiple threads calling `add()` concurrently
> corrupt the internal array:
> ```
> List<String> results = new ArrayList<>();
> list.parallelStream().forEach(results::add); // race condition!
> // results.size() may be less than list.size()
> // may contain nulls or throw ArrayIndexOutOfBoundsException
> ```
>
> Two correct approaches:
>
> **1. `collect()` — always thread-safe regardless of parallelism:**
> ```
> List<String> results = list.parallelStream()
>     .collect(Collectors.toList()); // internally uses thread-local containers
> ```
>
> **2. Thread-safe collection:**
> ```
> Queue<String> results = new ConcurrentLinkedQueue<>();
> list.parallelStream().forEach(results::add); // ConcurrentLinkedQueue is thread-safe
> ```
>
> `Collectors.toList()` works by giving each thread its own partial container,
> then merging the partial results at the end — no synchronisation needed
> during accumulation.

---

## Senior Level

**Q12. How would you efficiently find the top-3 highest-paid employees per department using streams?**

> ```
> Map<String, List<Employee>> top3ByDept = employees.stream()
>     .collect(Collectors.groupingBy(
>         e -> e.dept,
>         Collectors.collectingAndThen(
>             Collectors.toList(),
>             list -> list.stream()
>                         .sorted(Comparator.comparingInt((Employee e) -> e.salary).reversed())
>                         .limit(3)
>                         .collect(Collectors.toList()))));
> ```
>
> `collectingAndThen` applies a finishing function to the result of the
> downstream collector — here it sorts and limits each department's list
> after grouping. This is the idiomatic way to apply per-group post-processing.
>
> Alternative for large datasets — use `TreeSet` as downstream to maintain
> sorted order during accumulation, avoiding the sort-after-collect step:
> ```
> Map<String, TreeSet<Employee>> top3ByDept = employees.stream()
>     .collect(Collectors.groupingBy(
>         e -> e.dept,
>         Collectors.toCollection(() ->
>             new TreeSet<>(Comparator.comparingInt((Employee e) -> e.salary).reversed()))));
> // Then limit to 3 when iterating
> ```

---

**Q13. How do you handle a stream pipeline where some mapping operations may return `null`?**

> Three approaches:
>
> **1. Filter nulls explicitly:**
> ```
> List<String> emails = users.stream()
>     .map(User::getEmail)             // may return null
>     .filter(Objects::nonNull)        // remove nulls
>     .collect(Collectors.toList());
> ```
>
> **2. Map to Optional first, then flatMap:**
> ```
> List<String> emails = users.stream()
>     .map(u -> Optional.ofNullable(u.getEmail()))
>     .flatMap(Optional::stream)       // Java 9+: unwrap non-empty
>     .collect(Collectors.toList());
> ```
>
> **3. `Stream.ofNullable()` (Java 9+) per element:**
> ```
> List<String> emails = users.stream()
>     .flatMap(u -> Stream.ofNullable(u.getEmail()))
>     .collect(Collectors.toList());
> ```
>
> Option 3 is the most concise for Java 9+. Option 1 is the most readable
> for Java 8. Never let nulls flow into collectors — `toMap()` throws
> `NullPointerException` if a value is null, and `Collectors.joining()`
> will throw on null elements.

---

**Q14. What is `Collectors.collectingAndThen()` and give a production use case?**

> `collectingAndThen(downstream, finisher)` applies a downstream collector
> first, then applies a finishing function to transform the result.
>
> ```
> // Usage pattern
> Collectors.collectingAndThen(
>     Collectors.toList(),         // downstream: collect to list
>     Collections::unmodifiableList // finisher: wrap as unmodifiable
> )
> ```
>
> Production use cases:
>
> **1. Return unmodifiable collections from service methods:**
> ```
> List<String> names = employees.stream()
>     .map(e -> e.name)
>     .collect(Collectors.collectingAndThen(
>         Collectors.toList(),
>         Collections::unmodifiableList));
> ```
>
> **2. Post-process grouped results:**
> ```
> // Sort each group after grouping
> Map<String, List<Employee>> sorted = employees.stream()
>     .collect(Collectors.groupingBy(
>         e -> e.dept,
>         Collectors.collectingAndThen(
>             Collectors.toList(),
>             list -> { list.sort(Comparator.comparing(e -> e.name)); return list; })));
> ```
>
> **3. Compute a single value with finisher:**
> ```
> // Collect and immediately get count
> long count = employees.stream()
>     .collect(Collectors.collectingAndThen(
>         Collectors.toList(),
>         List::size));
> ```

---

**Q15. Design a stream pipeline that processes a large CSV of orders, filters invalid ones, enriches with customer data, groups by region, and returns a summary — what are the performance considerations?**

> ```
> // Assume: csvLines is a large Stream<String> (file-backed, lazy)
> Map<String, RegionSummary> summary = csvLines
>     .skip(1)                                          // skip header
>     .map(line -> parseOrder(line))                    // parse CSV line
>     .filter(Objects::nonNull)                         // drop parse failures
>     .filter(order -> order.amount > 0)                // drop invalid amounts
>     .filter(order -> order.status != CANCELLED)       // drop cancelled
>     .map(order -> enrich(order, customerCache))       // enrich from cache
>     .collect(Collectors.groupingBy(
>         order -> order.region,
>         Collectors.collectingAndThen(
>             Collectors.toList(),
>             orders -> new RegionSummary(
>                 orders.size(),
>                 orders.stream().mapToLong(o -> o.amount).sum(),
>                 orders.stream().mapToLong(o -> o.amount).average().orElse(0)))));
> ```
>
> Performance considerations:
>
> 1. **Lazy file reading** — use `Files.lines(path)` which returns a lazy
     >    `Stream<String>`, not reading the whole file at once. O(1) memory.
>
> 2. **Filter order** — put cheapest filters first. Status check is cheaper
     >    than customer enrichment — filter before enrichment to avoid calling
     >    `enrich()` for orders that will be discarded.
>
> 3. **Customer cache** — `enrich()` must read from a pre-loaded `HashMap`,
     >    not make DB calls per order. Inline DB calls in a stream are an
     >    anti-pattern — batch or pre-load.
>
> 4. **Parallel** — if `parseOrder` is CPU-intensive and the file is large,
     >    `parallel()` after `skip(1)` can help. But file-backed streams are
     >    not splittable — read sequentially into a List first, then parallelise.
>
> 5. **Memory** — `groupingBy` accumulates all orders in memory. For truly
     >    large datasets (millions of rows), use a database GROUP BY instead.