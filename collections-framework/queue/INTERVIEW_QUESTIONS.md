# 🎯 Interview Questions — Queue

---

> Queue questions as we gain experience should focus on heap internals, the Top-K pattern
> and why the heap type is counterintuitive, monotonic stack/deque problems,
> and why ArrayDeque beats both Stack class and LinkedList in every scenario.

---

## PriorityQueue Internals

**Q1. How does a PriorityQueue work internally? What data structure backs it?**

> Backed by `Object[] queue` — an array representation of a **binary min-heap**.
>
> Heap properties:
> - Parent of node at index `i` = `(i - 1) / 2`
> - Left child of `i` = `2i + 1`
> - Right child of `i` = `2i + 2`
> - Heap invariant: every parent ≤ both children (min-heap)
>
> `offer(e)` — add at end of array, then **bubble up**: swap with parent while element < parent. O(log n).
>
> `poll()` — remove root (min), move last element to root, then **sift down**: swap with smaller child while element > either child. O(log n).
>
> `peek()` — just read index 0 (the root). O(1).
>
> The array is NOT sorted. Only the root is guaranteed to be the minimum. Iterating a PriorityQueue does not give sorted order — only repeated `poll()` does.

---

**Q2. Why is iterating a PriorityQueue not in sorted order?**

> The heap property only guarantees each parent ≤ its children — not that all elements are in sorted order. The internal array layout is heap order, not sorted order.
>
> ```
> PriorityQueue<Integer> pq = new PriorityQueue<>();
> pq.offer(5); pq.offer(1); pq.offer(3); pq.offer(2); pq.offer(4);
> System.out.println(pq);          // [1, 2, 3, 5, 4] — heap order
> // NOT [1, 2, 3, 4, 5]
> ```
>
> To get sorted output you must `poll()` repeatedly — each poll removes the current minimum and re-heapifies. O(n log n) total — equivalent to heap sort.
>
> Common interview mistake: iterating or converting to list expecting sorted order.

---

**Q3. You need the K largest elements from a stream of 1 million numbers. Walk me through the optimal solution.**

> Use a **min-heap of size K**. This is counterintuitive but correct:
>
> ```
> PriorityQueue<Integer> topK = new PriorityQueue<>(); // min-heap
> for (int n : stream) {
>     topK.offer(n);
>     if (topK.size() > k) topK.poll(); // evict current minimum
> }
> // topK contains the K largest. topK.peek() = Kth largest.
> ```
>
> Why min-heap for K largest?
> - The heap acts as a filter — it keeps the K largest seen so far
> - When a new element arrives: if it's larger than the current minimum (heap top), the minimum gets evicted and the new element stays
> - If it's smaller than the minimum, poll() removes it immediately
> - After all elements: heap contains exactly the K largest
>
> Complexity: O(n log k) time, O(k) space — far better than sorting (O(n log n)) or keeping all elements.
>
> **The rule:** heap type is always OPPOSITE of what you're finding.
> - K largest → min-heap (kick out smallest intruders)
> - K smallest → max-heap (kick out largest intruders)

---

**Q4. Trace through finding K=3 smallest from [3, 1, 4, 1, 5, 9] using a max-heap.**

> ```
> Max-heap of size K=3. poll() removes LARGEST when overflow.
>
> offer(3) → heap: [3]           size=1, ok
> offer(1) → heap: [3, 1]        size=2, ok
> offer(4) → heap: [4, 3, 1]     size=3, ok  ← 4 at top (max)
> offer(1) → heap: [4, 3, 1, 1]  size=4 > 3 → poll() removes 4
>            heap: [3, 1, 1]
> offer(5) → heap: [5, 3, 1, 1]  size=4 > 3 → poll() removes 5
>            heap: [3, 1, 1]      ← 5 kicked out immediately
> offer(9) → heap: [9, 3, 1, 1]  size=4 > 3 → poll() removes 9
>            heap: [3, 1, 1]      ← 9 kicked out immediately
>
> Final heap: [3, 1, 1] ✅ — the 3 smallest elements
> heap.peek() = 3 = Kth smallest
> ```
>
> Key insight: large intruders (5, 9) are immediately ejected because they are the max of the heap. The heap naturally self-selects the K smallest.

---

**Q5. How would you merge K sorted arrays efficiently using a PriorityQueue?**

> Use a min-heap that stores `(value, arrayIndex, elementIndex)` tuples. Seed with the first element of each array. Poll min → add to result → push next element from the same array.
>
> ```
> PriorityQueue<int[]> heap =
>     new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
>
> // Seed with first element of each array
> for (int i = 0; i < arrays.length; i++)
>     heap.offer(new int[]{arrays[i][0], i, 0}); // {value, arrIdx, elemIdx}
>
> List<Integer> result = new ArrayList<>();
> while (!heap.isEmpty()) {
>     int[] curr = heap.poll();
>     result.add(curr[0]);
>     int arrIdx = curr[1], nextIdx = curr[2] + 1;
>     if (nextIdx < arrays[arrIdx].length)
>         heap.offer(new int[]{arrays[arrIdx][nextIdx], arrIdx, nextIdx});
> }
> ```
>
> Complexity: O(n log k) where n = total elements, k = number of arrays. The heap size never exceeds k. This is the pattern used in external merge sort and database merge joins.

