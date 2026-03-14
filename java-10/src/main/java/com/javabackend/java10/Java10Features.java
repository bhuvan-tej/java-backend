package com.javabackend.java10;

import java.util.*;
import java.util.stream.*;

/**
 *
 * TOPIC : Java 10 Features
 *
 * Java 10 (March 2018) — not an LTS release
 * Key theme: local variable type inference (var)
 *
 * FEATURES COVERED
 *  1. var — local variable type inference
 *  2. Optional.orElseThrow() — no-arg version
 *  3. Unmodifiable collectors — toUnmodifiableList/Set/Map
 *  4. List/Set/Map.copyOf — defensive copies
 *
 */
public class Java10Features {

    public static void main(String[] args) {
        System.out.println("━━━ EXAMPLE 1 — var basics ━━━\n");
        varBasics();

        System.out.println("\n━━━ EXAMPLE 2 — var in loops and lambdas ━━━\n");
        varLoopsAndLambdas();

        System.out.println("\n━━━ EXAMPLE 3 — var rules and limits ━━━\n");
        varRulesAndLimits();

        System.out.println("\n━━━ EXAMPLE 4 — Optional.orElseThrow ━━━\n");
        optionalOrElseThrow();

        System.out.println("\n━━━ EXAMPLE 5 — Unmodifiable collectors and copyOf ━━━\n");
        unmodifiableCollections();
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 1 — var basics
    // ─────────────────────────────────────────────
    static void varBasics() {
        // var — local variable type inference
        // The TYPE is inferred by the compiler from the right-hand side
        // Type is still STATIC — var is not dynamic like JavaScript

        // Before Java 10
        String name1 = "Alice";
        List<String> names1 = new ArrayList<>();
        Map<String, Integer> scores1 = new HashMap<>();

        // Java 10 — same types, less repetition
        var name   = "Alice";                  // inferred: String
        var names  = new ArrayList<String>();  // inferred: ArrayList<String>
        var scores = new HashMap<String, Integer>(); // inferred: HashMap<String, Integer>

        names.add("Alice");
        names.add("Bob");
        scores.put("Alice", 95);
        scores.put("Bob", 87);

        System.out.println("  name   : " + name   + " (" + ((Object)name).getClass().getSimpleName() + ")");
        System.out.println("  names  : " + names  + " (" + names.getClass().getSimpleName() + ")");
        System.out.println("  scores : " + scores + " (" + scores.getClass().getSimpleName() + ")");

        // var with complex generic types — biggest readability win
        // Before:
        Map<String, List<Integer>> groupedBefore = new HashMap<String, List<Integer>>();
        // After:
        var grouped = new HashMap<String, List<Integer>>();
        grouped.put("evens", List.of(2, 4, 6));
        grouped.put("odds",  List.of(1, 3, 5));
        System.out.println("  grouped: " + grouped);

        // var infers the most specific type from the RHS
        var list = new ArrayList<String>(); // ArrayList<String>, not List<String>
        System.out.println("  var infers: " + list.getClass().getSimpleName()); // ArrayList
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 2 — var in loops and lambdas
    // ─────────────────────────────────────────────
    static void varLoopsAndLambdas() {

        var numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        // ── var in enhanced for loop ──
        int sum = 0;
        for (var n : numbers) { // n inferred as Integer
            sum += n;
        }
        System.out.println("  sum: " + sum);

        // ── var in traditional for loop ──
        for (var i = 0; i < 3; i++) { // i inferred as int
            System.out.println("  loop i=" + i);
        }

        // ── var in try-with-resources ──
        // var scanner = new java.util.Scanner(System.in); // inferred: Scanner
        System.out.println("  var works in try-with-resources too");

        // ── var in lambda parameters (Java 11 feature — shown here for context) ──
        // In Java 10, var cannot be used in lambda parameters
        // In Java 11, (@NonNull var s) allows annotations on lambda params
        // numbers.stream().map((var n) -> n * 2); // Java 11+

        // ── var with streams ──
        var evenSquares = numbers.stream()
                .filter(n -> n % 2 == 0)
                .map(n -> n * n)
                .collect(Collectors.toList()); // inferred: List<Integer>
        System.out.println("  evenSquares: " + evenSquares);

        // ── var with Map.Entry ── biggest readability improvement
        var cityPopulation = new HashMap<String, Long>();
        cityPopulation.put("Mumbai",  20_667_656L);
        cityPopulation.put("Delhi",   32_941_000L);
        cityPopulation.put("Chennai",  7_088_000L);

        // Before: Map.Entry<String, Long> entry — verbose
        for (var entry : cityPopulation.entrySet()) {
            System.out.println("  " + entry.getKey() + " → " + entry.getValue());
        }
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 3 — var rules and limits
    // ─────────────────────────────────────────────
    static void varRulesAndLimits() {

        // ── WHERE var CAN be used ──
        var s = "local variable";           // ✅ local variable with initializer
        var list = new ArrayList<String>(); // ✅ local variable
        for (var i = 0; i < 1; i++) {}      // ✅ for loop index
        for (var item : List.of(1,2,3)) {}  // ✅ enhanced for loop
        // try (var r = new Scanner(...)) {} // ✅ try-with-resources

        // ── WHERE var CANNOT be used ──

        // ❌ Fields — var is only for local variables
        // var count = 0; // not allowed as instance/static field

        // ❌ Method parameters
        // void process(var input) {} // not allowed

        // ❌ Return types
        // var getResult() {} // not allowed

        // ❌ Without initializer — compiler has nothing to infer from
        // var x; // not allowed — no RHS to infer from

        // ❌ Initialised with null — null has no type
        // var obj = null; // not allowed — can't infer type from null

        // ❌ Array initializer shorthand
        // var arr = {1, 2, 3}; // not allowed — use new int[]{1,2,3}
        var arr = new int[]{1, 2, 3}; // ✅ this works

        // ── var is still statically typed ──
        var number = 42;        // inferred as int
        // number = "hello";    // compile error — still int, not dynamic

        // ── var infers concrete type, not interface ──
        var arrayList = new ArrayList<String>(); // ArrayList, not List
        arrayList.trimToSize(); // ArrayList-specific method — works because inferred as ArrayList
        // If declared as List<String>, trimToSize() would not compile

        System.out.println("  var is compile-time resolved — fully static typed");
        System.out.println("  var only works for local variables with an initialiser");
        System.out.println("  var infers the most specific type from the RHS");
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 4 — Optional.orElseThrow()
    // ─────────────────────────────────────────────
    static void optionalOrElseThrow() {

        // Java 8: orElseThrow(Supplier<Exception>) — requires supplier
        Optional<String> present = Optional.of("Alice");
        String v1 = present.orElseThrow(() -> new RuntimeException("not found"));
        System.out.println("  orElseThrow(supplier): " + v1);

        // Java 10: orElseThrow() — no-arg, throws NoSuchElementException
        // Semantically cleaner — signals "this must be present"
        String v2 = present.orElseThrow(); // throws NoSuchElementException if empty
        System.out.println("  orElseThrow()        : " + v2);

        // Equivalent to get() but more expressive in intent
        // get() is considered bad practice — use orElseThrow() instead
        // orElseThrow() makes the "must be present" contract explicit

        Optional<String> empty = Optional.empty();
        try {
            empty.orElseThrow(); // NoSuchElementException
        } catch (NoSuchElementException e) {
            System.out.println("  empty.orElseThrow()  : NoSuchElementException ✓");
        }

        System.out.println("  prefer orElseThrow() over get() — same but expressive");
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 5 — Unmodifiable collectors and copyOf
    // ─────────────────────────────────────────────
    static void unmodifiableCollections() {

        // ── Collectors.toUnmodifiableList/Set/Map ──
        // Java 8: collect(Collectors.toList()) — returns mutable list
        // Java 10: collect(Collectors.toUnmodifiableList()) — returns immutable list

        var numbers = List.of(1, 2, 3, 4, 5, 6);

        // Mutable result — Java 8 style
        var mutable = numbers.stream()
                .filter(n -> n % 2 == 0)
                .collect(Collectors.toList()); // mutable
        mutable.add(8); // allowed
        System.out.println("  mutable result  : " + mutable);

        // Immutable result — Java 10
        var immutable = numbers.stream()
                .filter(n -> n % 2 == 0)
                .collect(Collectors.toUnmodifiableList()); // immutable
        System.out.println("  immutable result: " + immutable);
        try {
            immutable.add(8); // throws
        } catch (UnsupportedOperationException e) {
            System.out.println("  immutable add   : UnsupportedOperationException ✓");
        }

        // toUnmodifiableSet and toUnmodifiableMap
        var uniqueNames = List.of("Alice", "Bob", "Alice", "Carol").stream()
                .collect(Collectors.toUnmodifiableSet());
        System.out.println("  unmodifiable set: " + uniqueNames);

        var nameLength = List.of("Alice", "Bob", "Carol").stream()
                .collect(Collectors.toUnmodifiableMap(
                        s -> s,
                        String::length
                ));
        System.out.println("  unmodifiable map: " + nameLength);

        // ── List/Set/Map.copyOf — defensive immutable copy ──
        // Creates a new immutable collection from an existing one
        var source = new ArrayList<>(List.of("a", "b", "c"));
        var copy   = List.copyOf(source); // immutable copy
        source.add("d"); // modifying source does NOT affect copy
        System.out.println("  source after add: " + source);
        System.out.println("  copy unchanged  : " + copy);

        // copyOf vs List.of — copyOf takes existing collection, List.of takes varargs
        // Both produce immutable results
        // copyOf does NOT copy if input is already an unmodifiable List.of result
        var alreadyImmutable = List.of("x", "y");
        var copyOfImmutable   = List.copyOf(alreadyImmutable);
        System.out.println("  same instance   : " + (alreadyImmutable == copyOfImmutable)); // true — optimized
    }

}