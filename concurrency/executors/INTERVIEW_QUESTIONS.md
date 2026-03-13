# 🎯 Interview Questions — Executors

---

**Q1. Why use a thread pool instead of creating a new thread per task?**

> Creating an OS thread is expensive — JVM allocates a stack (512KB–1MB
> by default), the OS schedules it, and context switching has overhead.
> For short-lived tasks, thread creation can take longer than the task itself.
>
> Thread pools solve this by reusing threads:
> - Threads are created once and kept alive to handle multiple tasks
> - Task submission is just queue insertion — no thread creation cost
> - Pool size is bounded — prevents unbounded resource consumption
> - Lifecycle is managed — shutdown, monitoring, rejection handling
>
> Production rule: never use `new Thread(task).start()` for application
> work. Always use an `ExecutorService`. The only exception is long-lived
> background threads (heartbeat, daemon) that exist for the JVM's lifetime.

---

**Q2. What is the difference between `submit()` and `execute()`?**

> `execute(Runnable)` — fire and forget. No return value. Exceptions thrown
> inside the task go to the thread's `UncaughtExceptionHandler` and are
> otherwise invisible:
>
> ```
> pool.execute(() -> { throw new RuntimeException("lost"); });
> // Exception printed to stderr — easy to miss in production
> ```
>
> `submit(Callable/Runnable)` — returns a `Future`. Exceptions are captured
> inside the `Future` and rethrown when you call `get()`:
>
> ```
> Future<?> f = pool.submit(() -> { throw new RuntimeException("captured"); });
> f.get(); // throws ExecutionException wrapping the RuntimeException
> ```
>
> In production always prefer `submit()` — you get a handle to the task,
> can check completion, cancel it, and retrieve exceptions. Silently lost
> failures are a common source of hard-to-debug production bugs.

---

**Q3. What happens when you call `future.get()` and the task threw an exception?**

> The exception is wrapped in an `ExecutionException` and rethrown from
> `get()`. The original cause is accessible via `getCause()`:
>
> ```
> Future<String> f = pool.submit(() -> {
>     throw new IllegalStateException("task failed");
> });
>
> try {
>     f.get();
> } catch (ExecutionException e) {
>     Throwable cause = e.getCause(); // IllegalStateException
>     log.error("Task failed", cause);
> } catch (InterruptedException e) {
>     Thread.currentThread().interrupt();
> }
> ```
>
> If you never call `get()`, the exception is permanently silenced — one
> of the most common bugs in concurrent code. If you don't need the result
> but do need to know about failures, at minimum call `get()` in a
> try-catch after task completion, or use `CompletableFuture` with
> `exceptionally()` for non-blocking error handling.

---

**Q4. What are the four ThreadPoolExecutor rejection policies and when would you use each?**

> Rejection occurs when the queue is full and the pool is at maximum size:
>
> - **AbortPolicy** (default) — throws `RejectedExecutionException`. Use when
    >   callers must know that a task was rejected and can handle it explicitly.
>
> - **CallerRunsPolicy** — the thread that called `submit()` runs the task
    >   itself. This naturally slows down task submission, creating backpressure.
    >   Use when you want to slow producers rather than drop tasks.
>
> - **DiscardPolicy** — silently drops the task. Use only for truly optional
    >   work like metrics collection or logging where occasional loss is acceptable.
>
> - **DiscardOldestPolicy** — removes the oldest queued task and retries
    >   the new submission. Use when newer tasks are more valuable than older ones
    >   — real-time price feeds, sensor data, UI refresh tasks.
>
> In most production systems `CallerRunsPolicy` is the safest default —
> it provides backpressure without dropping work or crashing callers.

---

**Q5. What is the difference between `scheduleAtFixedRate` and `scheduleWithFixedDelay`?**

