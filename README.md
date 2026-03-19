```
   ██╗ █████╗ ██╗   ██╗ █████╗     ██████╗  █████╗  ██████╗██╗  ██╗███████╗███╗   ██╗██████╗
   ██║██╔══██╗██║   ██║██╔══██╗    ██╔══██╗██╔══██╗██╔════╝██║ ██╔╝██╔════╝████╗  ██║██╔══██╗
   ██║███████║██║   ██║███████║    ██████╔╝███████║██║     █████╔╝ █████╗  ██╔██╗ ██║██║  ██║
██ ██║██╔══██║╚██╗ ██╔╝██╔══██║    ██╔══██╗██╔══██║██║     ██╔═██╗ ██╔══╝  ██║╚██╗██║██║  ██║
╚████║██║  ██║ ╚████╔╝ ██║  ██║    ██████╔╝██║  ██║╚██████╗██║  ██╗███████╗██║ ╚████║██████╔╝
 ╚═══╝╚═╝  ╚═╝  ╚═══╝  ╚═╝  ╚═╝    ╚═════╝ ╚═╝  ╚═╝ ╚═════╝╚═╝  ╚═╝╚══════╝╚═╝  ╚═══╝╚═════╝
```

## 🧭 What Is This?

A **structured, multi-module Java learning repository** built for developers who want to go beyond tutorials. Every concept is covered with:

- ✅ **Two levels per topic** — foundational (what & how) + adv level (why & when)
- ✅ **Production patterns** — real code you'd write at work, not toy examples
- ✅ **Heavy inline comments** — *why* explained at every step
- ✅ **Interview prep** — each module has its own `INTERVIEW_QUESTIONS.md`
- ✅ **Runnable** — every file has a `main()` and can be run independently

---

## 📦 Module 1 — Collections Framework ✅

```
  collections-framework/
  │
  ├── 📋 lists ─────────  ArrayList · LinkedList
  ├── 🔵 set ───────────  HashSet · LinkedHashSet · TreeSet
  ├── 🗂️  map ──────────  HashMap · LinkedHashMap · TreeMap · ConcurrentHashMap
  ├── 📬 queue ─────────  PriorityQueue · ArrayDeque
  ├── ⚖️  comparable ───  Comparable vs Comparator
  ├── 🔁 iterator ──────  Iterator · ListIterator · Fail-Fast · Fail-Safe
  ├── 🛠️  utils ────────  java.util.Collections utility class
  └── 🌍 realworld ─────  Word Frequency · Task Scheduler · Sliding Window Max
```

---

## ⚡ Module 2 — Java 8 Features ✅

```
  java-8/
  │
  ├── 🔷 lambdas ───────────  Syntax · Captures · Closures · Strategy Pattern
  ├── 🌊 streams ───────────  Basics · Intermediate · Terminal · Collectors · Numeric · Parallel · Advanced · Real World
  ├── 🎁 optionals ─────────  Creation · Retrieval · Chaining · Anti-patterns
  ├── ⚙️ functional ────────  Predicate · Function · Consumer · Supplier · Method References · Composition
  ├── ⚡  completablefuture ─  Async pipelines · thenApply · thenCompose · allOf · anyOf · Exception handling · Timeouts
  ├── 📅 datetime ──────────  LocalDate · LocalDateTime · ZonedDateTime · Instant · Period · Duration · DateTimeFormatter
  └── 🔧 defaultmethods ────  Default · Static · Diamond problem · Mixins
```

---

## 🔒 Module 3 — Concurrency ✅

```
  concurrency/
  │
  ├── 🧵 threads ──────────  Lifecycle · Daemon · join · sleep · interrupt · ThreadLocal
  ├── 🔐 synchronization ──  synchronized · volatile · happens-before · wait/notify · Deadlock
  ├── 🔑 locks ────────────  ReentrantLock · tryLock · ReadWriteLock · StampedLock · Condition
  ├── ⚙️  executors ───────  ThreadPoolExecutor · Future · Callable · ScheduledExecutor · ForkJoin
  ├── 🗂️  concurrent ──────  ConcurrentHashMap · CopyOnWriteArrayList · BlockingQueue
  └── ⚛️  atomic ──────────  AtomicInteger · AtomicReference · CAS · ABA · LongAdder
```

---

## ☕ Module 4 — Java 9 ✅
```
  java-9/
  │
  ├── 🏭 collection factories ─  List.of · Set.of · Map.of · Map.ofEntries · copyOf
  ├── 🌊 stream additions ─────  takeWhile · dropWhile · iterate(predicate) · ofNullable
  ├── 🎁 optional additions ───  ifPresentOrElse · or · Optional.stream
  ├── 🔧 interface private ────  private methods · private static methods
  └── ⚙️  process api ─────────  ProcessHandle · pid · info · allProcesses · onExit
```

