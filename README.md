```
   ██╗ █████╗ ██╗   ██╗ █████╗     ██████╗  █████╗  ██████╗██╗  ██╗███████╗███╗   ██╗██████╗
   ██║██╔══██╗██║   ██║██╔══██╗    ██╔══██╗██╔══██╗██╔════╝██║ ██╔╝██╔════╝████╗  ██║██╔══██╗
   ██║███████║██║   ██║███████║    ██████╔╝███████║██║     █████╔╝ █████╗  ██╔██╗ ██║██║  ██║
██ ██║██╔══██║╚██╗ ██╔╝██╔══██║    ██╔══██╗██╔══██║██║     ██╔═██╗ ██╔══╝  ██║╚██╗██║██║  ██║
╚████╔╝██║  ██║ ╚████╔╝ ██║  ██║    ██████╔╝██║  ██║╚██████╗██║  ██╗███████╗██║ ╚████║██████╔╝
 ╚═══╝ ╚═╝  ╚═╝  ╚═══╝  ╚═╝  ╚═╝    ╚═════╝ ╚═╝  ╚═╝ ╚═════╝╚═╝  ╚═╝╚══════╝╚═╝  ╚═══╝╚═════╝
```

## 🧭 What Is This?

A **structured, multi-module Java learning repository** built for developers who want to go beyond tutorials. Every concept is covered with:

- ✅ **Two levels per topic** — foundational (what & how) + adv level (why & when)
- ✅ **Production patterns** — real code you'd write at work, not toy examples
- ✅ **Heavy inline comments** — *why* explained at every step
- ✅ **Interview prep** — each module has its own `INTERVIEW_QUESTIONS.md`
- ✅ **Runnable** — every file has a `main()` and can be run independently

---

## 📦 Module 1 — Collections Framework

```
  collections-framework/
  │
  ├── 📋 lists ────────  ArrayList · LinkedList
  ├── 🔵 set ──────────  HashSet · LinkedHashSet · TreeSet
  ├── 🗂️  map ──────────  HashMap · LinkedHashMap · TreeMap · ConcurrentHashMap
  ├── 📬 queue ─────────  PriorityQueue · ArrayDeque
  ├── ⚖️  comparable ───  Comparable vs Comparator
  ├── 🔁 iterator ─────  Iterator · ListIterator · Fail-Fast · Fail-Safe
  ├── 🛠️  utils ─────────  java.util.Collections utility class
  └── 🌍 realworld ────  Word Frequency · Task Scheduler · Sliding Window Max
```

Each topic folder contains:
```
  <topic>/
    ├── src/main/java/com/javabackend/collections/<topic>/
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

Topics covered:
- HashMap internals — treeification, load factor, power-of-2 capacity
- ConcurrentHashMap vs synchronizedMap vs Hashtable
- hashCode + equals contract and what breaks silently when violated
- Fail-fast vs fail-safe — how `modCount` works under the hood
- LRU Cache design · TimSort internals · Thread safety tradeoffs
- NavigableMap range queries · Memory traps: subList(), trimToSize()

---

## 🛤️ Progress

| Module | Status | Topics |
|--------|--------|--------|
| 📦 Collections Framework | 🚧 In Progress | Lists · Set · Map · Queue · Comparable · Iterator · Utils · Real World |
| ⚡ Java 8 Features | 🔜 Next | Lambdas · Streams · Optionals · Method Refs · Functional Interfaces |
| 🎨 Design Patterns | 📅 Planned | Creational · Structural · Behavioural with real Spring examples |

---

*If this helped you, drop a ⭐ — it keeps the motivation going.*