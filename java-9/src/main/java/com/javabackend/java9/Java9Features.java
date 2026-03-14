package com.javabackend.java9;

import java.util.*;
import java.util.stream.*;

/**
 *
 * Java 9 Features
 *
 * Java 9 (September 2017) — not an LTS release
 * Key theme: convenience APIs + Stream/Optional improvements
 *
 * FEATURES COVERED
 *  1. Collection factory methods  — List.of, Set.of, Map.of
 *  2. Stream additions            — takeWhile, dropWhile, iterate, ofNullable
 *  3. Optional additions          — ifPresentOrElse, or, stream
 *  4. Interface private methods   — private helper methods in interfaces
 *  5. Process API improvements    — ProcessHandle
 *
 */
public class Java9Features {

    public static void main(String[] args) {
        System.out.println("━━━ EXAMPLE 1 — Collection Factory Methods ━━━\n");
        collectionFactoryMethods();

        System.out.println("\n━━━ EXAMPLE 2 — Stream Additions ━━━\n");
        streamAdditions();

        System.out.println("\n━━━ EXAMPLE 3 — Optional Additions ━━━\n");
        optionalAdditions();

        System.out.println("\n━━━ EXAMPLE 4 — Interface Private Methods ━━━\n");
        interfacePrivateMethods();

        System.out.println("\n━━━ EXAMPLE 5 — Process API ━━━\n");
        processApi();
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 1 — Collection Factory Methods
    // ─────────────────────────────────────────────
    static void collectionFactoryMethods() {

        // ── List.of — immutable list ──
        List<String> names = List.of("Alice", "Bob", "Carol");
        System.out.println("  List.of        : " + names);

        // ── Set.of — immutable set, no duplicates allowed ──
        Set<String> roles = Set.of("ADMIN", "USER", "VIEWER");
        System.out.println("  Set.of         : " + roles);

        // ── Map.of — immutable map, up to 10 entries ──
        Map<String, Integer> scores = Map.of(
                "Alice", 95,
                "Bob",   87,
                "Carol", 92
        );
        System.out.println("  Map.of         : " + scores);

        // ── Map.ofEntries — for more than 10 entries ──
        Map<String, Integer> more = Map.ofEntries(
                Map.entry("Alice", 95),
                Map.entry("Bob",   87),
                Map.entry("Carol", 92),
                Map.entry("Dave",  78)
        );
        System.out.println("  Map.ofEntries  : " + more);

        // ── Map.copyOf, List.copyOf, Set.copyOf ──
        List<String> mutable = new ArrayList<>(List.of("x", "y", "z"));
        List<String> immutable = List.copyOf(mutable);
        System.out.println("  List.copyOf    : " + immutable);

        // ── Key properties of factory collections ──
        // 1. Immutable — add/remove/set throws UnsupportedOperationException
        // 2. No nulls  — null elements throw NullPointerException
        // 3. Set/Map   — order not guaranteed, duplicates not allowed
        // 4. Serializable

        try {
            names.add("Dave"); // throws
        } catch (UnsupportedOperationException e) {
            System.out.println("  immutable add  : UnsupportedOperationException ✓");
        }

        try {
            List.of("a", null); // throws
        } catch (NullPointerException e) {
            System.out.println("  null in List.of: NullPointerException ✓");
        }

        // ── Before Java 9 — verbose ──
        // Arrays.asList — fixed size but mutable (set allowed, add not)
        // Collections.unmodifiableList(Arrays.asList(...)) — two lines
        // List.of is the clean replacement
        System.out.println("  Arrays.asList  : " + Arrays.asList("a", "b")); // for comparison
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 2 — Stream Additions
    // ─────────────────────────────────────────────
    static void streamAdditions() {

        // ── takeWhile — take elements while predicate is true, stop at first false ──
        // Only meaningful on ordered streams — predicate is evaluated in order
        List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        List<Integer> taken = numbers.stream()
                .takeWhile(n -> n < 5)   // takes 1,2,3,4 — stops when 5 fails
                .collect(Collectors.toList());
        System.out.println("  takeWhile < 5  : " + taken);

        // ── dropWhile — skip elements while predicate is true, keep rest ──
        List<Integer> dropped = numbers.stream()
                .dropWhile(n -> n < 5)   // drops 1,2,3,4 — keeps from 5 onwards
                .collect(Collectors.toList());
        System.out.println("  dropWhile < 5  : " + dropped);

        // ── takeWhile vs filter ──
        // filter(n -> n < 5) would give [1,2,3,4] from any order
        // takeWhile(n -> n < 5) stops at first element that fails — order matters
        List<Integer> unordered = List.of(1, 3, 5, 2, 4); // 5 fails early
        List<Integer> takenUnordered = unordered.stream()
                .takeWhile(n -> n < 5)
                .collect(Collectors.toList());
        System.out.println("  takeWhile unord: " + takenUnordered); // [1,3] — stops at 5

        // ── Stream.iterate with predicate (3-arg version) ──
        // Java 8: Stream.iterate(seed, fn) — infinite, needs limit()
        // Java 9: Stream.iterate(seed, predicate, fn) — terminates naturally
        List<Integer> powers = Stream.iterate(1, n -> n <= 100, n -> n * 2)
                .collect(Collectors.toList());
        System.out.println("  iterate(1,≤100,×2): " + powers); // [1,2,4,8,16,32,64]

        // ── Stream.ofNullable — avoid null check before streaming ──
        // Java 8: value == null ? Stream.empty() : Stream.of(value)
        // Java 9: Stream.ofNullable(value)
        String value = null;
        long count = Stream.ofNullable(value).count();
        System.out.println("  ofNullable(null): count=" + count); // 0

        String name = "Alice";
        List<String> fromNullable = Stream.ofNullable(name)
                .map(String::toUpperCase)
                .collect(Collectors.toList());
        System.out.println("  ofNullable(val) : " + fromNullable); // [ALICE]

        // ── Practical: flatMap with ofNullable to skip nulls ──
        List<String> withNulls = Arrays.asList("Alice", null, "Bob", null, "Carol");
        List<String> noNulls = withNulls.stream()
                .flatMap(Stream::ofNullable) // null → empty stream, filtered out
                .collect(Collectors.toList());
        System.out.println("  flatMap ofNull  : " + noNulls); // [Alice, Bob, Carol]
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 3 — Optional Additions
    // ─────────────────────────────────────────────
    static void optionalAdditions() {

        // ── ifPresentOrElse — act on value OR run empty action ──
        // Java 8: ifPresent only — no else branch
        Optional<String> present = Optional.of("Alice");
        Optional<String> empty   = Optional.empty();

        present.ifPresentOrElse(
                v -> System.out.println("  ifPresentOrElse: found " + v),
                ()  -> System.out.println("  ifPresentOrElse: not found")
        );
        empty.ifPresentOrElse(
                v -> System.out.println("  ifPresentOrElse: found " + v),
                ()  -> System.out.println("  ifPresentOrElse: not found")
        );

        // ── or — supply alternative Optional if empty ──
        // Java 8: orElse returns T, orElseGet returns T
        // Java 9: or returns Optional<T> — keeps Optional chain alive
        Optional<String> result = empty
                .or(() -> Optional.of("fallback from or()"));
        System.out.println("  or()           : " + result.get());

        // Chaining: try primary → secondary → tertiary
        Optional<String> user = findInCache("U1")
                .or(() -> findInDb("U1"))
                .or(() -> Optional.of("guest"));
        System.out.println("  or() chain     : " + user.get());

        // ── Optional.stream — bridge Optional into Stream pipeline ──
        // Java 8: ugly — optional.map(Stream::of).orElseGet(Stream::empty)
        // Java 9: optional.stream() — 0 or 1 element stream
        List<Optional<String>> optionals = List.of(
                Optional.of("Alice"),
                Optional.empty(),
                Optional.of("Bob"),
                Optional.empty(),
                Optional.of("Carol")
        );

        // Flatten optionals — filter empties, extract values
        List<String> values = optionals.stream()
                .flatMap(Optional::stream) // empty → 0 elements, present → 1 element
                .collect(Collectors.toList());
        System.out.println("  Optional.stream: " + values); // [Alice, Bob, Carol]
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 4 — Interface Private Methods
    // ─────────────────────────────────────────────
    static void interfacePrivateMethods() {
        // Java 8 added default and static methods to interfaces
        // Problem: default methods sharing logic had to duplicate code
        //          or expose helper as a default method (polluting API)
        // Java 9: private methods in interfaces — shared helper, not exposed

        Validator emailValidator = Validator.ofEmail();
        Validator phoneValidator = Validator.ofPhone();

        System.out.println("  email valid    : " + emailValidator.validate("user@example.com"));
        System.out.println("  email invalid  : " + emailValidator.validate("not-an-email"));
        System.out.println("  phone valid    : " + phoneValidator.validate("1234567890"));
        System.out.println("  phone invalid  : " + phoneValidator.validate("abc"));
    }

    interface Validator {
        boolean validate(String input);

        // Private method — shared logic, not part of public API
        private boolean isNotEmpty(String input) {
            return input != null && !input.isBlank();
        }

        // Private static method — shared static logic
        private static String sanitise(String input) {
            return input == null ? "" : input.trim();
        }

        static Validator ofEmail() {
            return input -> {
                String s = sanitise(input);
                // uses private instance method via lambda — not directly, shown below
                return s.contains("@") && s.contains(".");
            };
        }

        static Validator ofPhone() {
            return input -> {
                String s = sanitise(input);
                return s.matches("\\d{10}");
            };
        }

        // Default method using private helper
        default boolean validateWithLog(String input) {
            if (!isNotEmpty(input)) {
                System.out.println("  [log] empty input rejected");
                return false;
            }
            return validate(input);
        }
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 5 — Process API
    // ─────────────────────────────────────────────
    static void processApi() {
        // Java 9 greatly improved Process API for managing OS processes

        // ── Current process info ──
        ProcessHandle current = ProcessHandle.current();
        System.out.println("  current PID    : " + current.pid());
        System.out.println("  is alive       : " + current.isAlive());

        // ProcessHandle.Info — metadata about the process
        current.info().command().ifPresent(cmd ->
                System.out.println("  command        : " + cmd));
        current.info().startInstant().ifPresent(start ->
                System.out.println("  started at     : " + start));
        current.info().totalCpuDuration().ifPresent(cpu ->
                System.out.println("  cpu time       : " + cpu.toMillis() + "ms"));

        // ── List all processes (like ps aux) ──
        long javaProcessCount = ProcessHandle.allProcesses()
                .filter(p -> p.info().command()
                        .map(cmd -> cmd.toLowerCase().contains("java"))
                        .orElse(false))
                .count();
        System.out.println("  java processes : " + javaProcessCount);

        // ── onExit — non-blocking process completion callback ──
        // ProcessHandle.of(pid).ifPresent(p ->
        //     p.onExit().thenAccept(ph -> log("process " + ph.pid() + " exited")));
        System.out.println("  onExit()       : CompletableFuture<ProcessHandle> — async exit callback");

        // ── Parent process ──
        current.parent().ifPresent(parent ->
                System.out.println("  parent PID     : " + parent.pid()));
    }

    // helpers
    static Optional<String> findInCache(String id) { return Optional.empty(); }
    static Optional<String> findInDb(String id)    { return Optional.empty(); }

}