package com.javabackend.java8.streams;

import java.util.*;
import java.util.stream.*;

/**
 * Numeric Streams
 *
 * Covers:
 *   IntStream, LongStream, DoubleStream,
 *   range / rangeClosed, summaryStatistics,
 *   boxing cost, mapToObj, asLongStream,
 *   sum / avg / min / max without boxing
 *
 */
public class StreamNumeric {

    public static void main(String[] args) {
        System.out.println("━━━ IntStream ━━━\n");              intStreamDemo();
        System.out.println("\n━━━ LongStream / DoubleStream ━━━\n"); otherNumeric();
        System.out.println("\n━━━ summaryStatistics ━━━\n");    summaryStats();
        System.out.println("\n━━━ Boxing Cost ━━━\n");          boxingCost();
        System.out.println("\n━━━ Conversions ━━━\n");          conversions();
    }

    static void intStreamDemo() {
        // range — exclusive end
        System.out.print("range(1,6)       : ");
        IntStream.range(1, 6).forEach(n -> System.out.print(n + " "));
        System.out.println();

        // rangeClosed — inclusive end
        System.out.print("rangeClosed(1,5) : ");
        IntStream.rangeClosed(1, 5).forEach(n -> System.out.print(n + " "));
        System.out.println();

        // sum / avg / min / max — no boxing
        int[] arr = {3, 1, 4, 1, 5, 9, 2, 6};
        System.out.println("sum              : " + IntStream.of(arr).sum());
        System.out.println("avg              : " + IntStream.of(arr).average().getAsDouble());
        System.out.println("min              : " + IntStream.of(arr).min().getAsInt());
        System.out.println("max              : " + IntStream.of(arr).max().getAsInt());

        // Generate even numbers 0..20
        System.out.print("evens 0-20       : ");
        IntStream.iterate(0, n -> n + 2).limit(11)
                .forEach(n -> System.out.print(n + " "));
        System.out.println();

        // Sum of 1..100 (Gauss)
        System.out.println("sum 1..100       : " + IntStream.rangeClosed(1, 100).sum());

    }

    static void otherNumeric() {
        // LongStream — for large numbers
        System.out.print("LongStream 1..5  : ");
        LongStream.rangeClosed(1, 5).forEach(n -> System.out.print(n + " "));
        System.out.println();

        // Large sum without overflow
        long bigSum = LongStream.rangeClosed(1L, 1_000_000L).sum();
        System.out.printf("sum 1..1M        : %,d%n", bigSum);

        // DoubleStream
        System.out.print("DoubleStream     : ");
        DoubleStream.of(1.1, 2.2, 3.3, 4.4)
                .forEach(d -> System.out.printf("%.1f ", d));
        System.out.println();

        System.out.printf("DoubleStream avg : %.2f%n",
                DoubleStream.of(1.1, 2.2, 3.3, 4.4).average().getAsDouble());
    }

    static void summaryStats() {
        // IntSummaryStatistics — count, sum, min, max, avg in one pass
        int[] salaries = {95_000, 85_000, 72_000, 98_000, 68_000, 60_000};

        IntSummaryStatistics stats = IntStream.of(salaries).summaryStatistics();
        System.out.println("count            : " + stats.getCount());
        System.out.printf("sum              : ₹%,d%n", (long)stats.getSum());
        System.out.printf("min              : ₹%,d%n", stats.getMin());
        System.out.printf("max              : ₹%,d%n", stats.getMax());
        System.out.printf("avg              : ₹%,.0f%n", stats.getAverage());

        // DoubleSummaryStatistics
        DoubleSummaryStatistics dStats =
                DoubleStream.of(1.5, 2.5, 3.5, 4.5, 5.5).summaryStatistics();
        System.out.printf("double avg       : %.2f%n", dStats.getAverage());
    }

    static void boxingCost() {
        // Stream<Integer> — boxes each int to Integer object
        // IntStream — works directly with primitive int — zero boxing

        int n = 1_000_000;

        // Boxed — measures overhead in concept (avoid in hot paths)
        long t1 = System.nanoTime();
        long boxedSum = Stream.iterate(1, i -> i + 1)
                .limit(n)
                .mapToInt(Integer::intValue) // unbox back to int
                .sum();
        long t2 = System.nanoTime();

        // Primitive — no boxing at all
        long t3 = System.nanoTime();
        long primitiveSum = IntStream.rangeClosed(1, n).sum();
        long t4 = System.nanoTime();

        System.out.println("boxed sum        : " + boxedSum    + " (" + (t2-t1)/1_000_000 + "ms)");
        System.out.println("primitive sum    : " + primitiveSum + " (" + (t4-t3)/1_000_000 + "ms)");
        System.out.println("→ use IntStream for numeric work, not Stream<Integer>");
    }

    static void conversions() {
        // IntStream → Stream<String> via mapToObj
        System.out.print("mapToObj         : ");
        IntStream.rangeClosed(1, 5)
                .mapToObj(n -> "item" + n)
                .forEach(s -> System.out.print(s + " "));
        System.out.println();

        // IntStream → Stream<Integer> via boxed()
        List<Integer> boxed = IntStream.rangeClosed(1, 5)
                .boxed()
                .collect(Collectors.toList());
        System.out.println("boxed()          : " + boxed);

        // Stream<Integer> → IntStream via mapToInt
        List<Integer> nums = Arrays.asList(3, 1, 4, 1, 5);
        System.out.println("mapToInt sum     : " + nums.stream().mapToInt(i -> i).sum());

        // IntStream → LongStream
        System.out.println("asLongStream sum : " +
                IntStream.rangeClosed(1, 100).asLongStream().sum());

        // IntStream → DoubleStream
        System.out.printf("asDoubleStream   : %.2f%n",
                IntStream.of(1,2,3,4,5).asDoubleStream().average().getAsDouble());
    }

}