---

## ☕ Module 5 — Java 10 ✅
```
  java-10/
  │
  ├── 🔠 var ──────────────────  Type inference · rules · limits · concrete vs interface
  ├── 🎁 Optional ─────────────  orElseThrow() no-arg · vs get()
  └── 📋 Unmodifiable ─────────  toUnmodifiableList/Set/Map · copyOf · defensive copies
```

---

## ☕ Module 6 — Java 11 ✅ (LTS)
```
  java-11/
  │
  ├── 🔤 strings ──────────  isBlank · strip · stripLeading · stripTrailing · lines · repeat
  ├── 📁 files ────────────  Files.readString · Files.writeString
  ├── 🔍 predicate ────────  Predicate.not · method reference negation
  ├── 🔠 var in lambdas ───  var params · annotation support
  ├── 📋 toArray ──────────  toArray(String[]::new)
  └── 🌐 httpclient ───────  sync · async · POST · parallel · headers · timeout
```

---

## ☕ Module 7 — Java 12 to 14 ✅
```
  java-12-14/
  │
  ├── 🔀 switch expressions ─  arrow syntax · yield · exhaustiveness · enum switch
  ├── 📝 text blocks ────────  multiline strings · indentation · \s · line continuation
  ├── 🔤 string additions ───  indent() · transform() · formatted()
  ├── 🔍 pattern instanceof ─  bind variable · scope · in equals()
  └── 💥 helpful npe ────────  precise null messages · chained calls
```

---

## ☕ Module 8 — Java 15-16 ✅
```
  java-15-16/
  │
  ├── 📦 Records ──────────────  declaration · compact constructor · custom methods · interfaces · DTOs
  ├── 🔒 SealedClasses ────────  sealed · permits · final · non-sealed · sealed interfaces · Result type
  └── 🔧 MiscFeatures ─────────  Stream.toList() · String.formatted() · pattern matching stable
```

---

## ☕ Module 9 — Java 17 ✅ (LTS)
```
  java-17/
  │
  ├── 🔒 sealed stable ────────  HTTP response hierarchy · nested records · default methods
  ├── 📦 records stable ───────  wither pattern · generic records · Comparable · nested records
  ├── 🔍 pattern stable ───────  guards · negation · sealed dispatch · combined patterns
  ├── 🔐 encapsulation ────────  JDK internals · --add-opens · migration impact
  ├── 🎲 RandomGenerator ──────  unified interface · algorithms · factory · streams
  └── 📋 LTS context ──────────  what stabilised · migration checklist · Spring Boot 3.x
```

---

Each topic folder contains:
```
  <topic>/
    ├── src/main/java/com/javabackend/<module>/<topic>/
    │       └── *Samples.java          ← runnable examples with main()
    ├── README.md                      ← concept, mental model, methods, when to use
    ├── INTERVIEW_QUESTIONS.md         ← Q&A targeted at 5 years experience
    └── pom.xml
```

---

## 📊 Collections — Big-O at a Glance

```
              GET       ADD      REMOVE    CONTAINS    ORDER
              ─────────────────────────────────────────────────
ArrayList   │ O(1)   │ O(1)*  │ O(n)    │ O(n)     │ By index
LinkedList  │ O(n)   │ O(1)†  │ O(1)†   │ O(n)     │ By index
HashSet     │  —     │ O(1)   │ O(1)    │ O(1)     │ None
LinkedHSet  │  —     │ O(1)   │ O(1)    │ O(1)     │ Insertion
TreeSet     │  —     │ O(logn)│ O(logn) │ O(logn)  │ Sorted
HashMap     │ O(1)   │ O(1)   │ O(1)    │ O(1) key │ None
LinkedHMap  │ O(1)   │ O(1)   │ O(1)    │ O(1) key │ Insertion
TreeMap     │ O(logn)│ O(logn)│ O(logn) │ O(logn)  │ Sorted
PriorQueue  │ O(1) p │ O(logn)│ O(logn) │ O(n)     │ Heap
ArrayDeque  │ O(1) e │ O(1) e │ O(1) e  │ O(n)     │ Ends

  * Amortized   † Head/tail only   p peek only   e ends only
```

---

## 🎯 Interview Prep

Each module has a dedicated `INTERVIEW_QUESTIONS.md` with questions
**targeted as we gain Java experience** — not definitions, but deep
internals, production tradeoffs, and design decisions.

**Collections topics:**
- HashMap internals — treeification, load factor, power-of-2 capacity
- ConcurrentHashMap vs synchronizedMap vs Hashtable
- hashCode + equals contract and what breaks silently when violated
- Fail-fast vs fail-safe — how `modCount` works under the hood
- LRU Cache design · TimSort internals · Thread safety tradeoffs

