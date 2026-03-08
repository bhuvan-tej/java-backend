package com.javabackend.java8.streams;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

/**
 *
 * Parallel Streams
 *
 * Covers:
 *   parallel vs sequential, ForkJoinPool,
 *   ordering (forEachOrdered), thread safety,
 *   when to use / when NOT to use parallel,
 *   custom ForkJoinPool
 *
 */
public class StreamParallel {

    public static void main(String[] args) throws Exception {
        System.out.println("━━━ Parallel Basics ━━━\n");        parallelBasics();
        System.out.println("\n━━━ Ordering ━━━\n");              orderingDemo();
        System.out.println("\n━━━ Thread Safety ━━━\n");         threadSafety();
        System.out.println("\n━━━ When to Use / Avoid ━━━\n");   whenToUse();
        System.out.println("\n━━━ Custom ForkJoinPool ━━━\n");   customPool();
    }

    static void parallelBasics() {
        List<Integer> numbers = IntStream.rangeClosed(1, 1_000_000)
                .boxed().collect(Collectors.toList());

        // Sequential
        long t1 = System.nanoTime();
        long seqSum = numbers.stream().mapToLong(i -> i).sum();
        long seqMs  = (System.nanoTime() - t1) / 1_000_000;

        // Parallel — uses ForkJoinPool.commonPool() by default
        long t2 = System.nanoTime();
        long parSum = numbers.parallelStream().mapToLong(i -> i).sum();
        long parMs  = (System.nanoTime() - t2) / 1_000_000;

        System.out.println("Sequential sum   : " + seqSum + " (" + seqMs + "ms)");
        System.out.println("Parallel   sum   : " + parSum + " (" + parMs + "ms)");
        System.out.println("Results match    : " + (seqSum == parSum));
        System.out.println("CPUs available   : " + Runtime.getRuntime().availableProcessors());

        // .parallel() and .sequential() switch mode mid-pipeline
        // The LAST call wins for the whole pipeline
        long sum = numbers.stream()
                .parallel()     // parallel
                .filter(n -> n % 2 == 0)
                .sequential()   // switches back to sequential
                .mapToLong(i -> i)
                .sum();
        System.out.println("last wins (seq)  : " + sum);
    }

    static void orderingDemo() {
        List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8);

        // forEach — unordered in parallel (may print in any order)
        System.out.print("parallel forEach : ");
        nums.parallelStream().forEach(n -> System.out.print(n + " "));
        System.out.println();

        // forEachOrdered — maintains encounter order even in parallel
        System.out.print("forEachOrdered   : ");
        nums.parallelStream().forEachOrdered(n -> System.out.print(n + " "));
        System.out.println();

        // collect — result is always correctly ordered for ordered sources
        List<Integer> result = nums.parallelStream()
                .filter(n -> n % 2 == 0)
                .collect(Collectors.toList());
        System.out.println("parallel collect : " + result); // [2,4,6,8] — ordered
    }

    static void threadSafety() {
        List<Integer> nums = IntStream.rangeClosed(1, 1000)
                .boxed().collect(Collectors.toList());

        // ❌ WRONG — ArrayList is not thread-safe, parallel add corrupts it
        List<Integer> unsafe = new ArrayList<>();
        nums.parallelStream().forEach(unsafe::add); // race condition!
        System.out.println("unsafe add size  : " + unsafe.size() + " (expected 1000, may differ!)");

        // ✅ CORRECT — collect() is always thread-safe
        List<Integer> safe = nums.parallelStream()
                .collect(Collectors.toList());
        System.out.println("collect size     : " + safe.size());

        // ✅ CORRECT — use ConcurrentLinkedQueue for concurrent accumulation
        Queue<Integer> concurrent = new ConcurrentLinkedQueue<>();
        nums.parallelStream().forEach(concurrent::add);
        System.out.println("concurrent size  : " + concurrent.size());
    }

    static void whenToUse() {
        System.out.println("USE parallel when:");
        System.out.println(" ✅ Data set is LARGE (100k+ elements)");
        System.out.println(" ✅ Operations are CPU-intensive (no I/O)");
        System.out.println(" ✅ No shared mutable state");
        System.out.println(" ✅ Source is easily splittable (ArrayList, array)");
        System.out.println(" ✅ Operations are stateless and independent");

        System.out.println("AVOID parallel when:");
        System.out.println(" ❌ Small data sets — threading overhead > benefit");
        System.out.println(" ❌ I/O-bound operations — threads block, no CPU gain");
        System.out.println(" ❌ Shared mutable state — race conditions");
        System.out.println(" ❌ LinkedList source — poor splitter, bad parallelism");
        System.out.println(" ❌ Operations with side effects (DB writes, logging)");
        System.out.println(" ❌ Order-sensitive operations where ordering overhead dominates");

        // Small list — parallel is SLOWER due to thread overhead
        List<Integer> small = Arrays.asList(1,2,3,4,5);
        long t1 = System.nanoTime();
        small.stream().mapToInt(i->i).sum();
        long seqNs = System.nanoTime() - t1;

        long t2 = System.nanoTime();
        small.parallelStream().mapToInt(i->i).sum();
        long parNs = System.nanoTime() - t2;

        System.out.println("Small list seq   : " + seqNs + "ns");
        System.out.println("Small list par   : " + parNs + "ns (likely slower)");
    }

    static void customPool() throws Exception {
        // By default, parallel streams use ForkJoinPool.commonPool()
        // which is shared with other parallel tasks in the JVM.
        // In a web server, this can starve other requests.

        // Use a dedicated ForkJoinPool for isolation
        ForkJoinPool customPool = new ForkJoinPool(4); // 4 threads

        List<Integer> nums = IntStream.rangeClosed(1, 1_000_000)
                .boxed().collect(Collectors.toList());

        long sum = customPool.submit(() ->
                nums.parallelStream().mapToLong(i -> i).sum()
        ).get();

        System.out.println("Custom pool sum  : " + sum);
        System.out.println("Custom pool size : " + customPool.getParallelism());
        customPool.shutdown();
    }

}