# 📬 Queue

> Covers `PriorityQueue` and `ArrayDeque` — the two Queue implementations
> you will reach for in production and in every coding interview.
> One gives you priority ordering, the other gives you the fastest
> Stack/Queue/Deque in the JDK.

---

## 🧠 Mental Model

```
PriorityQueue (Min-Heap)           ArrayDeque (Circular Buffer)
──────────────────────────         ──────────────────────────────────
        1   ← root (min)            head                        tail
       / \                           ↓                           ↓
      3   2                        [ _ | A | B | C | D | E | _ | _ ]
     / \   \                         ↑ circular — wraps around
    5   4   8
                                  addFirst → moves head left  O(1)
peek()  → root      O(1)          addLast  → moves tail right O(1)
poll()  → root, heapify O(log n)  pollFirst → moves head right O(1)
offer() → insert, bubble O(log n) pollLast → moves tail left O(1)

NOT sorted — only root            No node allocation overhead
is guaranteed to be min           Cache-friendly contiguous memory
```

---

## 📄 Classes in this Module

### `PriorityQueueSamples.java`

| Example      | What it covers |
|--------------|----------------|
| Foundational | ER Triage — min-heap, max-heap, custom Comparator on objects |
| Adv Level    | Top-K largest (min-heap trick), Merge K sorted arrays, Task scheduler |

**Key methods:**
```
pq.offer(element)        // insert — O(log n), bubbles up
pq.poll()                // remove + return min (or max) — O(log n), heapify down
pq.peek()                // view min/max without removing — O(1)
pq.size()                // current number of elements
pq.isEmpty()             // check before peek/poll to avoid NPE

// Min-heap (default)
PriorityQueue<Integer> minHeap = new PriorityQueue<>();

// Max-heap
PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Comparator.reverseOrder());

// Custom object, custom comparator
PriorityQueue<Task> pq = new PriorityQueue<>(
    Comparator.comparingInt(Task::getPriority)
              .thenComparingLong(Task::getDeadline));
```

**When to use PriorityQueue:**
```
✅ Always need the min or max element quickly    → O(1) peek, O(log n) poll
✅ Top-K largest / K smallest problems
✅ Task / job scheduling by priority or deadline
✅ Merge K sorted arrays / streams
✅ Dijkstra's shortest path, A* search

❌ Need FIFO order                               → ArrayDeque
❌ Need sorted iteration of ALL elements        → TreeSet (O(log n) each)
❌ Thread safety needed                          → PriorityBlockingQueue
❌ null elements                                 → throws NullPointerException
```

---

### `ArrayDequeSamples.java`

| Example      | What it covers |
|--------------|----------------|
| Foundational | Stack (LIFO), Queue (FIFO), Deque (both ends), bracket matching |
| Adv Level    | BFS level-order, DFS iterative, Next Greater Element, Sliding Window Maximum |

**Key methods:**
```
// ── As Stack (LIFO) ──
deque.push(e)          // addFirst — O(1)
deque.pop()            // removeFirst — O(1), throws if empty
deque.peek()           // peekFirst — O(1), returns null if empty

// ── As Queue (FIFO) ──
deque.offer(e)         // addLast — O(1)
deque.poll()           // removeFirst — O(1), returns null if empty
deque.peek()           // peekFirst — O(1), returns null if empty

// ── As Deque (both ends) ──
deque.addFirst(e)      // O(1)
deque.addLast(e)       // O(1)
deque.pollFirst()      // O(1), returns null if empty
deque.pollLast()       // O(1), returns null if empty
deque.peekFirst()      // O(1)
deque.peekLast()       // O(1)
```

**When to use ArrayDeque:**
```
✅ Stack — always prefer over java.util.Stack   → no synchronisation overhead
✅ Queue — always prefer over LinkedList        → cache-friendly, less memory
✅ BFS traversal                                → offer/poll pattern
✅ DFS iterative (avoid recursion stack overflow)
✅ Monotonic stack / deque problems
✅ Sliding window maximum/minimum

❌ Priority ordering needed                     → PriorityQueue
❌ Thread safety needed                         → LinkedBlockingDeque
❌ null elements needed                         → throws NullPointerException
```

---

## ⚡ The Two Heap Patterns You Must Know

### Pattern 1 — Top-K