---

## ArrayDeque Internals

**Q6. How does ArrayDeque work internally? How is it different from LinkedList as a Queue?**

> `ArrayDeque` is backed by a **circular array** (`Object[] elements`) with two pointers: `head` and `tail`.
>
> - `addFirst()` — decrement head (wraps around), store at new head. O(1)
> - `addLast()` — store at tail, increment tail (wraps around). O(1)
> - `pollFirst()` — read head, null it out, increment head. O(1)
> - `pollLast()` — decrement tail, read, null out. O(1)
> - When full → resize to 2×, copy elements to straighten out the circular layout. O(n) amortized.
>
> vs `LinkedList`:
> - `LinkedList` allocates a new `Node` object for every element — heap allocation + GC pressure
> - `ArrayDeque` reuses the same array — cache-friendly, no node overhead
> - `ArrayDeque` is faster in practice for both Stack and Queue use cases
> - `LinkedList` wins only when you need O(1) insert/remove from the middle (rare)

---

**Q7. Why should you never use `java.util.Stack` in new code?**

> `java.util.Stack` extends `Vector` which synchronises every method with a single mutex — including `push()`, `pop()`, `peek()`, `isEmpty()`, and `size()`. In single-threaded code (which is almost always the case for a stack) you pay lock acquisition/release overhead on every operation for no benefit.
>
> `ArrayDeque` has no synchronisation and is significantly faster:
> ```
> // NEVER in new code
> Stack<Integer> stack = new Stack<>();
>
> // ALWAYS prefer
> Deque<Integer> stack = new ArrayDeque<>();
> stack.push(1);
> stack.pop();
> stack.peek();
> ```
>
> Java's own Javadoc for `Stack` says: "A more complete and consistent set of LIFO stack operations is provided by the Deque interface and its implementations, which should be used in preference to this class."

---

## Patterns

**Q8. Explain the monotonic stack pattern. When do you use it?**

> A monotonic stack maintains elements in strictly increasing or decreasing order. When a new element violates the monotonic property, elements are popped until the property is restored.
>
> Use for: **Next Greater Element**, **Previous Smaller Element**, **largest rectangle in histogram**, **daily temperatures**.
>
> **Next Greater Element pattern (decreasing stack):**
> ```
> Deque<Integer> stack = new ArrayDeque<>(); // stores indices
> int[] result = new int[n];
> Arrays.fill(result, -1);
>
> for (int i = 0; i < n; i++) {
>     // Current element is NGE for everything smaller at top of stack
>     while (!stack.isEmpty() && arr[stack.peek()] < arr[i])
>         result[stack.pop()] = arr[i];
>     stack.push(i);
> }
> ```
>
> Each element is pushed once and popped once → O(n) total despite the nested loop.

---

**Q9. Explain the sliding window maximum problem and why a monotonic deque gives O(n) instead of O(n·k).**

> Naive approach: for each window of size k, scan all k elements to find max → O(n·k).
>
> Monotonic deque insight: maintain a deque of indices where values are in **decreasing order**. The front is always the index of the current window's maximum.
>
> Key operations per element:
> 1. Remove front if outside window — O(1)
> 2. Remove back while back value ≤ current — those values are useless (current is newer and larger, so they will never be max while current is in window)
> 3. Add current index to back
> 4. Front of deque = window max
>
> Each index is added once and removed once → O(n) total.
>
> ```
> Deque<Integer> dq = new ArrayDeque<>();
> for (int i = 0; i < n; i++) {
>     if (!dq.isEmpty() && dq.peekFirst() < i - k + 1)
>         dq.pollFirst();                                  // expired
>     while (!dq.isEmpty() && nums[dq.peekLast()] <= nums[i])
>         dq.pollLast();                                   // useless
>     dq.offerLast(i);
>     if (i >= k - 1) result[i - k + 1] = nums[dq.peekFirst()];
> }
> ```

---

**Q10. What is the difference between `offer()`/`poll()`/`peek()` and `add()`/`remove()`/`element()`?**

> Both sets do the same thing but differ in behaviour when the queue is empty or full:
>
> | Method | Queue empty / full | Returns |
> |--------|-------------------|---------|
> | `offer(e)` | returns false if full | boolean |
> | `add(e)` | throws IllegalStateException if full | boolean |
> | `poll()` | returns null if empty | element or null |
> | `remove()` | throws NoSuchElementException if empty | element |
> | `peek()` | returns null if empty | element or null |
> | `element()` | throws NoSuchElementException if empty | element |
>
> For unbounded queues (ArrayDeque, PriorityQueue) full capacity is never an issue so `offer()` and `add()` behave identically. Always prefer `offer()`/`poll()`/`peek()` — they never throw unexpected exceptions and are safer in production.

---

## Design & Production

**Q11. In a task scheduler, tasks arrive continuously and you always need to process the highest-priority task next. How do you implement this?**

