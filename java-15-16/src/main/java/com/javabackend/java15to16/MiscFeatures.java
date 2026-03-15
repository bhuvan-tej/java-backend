package com.javabackend.java15to16;

import java.util.*;
import java.util.stream.*;

/**
 *
 * Java 15-16 — Misc Features
 *
 * Java 15 (September 2020) — non-LTS
 * Java 16 (March 2021)     — non-LTS
 *
 * FEATURES COVERED
 *  1. Stream.toList()           — unmodifiable list, shorthand (Java 16)
 *  2. String.formatted()        — instance format method (Java 15)
 *  3. Pattern matching stable   — instanceof stable in Java 16
 *  4. Records + pattern matching — combining both features
 *
 */
public class MiscFeatures {

    static void main(String[] args) {
        System.out.println("━━━ EXAMPLE 1 — Stream.toList() ━━━\n");
        streamToList();

        System.out.println("\n━━━ EXAMPLE 2 — String.formatted() ━━━\n");
        stringFormatted();

        System.out.println("\n━━━ EXAMPLE 3 — Pattern Matching Stable ━━━\n");
        patternMatchingStable();

        System.out.println("\n━━━ EXAMPLE 4 — Records + Pattern Matching ━━━\n");
        recordsAndPatternMatching();
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 1 — Stream.toList()
    // ─────────────────────────────────────────────
    static void streamToList() {
        var numbers = List.of(5, 3, 8, 1, 9, 2, 7, 4, 6);

        // ── Before Java 16 — collect(Collectors.toList()) ──
        List<Integer> mutableList = numbers.stream()
                .filter(n -> n > 4)
                .sorted()
                .collect(Collectors.toList()); // mutable list
        mutableList.add(99); // allowed — mutable
        System.out.println("  collect(toList) : " + mutableList);

        // ── Java 16 — Stream.toList() ──
        // Shorter, returns UNMODIFIABLE list
        List<Integer> immutableList = numbers.stream()
                .filter(n -> n > 4)
                .sorted()
                .toList(); // unmodifiable
        System.out.println("  stream.toList() : " + immutableList);

        try {
            immutableList.add(99); // throws
        } catch (UnsupportedOperationException e) {
            System.out.println("  toList() is unmodifiable ✓");
        }

        // ── Key difference — toList() vs collect(toList()) ──
        // toList()                     → always unmodifiable
        // collect(Collectors.toList()) → mutable (implementation detail — usually ArrayList)
        // collect(toUnmodifiableList())→ unmodifiable (Java 10+)

        // ── Null behavior difference ──
        // toList()                     → allows nulls in the stream
        // collect(toUnmodifiableList())→ throws NullPointerException on nulls
        List<String> withNull = Arrays.asList("a", null, "b");
        List<String> result = withNull.stream().toList(); // nulls preserved
        System.out.println("  toList with null: " + result);

        // ── Practical use — when you need read-only result ──
        List<String> names = List.of("Alice", "Bob", "Carol", "Dave");
        List<String> filtered = names.stream()
                .filter(n -> n.length() > 3)
                .map(String::toUpperCase)
                .toList(); // clean, no Collectors import needed
        System.out.println("  filtered names  : " + filtered);
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 2 — String.formatted() (Java 15)
    // ─────────────────────────────────────────────
    static void stringFormatted() {
        // ── String.format() — static, classic ──
        String old = String.format("Hello %s, you are %d years old", "Alice", 30);
        System.out.println("  String.format   : " + old);

        // ── String.formatted() — instance method, more fluent ──
        String modern = "Hello %s, you are %d years old".formatted("Alice", 30);
        System.out.println("  .formatted()    : " + modern);

        // ── Useful with text blocks ──
        String template = """
                Dear %s,
                Your order #%d has been confirmed.
                Total: $%.2f
                """;
        String email = template.formatted("Alice", 12345, 99.99);
        System.out.println("  text block formatted:\n" + email);

        // ── Chaining ──
        String result = "  %s scored %d%%  "
                .formatted("Alice", 95)
                .strip();
        System.out.println("  chained         : " + result);

        // Same format specifiers as String.format — %s %d %f %n %% etc.
        // formatted() is just a convenience — equivalent output
        System.out.println("  formatted() == String.format() in output ✓");
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 3 — Pattern Matching instanceof stable (Java 16)
    // ─────────────────────────────────────────────
    static void patternMatchingStable() {
        // Preview in Java 14, 15 — stable in Java 16
        // Covered in java-12-14 as preview — showing stable usage here

        Object[] objects = {"hello", 42, 3.14, List.of(1, 2, 3), null};

        for (Object obj : objects) {
            String desc = classifyObject(obj);
            System.out.println("  " + desc);
        }

        // ── Negation pattern ──
        Object val = "hello";
        if (!(val instanceof String s)) {
            System.out.println("  not a string");
            return; // s not in scope here
        }
        // s IS in scope here — compiler knows val is String after the guard
        System.out.println("  negation pattern: '" + s.toUpperCase() + "'");

        // ── In equals() — idiomatic Java 16 style ──
        System.out.println("  equals() pattern: obj instanceof MyClass c && field.equals(c.field)");
    }

    static String classifyObject(Object obj) {
        if (obj instanceof String s)       return "String of length " + s.length();
        if (obj instanceof Integer i)      return "Integer: " + i;
        if (obj instanceof Double d)       return "Double: " + d;
        if (obj instanceof List<?> list)   return "List of size " + list.size();
        if (obj == null)                   return "null value";
        return "unknown: " + obj.getClass().getSimpleName();
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 4 — Records + Pattern Matching
    // Two Java 16 features working together
    // ─────────────────────────────────────────────

    sealed interface Event permits UserCreated, OrderPlaced, PaymentReceived {}
    record UserCreated(String userId, String name)          implements Event {}
    record OrderPlaced(String orderId, String userId, double amount) implements Event {}
    record PaymentReceived(String paymentId, double amount) implements Event {}

    static void recordsAndPatternMatching() {
        var events = List.of(
                new UserCreated("U1", "Alice"),
                new OrderPlaced("O1", "U1", 150.00),
                new PaymentReceived("P1", 150.00),
                new UserCreated("U2", "Bob"),
                new OrderPlaced("O2", "U2", 75.50)
        );

        // ── Pattern match on sealed records ──
        // Compiler knows all Event subtypes — no default needed
        for (Event event : events) {
            String log = logEvent(event);
            System.out.println("  " + log);
        }

        // ── Aggregate by type ──
        long users    = events.stream().filter(e -> e instanceof UserCreated).count();
        long orders   = events.stream().filter(e -> e instanceof OrderPlaced).count();
        double revenue = events.stream()
                .filter(e -> e instanceof PaymentReceived)
                .mapToDouble(e -> ((PaymentReceived) e).amount())
                .sum();

        System.out.println("  users created   : " + users);
        System.out.println("  orders placed   : " + orders);
        System.out.printf("  total revenue   : %.2f%n", revenue);

        // Records + Sealed + Pattern Matching = expressive domain modeling
        // - Records: immutable data
        // - Sealed:  controlled hierarchy
        // - Pattern: exhaustive dispatch
        System.out.println("  records + sealed + pattern = algebraic data types ✓");
    }

    static String logEvent(Event event) {
        if (event instanceof UserCreated u)
            return "[USER]    %s created — name: %s".formatted(u.userId(), u.name());
        if (event instanceof OrderPlaced o)
            return "[ORDER]   %s by user %s — $%.2f".formatted(o.orderId(), o.userId(), o.amount());
        if (event instanceof PaymentReceived p)
            return "[PAYMENT] %s received — $%.2f".formatted(p.paymentId(), p.amount());
        throw new AssertionError("unreachable — sealed hierarchy");
    }

}