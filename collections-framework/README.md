# 📦 Collections Framework

> A complete, runnable reference for the Java Collections Framework.
> Every topic has two examples — **foundational** and **adv level** —
> with inline comments explaining the **why**, not just the **what**.

---

## 🗺️ Collection Hierarchy

```
java.util
│
├── Collection (Interface)
│   │
│   ├── List ──────────────── Ordered, allows duplicates
│   │   ├── ArrayList         Random access O(1), dynamic array
│   │   └── LinkedList        Fast insert/delete at ends O(1)
│   │
│   ├── Set ──────────────── No duplicates
│   │   ├── HashSet           Fastest, no order guarantee
│   │   ├── LinkedHashSet     Insertion order preserved
│   │   └── TreeSet           Always sorted (Red-Black Tree)
│   │
│   └── Queue ────────────── FIFO / Priority ordering
│       ├── PriorityQueue     Min-heap, smallest element first
│       └── ArrayDeque        Double-ended, faster than Stack/LinkedList
│
└── Map (Interface) ───────── Key-Value pairs
    ├── HashMap               Fastest, no order
    ├── LinkedHashMap         Insertion order preserved
    ├── TreeMap               Sorted by keys
    └── ConcurrentHashMap     Thread-safe HashMap
```

---

## 🧭 Which Collection to Use?

```
Need KEY-VALUE pairs?
  YES ──▶ MAP
          ├── Fastest, no order       →  HashMap
          ├── Insertion order         →  LinkedHashMap
          ├── Sorted keys             →  TreeMap
          ├── Thread-safe             →  ConcurrentHashMap
          └── LRU Cache               →  LinkedHashMap(accessOrder=true)

  NO  ──▶ COLLECTION
          │
          ├── Unique elements only?
          │     YES ──▶ SET
          │             ├── Fastest, no order    →  HashSet
          │             ├── Insertion order      →  LinkedHashSet
          │             └── Sorted               →  TreeSet
          │
          └── Duplicates allowed?
                        ├── Random access by index   →  ArrayList
                        ├── Fast head/tail ops        →  LinkedList
                        ├── FIFO Queue                →  ArrayDeque
                        ├── LIFO Stack                →  ArrayDeque
                        └── Priority ordering         →  PriorityQueue
```

---

## 📂 Module Structure

```
collections-framework/
│
├── lists/              ArrayList · LinkedList
```

Each module contains:
```
<module>/
  src/main/java/com/javabackend/collections/<module>/
  README.md                ← concept, methods, when to use
  INTERVIEW_QUESTIONS.md   ← topic-specific Q&A for interviews
  pom.xml
```

---

## 📖 Learning Order

| # | Module | Class | Key Concept |
|---|--------|-------|-------------|
| 1 | lists | `ArrayListSamples` | Dynamic array, O(1) access, removeIf |
| 2 | lists | `LinkedListSamples` | Undo/Redo with two-stack pattern |

---

## 📊 Big-O Quick Reference

| Collection | Get | Add | Remove | Contains | Ordered? |
|------------|-----|-----|--------|----------|----------|
| ArrayList | O(1) | O(1)* | O(n) | O(n) | By index |
| LinkedList | O(n) | O(1)† | O(1)† | O(n) | By index |