**Java 8 topics:**
- Lambda vs anonymous class at JVM level — invokedynamic, singleton non-capturing
- Stream lazy evaluation — vertical processing, short-circuit optimisation
- `thenApply` vs `thenCompose` — the flatMap parallel for CompletableFuture
- `orElse` vs `orElseGet` — why eager evaluation is a hidden performance trap
- Diamond problem resolution — `InterfaceName.super.method()` syntax
- `DateTimeFormatter` thread safety vs `SimpleDateFormat`
- Parallel stream pitfalls — commonPool starvation, shared mutable state

**Concurrency topics:**
- Race conditions, happens-before, synchronized vs volatile
- Deadlock — four conditions, consistent lock ordering, tryLock fix
- ReentrantLock vs synchronized, ReadWriteLock, StampedLock optimistic read
- Thread pool sizing — CPU-bound vs IO-bound
- ForkJoinPool work stealing, why not to block in common pool
- CAS, ABA problem, LongAdder vs AtomicLong under contention

**Java 9 topics:**
- `List.of` vs `Arrays.asList` — mutability, nulls, backing array
- `takeWhile` vs `filter` — order dependency, prefix semantics
- `var` — type inference rules, where it cannot be used
- `String` methods — `isBlank`, `strip`, `lines`, `repeat` (Java 11)
- `HttpClient` — async vs sync, Java 11 replacement for `HttpURLConnection`
- Records — immutability, canonical constructor, when to use vs regular class
- Sealed classes — exhaustive pattern matching, permitted subtypes
- Virtual threads — carrier threads, platform vs virtual, when to use
- Pattern matching `switch` — exhaustiveness, guards, type patterns

**Java 11 topics:**
- strip() vs trim() — Unicode whitespace handling
- lines() vs split("\\n") — line ending handling, Stream vs array
- HttpClient vs HttpURLConnection — async, HTTP/2, thread safety
- Predicate.not() — why it was needed, method reference negation
- var in lambda parameters — annotations, mixing rules

**Java 12-14 topics:**
- Switch statement vs switch expression — fall-through, exhaustiveness, yield
- yield vs return in switch blocks
- Text block indentation — how stripping works, closing """ position
- Pattern matching instanceof — scope rules, vs explicit cast
- Helpful NPEs — what changed, chained call debugging

**Java 15-16 topics:**
- Records — what compiler generates, compact constructor, vs immutable class
- Sealed classes — permits, final/sealed/non-sealed subtypes, exhaustive dispatch
- Records vs regular class — constraints, transparency, when to use each
- Stream.toList() vs collect(toList()) — mutability, null handling
- Sealed + pattern matching — algebraic data types, domain modeling

**Java 17 topics:**
- LTS meaning — enterprise adoption, Spring Boot 3.x requirement
- Wither pattern for records — immutable update
- RandomGenerator API — unified interface, algorithm selection
- Strong encapsulation — what broke migrating from Java 11, --add-opens fix
- sealed vs final — controlled vs no extension
- Java 17 vs Java 11 — migration decision

---

## 🛤️ Progress

| Module                   | Status        | Topics                                                                                                               |
|--------------------------|---------------|----------------------------------------------------------------------------------------------------------------------|
| 📦 Collections Framework | ✅ Complete    | Lists · Set · Map · Queue · Comparable · Iterator · Utils · Real World                                               |
| ⚡ Java 8 Features        | ✅ Complete    | Lambdas · Streams · Optionals · Method Refs · Functional Interfaces · CompletableFuture · DateTime · Default Methods |
| 🔒 Java Concurrency      | ✅ Complete    | Threads · Locks · Executors · Concurrent Collections · Atomic · Fork/Join                                            |
| ☕ Java 9                 | ✅ Complete    | Collection factories · Stream additions · Optional additions · Interface private methods                             |
| ☕ Java 10                | ✅ Complete    | var · Local variable type inference                                                                                  |
| ☕ Java 11                | ✅ Complete    | String methods · Files · HttpClient                                                                                  |
| ☕ Java 12-14             | ✅ Complete    | Switch expressions · Text blocks                                                                                     |
| ☕ Java 15-16             | ✅ Complete    | Records · Sealed classes (preview)                                                                                   |
| ☕ Java 17                | ✅ Complete    | Records · Sealed classes · Pattern matching (LTS)                                                                    |
| ☕ Java 21                | ✅ Complete    | Virtual threads · Pattern matching switch · Sequenced collections (LTS)                                              |
| 🎨 DSA                    | 🚧 In Progess | Arrays · Strings · many more                                                                                         |
| 🎨 Design Patterns       | 📅 Planned    | Creational · Structural · Behavioural with real Spring examples                                                      |

---

*If this helped you, drop a ⭐ — it keeps the motivation going.*