```
Finding K LARGEST → MIN-heap of size K
  Logic: kick out the SMALLEST intruder
  After all elements → heap contains K largest
  heap.peek() = Kth largest element

Finding K SMALLEST → MAX-heap of size K
  Logic: kick out the LARGEST intruder
  After all elements → heap contains K smallest
  heap.peek() = Kth smallest element

Rule: heap type is always OPPOSITE of what you're finding
```

```
// Top-K Largest — O(n log k)
PriorityQueue<Integer> topK = new PriorityQueue<>(); // min-heap
for (int n : nums) {
    topK.offer(n);
    if (topK.size() > k) topK.poll(); // kick out smallest
}
// topK now contains K largest. topK.peek() = Kth largest.

// K Smallest — O(n log k)
PriorityQueue<Integer> kSmallest =
        new PriorityQueue<>(Comparator.reverseOrder()); // max-heap
for (int n : nums) {
    kSmallest.offer(n);
    if (kSmallest.size() > k) kSmallest.poll(); // kick out largest
}
// kSmallest now contains K smallest. kSmallest.peek() = Kth smallest.
```

### Pattern 2 — Monotonic Deque (Sliding Window Max)

```
Goal: find maximum in every window of size K — O(n) not O(n·k)

Deque stores INDICES (not values), front = index of current window max.

For each element i:
  1. Remove front if outside window  (index < i - k + 1)
  2. Remove back while back's value ≤ current value
     (they can never be max while current is in window)
  3. Add current index to back
  4. When window full (i >= k-1): result = nums[deque.front()]
```

```
Deque<Integer> dq = new ArrayDeque<>(); // stores indices
for (int i = 0; i < n; i++) {
    if (!dq.isEmpty() && dq.peekFirst() < i - k + 1)
        dq.pollFirst();                              // outside window
    while (!dq.isEmpty() && nums[dq.peekLast()] <= nums[i])
        dq.pollLast();                               // never useful
    dq.offerLast(i);
    if (i >= k - 1) result[i - k + 1] = nums[dq.peekFirst()];
}
```

---

## ⚡ PriorityQueue vs ArrayDeque — Quick Reference

| | PriorityQueue | ArrayDeque |
|---|---|---|
| Internal structure | Binary heap (array) | Circular array |
| Ordering | By priority (min or max) | Insertion order (FIFO or LIFO) |
| `peek()` | O(1) — min/max | O(1) — front or back |
| `offer()` / `add()` | O(log n) | O(1) amortized |
| `poll()` / `remove()` | O(log n) | O(1) |
| Contains | O(n) | O(n) |
| Null elements | ❌ NPE | ❌ NPE |
| Thread safe | ❌ | ❌ |
| Use as Stack | ❌ | ✅ push/pop/peek |
| Use as Queue | ✅ priority ordering | ✅ FIFO |
| Use as Deque | ❌ | ✅ both ends |

---

## 🔑 Common Mistakes

```
// ❌ WRONG — using java.util.Stack (synchronised, legacy)
Stack<Integer> stack = new Stack<>();
stack.push(1);

// ✅ CORRECT — ArrayDeque is faster, no lock overhead
Deque<Integer> stack = new ArrayDeque<>();
stack.push(1);

// ❌ WRONG — PriorityQueue is NOT sorted on iteration
PriorityQueue<Integer> pq = new PriorityQueue<>();
pq.offer(3); pq.offer(1); pq.offer(2);
System.out.println(pq); // [1, 3, 2] — heap order, not sorted!

// ✅ CORRECT — only poll() gives sorted order
while (!pq.isEmpty()) System.out.print(pq.poll() + " "); // 1 2 3

// ❌ WRONG — K Largest with MAX-heap (wastes memory, doesn't give Kth)
PriorityQueue<Integer> wrong = new PriorityQueue<>(Comparator.reverseOrder());
// adds ALL elements, never evicts — O(n log n) time, O(n) space

// ✅ CORRECT — K Largest with MIN-heap of size K — O(n log k) time, O(k) space
PriorityQueue<Integer> topK = new PriorityQueue<>();
for (int n : nums) {
    topK.offer(n);
    if (topK.size() > k) topK.poll();
}

// ❌ WRONG — modifying comparator field after adding to PriorityQueue
task.setPriority(1); // mutating after insertion — heap property violated!

// ✅ CORRECT — remove, update, re-insert
pq.remove(task);
task.setPriority(1);
pq.offer(task);
```