> `PriorityQueue` with a custom comparator. For production, wrap with `PriorityBlockingQueue` for thread safety:
>
> ```
> // Single-threaded
> PriorityQueue<Task> scheduler = new PriorityQueue<>(
>     Comparator.comparingInt(Task::getPriority)
>               .thenComparingLong(Task::getDeadline));
>
> // Multi-threaded — producer threads submit, one consumer processes
> BlockingQueue<Task> scheduler = new PriorityBlockingQueue<>(11,
>     Comparator.comparingInt(Task::getPriority));
>
> // Consumer thread
> while (true) {
>     Task task = scheduler.take(); // blocks until available
>     process(task);
> }
> ```
>
> `PriorityBlockingQueue` is unbounded and thread-safe. `take()` blocks when empty. Used internally by Java's `ThreadPoolExecutor` when you pass a `PriorityBlockingQueue` as the work queue.

---

**Q12. You need to find the median of a data stream at any point. How do you use two heaps?**

> Maintain two heaps: a max-heap for the lower half and a min-heap for the upper half. The median is always at the tops of these heaps.
>
> ```
> PriorityQueue<Integer> lower = new PriorityQueue<>(Comparator.reverseOrder()); // max-heap
> PriorityQueue<Integer> upper = new PriorityQueue<>();                           // min-heap
>
> void addNum(int num) {
>     lower.offer(num);
>     upper.offer(lower.poll());    // balance: push lower's max to upper
>     if (lower.size() < upper.size())
>         lower.offer(upper.poll()); // keep lower size >= upper size
> }
>
> double findMedian() {
>     if (lower.size() > upper.size()) return lower.peek();
>     return (lower.peek() + upper.peek()) / 2.0;
> }
> ```
>
> Invariant: `lower.size() == upper.size()` or `lower.size() == upper.size() + 1`.
> All elements in lower ≤ all elements in upper.
> Median = lower.peek() (odd count) or average of both tops (even count).
> Each `addNum()` is O(log n). `findMedian()` is O(1).

---

**Q13. When would you use `PriorityBlockingQueue` over `PriorityQueue`?**

> `PriorityQueue` is not thread-safe. Concurrent access corrupts the internal heap array. Use `PriorityBlockingQueue` when:
>
> 1. **Producer-consumer pattern** — multiple threads submit tasks, one or more threads consume in priority order
> 2. **Thread pool with priority** — pass to `ThreadPoolExecutor` constructor to execute higher-priority Runnables first
> 3. **Event processing pipelines** — events arrive from multiple threads and must be processed by severity
>
> `PriorityBlockingQueue` is unbounded (grows automatically), thread-safe, and `take()` blocks the consumer thread when empty — which is exactly what a consumer thread should do instead of busy-waiting.
>
> For bounded priority queues with backpressure, consider a custom `SynchronousQueue` wrapper or a library like `Disruptor`.

---

**Q14. How would you implement BFS and DFS iteratively using ArrayDeque?**

> **BFS — uses ArrayDeque as Queue (FIFO):**
> ```
> Queue<Node> queue = new ArrayDeque<>();
> queue.offer(root);
> while (!queue.isEmpty()) {
>     Node node = queue.poll();        // process front
>     visit(node);
>     for (Node child : node.children)
>         queue.offer(child);          // add to back
> }
> ```
>
> **DFS — uses ArrayDeque as Stack (LIFO):**
> ```
> Deque<Node> stack = new ArrayDeque<>();
> stack.push(root);
> while (!stack.isEmpty()) {
>     Node node = stack.pop();         // process top
>     if (!visited(node)) {
>         visit(node);
>         for (Node child : node.children)
>             stack.push(child);       // add to top
>     }
> }
> ```
>
> Iterative DFS avoids `StackOverflowError` on deep graphs — recursive DFS uses the JVM call stack which is typically limited to ~500–1000 deep frames. Iterative DFS uses heap memory which is only limited by available RAM.

---

**Q15. What is the time complexity of `contains()` on a PriorityQueue and why does it matter?**

> `PriorityQueue.contains()` is **O(n)** — it linearly scans the internal array. There is no index structure. This matters significantly for the "update priority" use case:
>
> ```
> // WRONG approach — O(n) contains + O(n) remove + O(log n) offer
> if (pq.contains(task)) {
>     pq.remove(task); // O(n) scan
>     task.setPriority(newPriority);
>     pq.offer(task);
> }
> ```
>
> For frequent priority updates on large heaps this becomes a bottleneck. Production solutions:
>
> 1. **Lazy deletion** — mark old entry as invalid, add new entry, skip invalids on poll:
> ```
> Set<Task> invalidated = new HashSet<>();
> pq.offer(newTask);
> invalidated.add(oldTask);
> // In poll loop:
> while (!pq.isEmpty() && invalidated.contains(pq.peek()))
>     pq.poll(); // skip
> Task next = pq.poll();
> ```
>
> 2. **Indexed priority queue** — maintain a HashMap from element to index in the heap array. Allows O(log n) decrease-key. Used in production graph algorithms (Dijkstra with updates).