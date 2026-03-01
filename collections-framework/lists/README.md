# 📋 Lists

> Covers `ArrayList` and `LinkedList` — the two most commonly used
> List implementations in Java. Each class has a foundational example
> and a senior-level real-world example.

---

## 🧠 Mental Model

```
ArrayList                              LinkedList
───────────────────────────────        ──────────────────────────────────
[ 0 ][ 1 ][ 2 ][ 3 ][ 4 ][ 5 ]       [A] ⟷ [B] ⟷ [C] ⟷ [D] ⟷ [E]

Backed by Object[]                     Doubly-linked nodes
Contiguous memory (cache friendly)     Scattered heap nodes
Great for: random access by index      Great for: head/tail insert/remove
Bad for:   insert/delete in middle     Bad for:   random access by index
```

---

## 📄 Classes in this Module

### `ArrayListSamples.java`

| Example | What it covers |
|---------|----------------|
| Foundational | Student waitlist — add, get, set, remove, contains, subList, sort |
| Senior Level | E-commerce orders — pre-sizing, removeIf, Comparator chaining, Stream partitioning, ListIterator, unmodifiableList, trimToSize |

**Key methods:**
```
list.add(element)                // O(1) amortized — appends to end
list.add(index, element)         // O(n) — shifts right from index
list.get(index)                  // O(1) — ArrayList's biggest strength
list.set(index, value)           // O(1) — in-place replace
list.remove(index)               // O(n) — shifts left after removal
list.remove(Object)              // O(n) — scans then shifts
list.contains(element)           // O(n) — linear scan
list.subList(from, to)           // O(1) — returns a VIEW, not a copy
list.removeIf(predicate)         // O(n) — safe bulk removal (Java 8+)
list.sort(comparator)            // O(n log n) — TimSort in-place
Collections.unmodifiableList()   // read-only wrapper — defensive programming
((ArrayList) list).trimToSize()  // shrinks internal array to exact size
```

**When to use ArrayList:**
```
✅ Random access by index is frequent        → O(1)
✅ Most inserts happen at the END            → O(1) amortized
✅ Iteration is the primary operation
✅ Approximate size is known upfront         → pre-size to avoid resizes

❌ Frequent insert/delete in the MIDDLE      → O(n) shifts, use LinkedList
❌ Thread safety needed                      → use CopyOnWriteArrayList
❌ Only unique elements needed               → use HashSet
```

---

### `LinkedListSamples.java`

| Example | What it covers |
|---------|----------------|
| Foundational | Browser history (Stack) + Print queue (Queue) |
| Senior Level | Text editor Undo/Redo — two-stack pattern with O(1) ops at both ends |

**Key methods:**
```
deque.push(element)        // addFirst — O(1) LIFO stack push
deque.pop()                // removeFirst — O(1) LIFO stack pop
deque.peek()               // peekFirst — O(1) view without removing
queue.offer(element)       // addLast — O(1) FIFO enqueue
queue.poll()               // removeFirst — O(1) FIFO dequeue
list.addFirst(element)     // O(1) — insert at head
list.addLast(element)      // O(1) — insert at tail
list.peekFirst()           // O(1) — view head
list.peekLast()            // O(1) — view tail
list.descendingIterator()  // iterate in reverse order
```

**When to use LinkedList:**
```
✅ Frequent insert/remove at HEAD or TAIL    → O(1)
✅ Need one object acting as List + Deque
✅ Implementing Undo/Redo, browser history

❌ Random access by index                    → O(n) traversal, use ArrayList
❌ Pure Stack or Queue                       → ArrayDeque is faster
❌ Memory is tight                           → 2 extra pointers per node
```

---

## ⚡ ArrayList vs LinkedList — Quick Comparison

| Operation | ArrayList | LinkedList | Winner |
|-----------|-----------|------------|--------|
| `get(index)` | O(1) | O(n) | ArrayList ✅ |
| `add()` at end | O(1)* | O(1) | Tie |
| `add()` at start | O(n) | O(1) | LinkedList ✅ |
| `remove()` at start | O(n) | O(1) | LinkedList ✅ |
| `remove()` at end | O(1) | O(1) | Tie |
| `contains()` | O(n) | O(n) | Tie |
| Memory | Less | More (pointers) | ArrayList ✅ |
| Cache friendly | Yes | No | ArrayList ✅ |

> **Rule of thumb:** Default to `ArrayList`.
> Switch to `LinkedList` only for frequent head/tail ops with no random access.
> Switch to `ArrayDeque` if you only need Stack or Queue behaviour.

---

## 🔑 Common Mistakes

```
// ❌ WRONG — ConcurrentModificationException!
for (String s : list) {
    if (s.equals("X")) list.remove(s);
}

// ✅ CORRECT — removeIf (Java 8+, cleanest)
list.removeIf(s -> s.equals("X"));

// ✅ CORRECT — Iterator.remove()
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    if (it.next().equals("X")) it.remove();
}

// ❌ WRONG — Arrays.asList() returns FIXED SIZE list
List<String> fixed = Arrays.asList("A", "B", "C");
fixed.add("D");  // throws UnsupportedOperationException!

// ✅ CORRECT — wrap in ArrayList to make mutable
List<String> mutable = new ArrayList<>(Arrays.asList("A", "B", "C"));
mutable.add("D");  // works fine

// ❌ WRONG — remove(int) vs remove(Object) confusion
List<Integer> nums = new ArrayList<>(Arrays.asList(1, 2, 3));
nums.remove(1);              // removes at INDEX 1 → list = [1, 3]
nums.remove(Integer.valueOf(1)); // removes VALUE 1 → list = [2, 3]
```