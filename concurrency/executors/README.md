# ⚙️ Executors

> Thread pools decouple task submission from execution. Instead of creating
> a new OS thread per task, threads are reused — cheaper, faster, bounded.

---

## 🧠 Core Concepts

- **Executor** — submit a `Runnable`, fire and forget
- **ExecutorService** — submit `Callable`, get `Future`, manage lifecycle
- **ThreadPoolExecutor** — full control: pool size, queue, rejection policy
- **ScheduledExecutorService** — run tasks after delay or at fixed intervals
- **ForkJoinPool** — work stealing, recursive task decomposition

---

## 📄 Classes in this Module

### `ExecutorSamples.java`

| Example | What it covers |
|---------|----------------|
| Executors Factory | fixed, cached, single, virtual thread pools |
| Future and Callable | submit, get, timeout, invokeAll, invokeAny |
| ThreadPoolExecutor | full constructor, sizing, diagnostics, rejection policies |
| ScheduledExecutor | schedule, scheduleAtFixedRate, scheduleWithFixedDelay |
| ForkJoin | RecursiveTask, fork/join pattern, work stealing, common pool |

---

## ⚡ Executors Factory Methods

```
// Fixed — N threads, unbounded queue
// Use for CPU-bound tasks: N = Runtime.availableProcessors()
ExecutorService fixed = Executors.newFixedThreadPool(4);

// Cached — threads on demand, 60s keepalive
// Dangerous under burst — can spawn thousands of threads
ExecutorService cached = Executors.newCachedThreadPool();

// Single — one thread, tasks run in submission order
// Use for sequencing, actor pattern
ExecutorService single = Executors.newSingleThreadExecutor();

// Virtual (Java 21+) — one virtual thread per task
// Best for I/O-bound — lightweight, no pooling needed
ExecutorService virtual = Executors.newVirtualThreadPerTaskExecutor();

// Scheduled — run after delay or at fixed intervals
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
```

---

## ⚡ Future and Callable

```
// Submit a Callable — returns a Future
Future<String> future = pool.submit(() -> {
    Thread.sleep(100);
    return "result";
});

future.isDone();                             // non-blocking check
String result = future.get();               // blocking — wait for result
String result = future.get(200, MILLISECONDS); // wait max 200ms → TimeoutException
future.cancel(true);                        // interrupt running task

// invokeAll — submit all, wait for all to complete
List<Future<Integer>> futures = pool.invokeAll(taskList);
for (Future<Integer> f : futures) {
    results.add(f.get()); // each get() is already done — no wait
}

// invokeAny — first to complete wins, rest cancelled
String first = pool.invokeAny(taskList);
```

---

## ⚡ ThreadPoolExecutor

```
new ThreadPoolExecutor(
    2,                            // corePoolSize   — always alive
    4,                            // maxPoolSize    — created under surge
    30, TimeUnit.SECONDS,         // keepAlive      — extra threads idle timeout
    new ArrayBlockingQueue<>(10), // workQueue      — bounded = backpressure
    threadFactory,                // name your threads
    new CallerRunsPolicy()        // rejection policy
);
```

**Pool sizing:**
- CPU-bound: `corePoolSize = Runtime.availableProcessors()`
- IO-bound: `corePoolSize = processors × (1 + waitTime / computeTime)`

**Rejection policies (queue full + max threads reached):**
- `AbortPolicy` — throw `RejectedExecutionException` (default)
- `CallerRunsPolicy` — caller thread runs the task (natural backpressure)
- `DiscardPolicy` — silently drop the task
- `DiscardOldestPolicy` — drop oldest queued task, retry submit

**Diagnostics:**
```
pool.getPoolSize()          // current thread count
pool.getActiveCount()       // threads executing tasks
pool.getQueue().size()      // tasks waiting in queue
pool.getCompletedTaskCount()// tasks finished
```

---

## ⚡ ScheduledExecutorService

```
// Once after delay
scheduler.schedule(task, 100, MILLISECONDS);

// Every 100ms — measured from START of previous execution
// If task takes longer than period, next run starts immediately
scheduler.scheduleAtFixedRate(task, 50, 100, MILLISECONDS);

// 150ms AFTER previous execution ENDS
// Guarantees a gap between runs — safer for variable-duration tasks
scheduler.scheduleWithFixedDelay(task, 50, 150, MILLISECONDS);

// Cancel a scheduled task
ScheduledFuture<?> sf = scheduler.scheduleAtFixedRate(...);
sf.cancel(false); // false = don't interrupt if running
```

**When to use which:**
- `scheduleAtFixedRate` — heartbeat, metrics collection, polling
- `scheduleWithFixedDelay` — cleanup jobs, retry loops, anything that must not overlap

---

## ⚡ ForkJoinPool

```
// RecursiveTask — splits work, merges results
class SumTask extends RecursiveTask<Long> {
    protected Long compute() {
        if (size <= THRESHOLD) {
            return computeDirectly(); // base case
        }
        SumTask left  = new SumTask(leftHalf);
        SumTask right = new SumTask(rightHalf);
        left.fork();                     // async — push to deque
        long rightResult = right.compute(); // run right in current thread
        long leftResult  = left.join();     // wait for left
        return leftResult + rightResult;
    }
}

ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
long result = pool.invoke(new SumTask(data, 0, data.length));

// Common pool — shared JVM-wide, used by parallel streams + CompletableFuture
ForkJoinPool.commonPool().invoke(task);
// NEVER block in common pool tasks — starves parallel streams
```

**Work stealing:** each worker has its own deque. Idle workers steal tasks
from the tail of busy workers' deques — minimises contention, maximises CPU utilisation.

---

## 🔑 Lifecycle

```
pool.shutdown();                          // stop accepting new tasks, finish queued
pool.shutdownNow();                       // interrupt running, return queued tasks
pool.awaitTermination(10, SECONDS);       // wait for clean shutdown
pool.isShutdown();                        // shutdown initiated?
pool.isTerminated();                      // all tasks done?
```

**Always shut down pools** — non-daemon threads keep JVM alive.

---

## 🔑 Common Mistakes

```
// ❌ newCachedThreadPool under burst — creates unbounded threads → OOM
ExecutorService pool = Executors.newCachedThreadPool();
// 10,000 tasks submitted → 10,000 threads → OutOfMemoryError

// ✅ Use bounded pool with bounded queue
new ThreadPoolExecutor(10, 20, 60s, new ArrayBlockingQueue<>(1000), ...);

// ❌ Ignoring Future exceptions — failures silently swallowed
pool.submit(() -> { throw new RuntimeException("failed"); });
// Nothing printed — exception sits in Future, never retrieved

// ✅ Always call get() or handle the Future
Future<?> f = pool.submit(task);
try { f.get(); } catch (ExecutionException e) { log(e.getCause()); }

// ❌ Blocking in ForkJoinPool common pool
ForkJoinPool.commonPool().submit(() -> {
    Thread.sleep(5000); // blocks a common pool thread → starves parallel streams
});

// ❌ Not shutting down pools — JVM never exits
ExecutorService pool = Executors.newFixedThreadPool(4);
// ... use pool ...
// forgot pool.shutdown() → JVM hangs
```

---