> Both run a task repeatedly but measure the interval differently:
>
> `scheduleAtFixedRate(task, initialDelay, period)` — period is measured
> from the **start** of the previous execution. If the task takes longer
> than the period, the next run starts immediately after completion — no
> overlap, but runs pile up:
>
> ```
> Task takes 150ms, period = 100ms:
> Run 1: start 0ms,   end 150ms
> Run 2: start 150ms  (immediately — was due at 100ms)
> Run 3: start 300ms  (immediately — was due at 200ms)
> ```
>
> `scheduleWithFixedDelay(task, initialDelay, delay)` — delay is measured
> from the **end** of the previous execution. Always guarantees a gap:
>
> ```
> Task takes 150ms, delay = 100ms:
> Run 1: start 0ms,   end 150ms
> Run 2: start 250ms  (150ms end + 100ms delay)
> Run 3: start 500ms  (250ms + 150ms task + 100ms delay)
> ```
>
> Use `scheduleAtFixedRate` for: heartbeats, polling, metrics — where
> timing regularity matters more than gap between runs.
> Use `scheduleWithFixedDelay` for: cleanup jobs, retry loops — where
> you want breathing room between runs and overlap must be prevented.

---

**Q6. What is work stealing in ForkJoinPool? Why is it efficient?**

> Each worker thread in a `ForkJoinPool` maintains its own double-ended
> queue (deque) of tasks. When a worker forks a subtask, it pushes it
> to its own deque. When a worker runs out of tasks, instead of blocking
> it steals tasks from the **tail** of another busy worker's deque:
>
> ```
> Worker 1 deque: [task-A, task-B, task-C]  ← push/pop from head
> Worker 2 deque: []  → steals task-C from tail of Worker 1
> ```
>
> Why it's efficient:
> - No central task queue — no contention between workers
> - Workers steal from tail while owners pop from head — minimal collision
> - CPU utilisation stays high — idle workers never block, they steal
> - Natural load balancing — busy workers get stolen from, idle workers steal
>
> Best suited for recursive divide-and-conquer: merge sort, parallel array
> operations, tree traversal — tasks that fork into independent subtasks.

---

**Q7. Why should you never block inside a ForkJoinPool task?**

> `ForkJoinPool` is sized to the number of CPU cores (default:
> `Runtime.availableProcessors()`). All threads are expected to be
> CPU-active — dividing and computing, not waiting.
>
> If a task blocks on I/O, a lock, or `Thread.sleep()`, it occupies
> a pool thread without doing CPU work. With enough blocking tasks,
> all threads are occupied and no progress is made — the pool starves:
>
> ```
> // ❌ Blocks a ForkJoin thread — starves the pool
> ForkJoinPool.commonPool().submit(() -> {
>     String data = httpClient.get(url); // blocks for 500ms
>     process(data);
> });
> ```
>
> The common pool is shared by parallel streams and `CompletableFuture`
> — blocking in it starves those too.
>
> For blocking work: use a separate `ExecutorService` with a thread pool
> sized for I/O (not CPU count). For `CompletableFuture`, always pass
> a custom executor to `supplyAsync(task, myIoPool)` when the task does I/O.

---

**Q8. How do you properly shut down an ExecutorService?**

> A two-step shutdown is the standard pattern:
>
> ```
> pool.shutdown(); // stop accepting new tasks, finish queued tasks
> try {
>     if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
>         pool.shutdownNow(); // interrupt running tasks
>         if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
>             log.error("Pool did not terminate");
>         }
>     }
> } catch (InterruptedException e) {
>     pool.shutdownNow();
>     Thread.currentThread().interrupt();
> }
> ```
>
> `shutdown()` — graceful. Lets queued tasks complete, rejects new submissions.
> `shutdownNow()` — forceful. Interrupts running tasks, returns queued tasks
> as a list (which you can log or retry).
>
> Why it matters: non-daemon threads in a pool keep the JVM alive. Forgetting
> to shut down is a common cause of JVM hanging at exit in production. In
> Spring Boot, `ThreadPoolTaskExecutor` handles this via `destroy()` in the
> application context lifecycle — one reason to prefer it over raw pools.