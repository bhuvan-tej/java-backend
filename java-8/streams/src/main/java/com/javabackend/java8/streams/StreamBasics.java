package com.javabackend.java8.streams;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 *
 * Stream Basics
 *
 * Covers:
 *   - What a stream is (not a data structure)
 *   - All stream sources
 *   - Lazy evaluation proof
 *   - Single-use rule
 *   - Stream vs Collection
 *
 */
public class StreamBasics {

    public static void main(String[] args) {
        System.out.println("━━━ Sources ━━━\n");
        sources();

        System.out.println("\n━━━ Lazy Evaluation ━━━\n");
        lazyEvaluation();

        System.out.println("\n━━━ Single-Use Rule ━━━\n");
        singleUse();

        System.out.println("\n━━━ Stream vs Collection ━━━\n");
        streamVsCollection();
    }

    static void sources() {
        // Collection
        List<String> list = Arrays.asList("a", "b", "c");
        System.out.print("From collection  : ");
        list.stream().forEach(s -> System.out.print(s + " "));
        System.out.println();

        // Array
        String[] arr = {"x", "y", "z"};
        System.out.print("From array       : ");
        Arrays.stream(arr).forEach(s -> System.out.print(s + " "));
        System.out.println();

        // Stream.of
        System.out.print("Stream.of        : ");
        Stream.of("p", "q", "r").forEach(s -> System.out.print(s + " "));
        System.out.println();

        // Stream.empty
        System.out.println("Stream.empty size: " + Stream.empty().count());

        // Stream.generate — infinite, must limit
        System.out.print("generate (ha×3)  : ");
        Stream.generate(() -> "ha").limit(3).forEach(s -> System.out.print(s + " "));
        System.out.println();

        // Stream.iterate — infinite sequence
        System.out.print("iterate ×2       : ");
        Stream.iterate(1, n -> n * 2).limit(8)
                .forEach(n -> System.out.print(n + " "));
        System.out.println();

        // IntStream ranges — no boxing
        System.out.print("range(1,5)       : ");
        IntStream.range(1, 5).forEach(n -> System.out.print(n + " "));      // 1 2 3 4
        System.out.println();

        System.out.print("rangeClosed(1,5) : ");
        IntStream.rangeClosed(1, 5).forEach(n -> System.out.print(n + " ")); // 1 2 3 4 5
        System.out.println();

        // Stream.concat
        Stream<String> s1 = Stream.of("A", "B");
        Stream<String> s2 = Stream.of("C", "D");
        System.out.print("Stream.concat    : ");
        Stream.concat(s1, s2).forEach(s -> System.out.print(s + " "));
        System.out.println();

        // String chars as IntStream
        System.out.print("\"hello\" chars    : ");
        "hello".chars().forEach(c -> System.out.print((char)c + " "));
        System.out.println();
    }

    static void lazyEvaluation() {
        List<String> names = Arrays.asList("Alice", "Bob", "Charlie", "Diana");

        // Nothing prints until terminal op
        Stream<String> pipeline = names.stream()
                .filter(s -> {
                    System.out.println("  filter  : " + s);
                    return s.length() > 3;
                })
                .map(s -> {
                    System.out.println("  map     : " + s);
                    return s.toUpperCase();
                });

        System.out.println("Pipeline built — nothing ran yet");
        System.out.println("Calling findFirst()...");

        // findFirst is short-circuit — stops after first match
        // Watch: filter+map run per element only until first match found
        Optional<String> first = pipeline.findFirst();
        System.out.println("Result: " + first.orElse("none"));

        // Vertical (depth-first) processing — filter(Alice) → map(Alice)
        // NOT: filter all → then map all
        System.out.println("\nVertical processing demo (filter then map per element):");
        List<String> result = names.stream()
                .filter(s -> {
                    System.out.println("  F:" + s);
                    return s.length() > 3;
                })
                .map(s -> {
                    System.out.println("  M:" + s);
                    return s.toUpperCase();
                })
                .collect(Collectors.toList());
        System.out.println("Result: " + result);
    }

    static void singleUse() {
        List<String> list = Arrays.asList("a", "b", "c");

        Stream<String> stream = list.stream();
        System.out.println("First use  (count): " + stream.count()); // OK

        try {
            stream.count(); // stream already consumed
        } catch (IllegalStateException e) {
            System.out.println("Second use         : IllegalStateException ✓");
        }

        // Fix: get a new stream each time from the collection
        Supplier<Stream<String>> supplier = list::stream;
        System.out.println("Via supplier (1st) : " + supplier.get().count());
        System.out.println("Via supplier (2nd) : " + supplier.get().count());
    }

    static void streamVsCollection() {
        // Collection — stores data, can iterate multiple times, has size
        List<Integer> collection = Arrays.asList(1, 2, 3, 4, 5);
        System.out.println("Collection size    : " + collection.size());
        System.out.println("Collection again   : " + collection.size()); // iterate again fine

        // Stream — does NOT store data, single-use pipeline, no size until terminal
        // Stream is a VIEW over data — lazy pipeline
        System.out.println("\nStream properties:");
        System.out.println("  Does not store data   — reads from source");
        System.out.println("  Lazy                  — nothing runs until terminal op");
        System.out.println("  Single-use            — consumed after terminal op");
        System.out.println("  Can be infinite       — Stream.generate / Stream.iterate");
        System.out.println("  Can be parallel       — .parallel()");

        // Infinite stream — impossible with a Collection
        System.out.print("\nFirst 5 Fibonacci  : ");
        Stream.iterate(new long[]{0, 1}, f -> new long[]{f[1], f[0] + f[1]})
                .limit(10)
                .map(f -> f[0])
                .forEach(n -> System.out.print(n + " "));
        System.out.println();
    }

}
