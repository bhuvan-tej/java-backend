package com.javabackend.java21;

import java.util.*;
import java.util.stream.*;

/**
 *
 * Java 21 — Misc Features
 *
 * Java 21 (September 2023) — LTS release
 *
 * FEATURES COVERED
 *  1. Sequenced Collections  — SequencedCollection, SequencedMap (stable)
 *  2. String Templates       — preview in Java 21 (covered conceptually)
 *  3. Unnamed Classes        — preview in Java 21 (conceptual)
 *  4. Key API additions      — Math.clamp, Character additions
 *
 */
public class MiscFeatures {

    public static void main(String[] args) {
        System.out.println("━━━ EXAMPLE 1 — Sequenced Collections ━━━\n");
        sequencedCollections();

        System.out.println("\n━━━ EXAMPLE 2 — SequencedMap ━━━\n");
        sequencedMap();

        System.out.println("\n━━━ EXAMPLE 3 — String Templates (Preview) ━━━\n");
        stringTemplates();

        System.out.println("\n━━━ EXAMPLE 4 — Math.clamp and Other Additions ━━━\n");
        otherAdditions();

        System.out.println("\n━━━ EXAMPLE 5 — Java 21 as LTS ━━━\n");
        java21AsLts();
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 1 — Sequenced Collections
    // New interfaces that add first/last element access
    // ─────────────────────────────────────────────
    static void sequencedCollections() {

        // THE PROBLEM before Java 21:
        // Getting the first/last element was inconsistent across collection types:
        //   List     : list.get(0)                   list.get(list.size() - 1)
        //   Deque    : deque.peekFirst()              deque.peekLast()
        //   SortedSet: sortedSet.first()              sortedSet.last()
        //   LinkedHashSet: no direct API!             no direct API!
        // No unified way — had to know the exact collection type

        // ── Java 21 — SequencedCollection interface ──
        // Added to: List, Deque, LinkedHashSet, SortedSet and their implementations
        // Provides: getFirst(), getLast(), addFirst(), addLast(),
        //           removeFirst(), removeLast(), reversed()

        // ── List ──
        List<String> list = new ArrayList<>(List.of("a", "b", "c", "d"));

        System.out.println("  list.getFirst() : " + list.getFirst()); // a
        System.out.println("  list.getLast()  : " + list.getLast());  // d

        list.addFirst("z"); // inserts at front — list is now [z, a, b, c, d]
        list.addLast("y");  // inserts at end   — list is now [z, a, b, c, d, y]
        System.out.println("  after addFirst/Last: " + list);

        list.removeFirst(); // removes z
        list.removeLast();  // removes y
        System.out.println("  after removeFirst/Last: " + list); // [a, b, c, d]

        // ── reversed() — returns a reversed VIEW, not a copy ──
        // Changes to the original are reflected in the reversed view
        List<String> reversed = list.reversed();
        System.out.println("  reversed view   : " + reversed); // [d, c, b, a]
        list.add("e");
        System.out.println("  reversed after add: " + reversed); // [e, d, c, b, a] — live view

        // ── LinkedHashSet — previously had no first/last API ──
        LinkedHashSet<String> lhs = new LinkedHashSet<>(List.of("x", "y", "z"));
        System.out.println("  lhs.getFirst(): " + lhs.getFirst()); // x — insertion order
        System.out.println("  lhs.getLast() : " + lhs.getLast());  // z
        System.out.println("  lhs.reversed(): " + lhs.reversed()); // [z, y, x]

        // ── SortedSet ──
        SortedSet<Integer> sorted = new TreeSet<>(Set.of(3, 1, 4, 5, 9, 2, 6));
        System.out.println("  sorted.getFirst(): " + sorted.getFirst()); // 1 — min
        System.out.println("  sorted.getLast() : " + sorted.getLast());  // 9 — max

        // ── Deque ──
        Deque<String> deque = new ArrayDeque<>(List.of("first", "middle", "last"));
        System.out.println("  deque.getFirst(): " + deque.getFirst());
        System.out.println("  deque.getLast() : " + deque.getLast());
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 2 — SequencedMap
    // Maps with defined encounter order get first/last entry APIs
    // ─────────────────────────────────────────────
    static void sequencedMap() {

        // ── SequencedMap — extends Map with first/last entry access ──
        // Implemented by: LinkedHashMap, TreeMap, and their views

        // ── LinkedHashMap — insertion-ordered ──
        LinkedHashMap<String, Integer> scores = new LinkedHashMap<>();
        scores.put("Alice", 95);
        scores.put("Bob",   87);
        scores.put("Carol", 92);
        scores.put("Dave",  78);

        // New APIs — no more iterator tricks to get first/last
        System.out.println("  firstEntry(): " + scores.firstEntry()); // Alice=95
        System.out.println("  lastEntry() : " + scores.lastEntry());  // Dave=78

        // putFirst / putLast — insert at defined position
        scores.putFirst("Zara", 99);  // Zara goes to front
        scores.putLast("Eve", 88);    // Eve goes to end
        System.out.println("  after putFirst/Last: " + scores);

        // reversed() — reversed view of the map
        SequencedMap<String, Integer> rev = scores.reversed();
        System.out.println("  reversed firstEntry: " + rev.firstEntry()); // was last

        // ── TreeMap — sorted by key ──
        TreeMap<String, Integer> treeMap = new TreeMap<>(scores);
        System.out.println("  treeMap firstEntry: " + treeMap.firstEntry()); // alphabetically first
        System.out.println("  treeMap lastEntry : " + treeMap.lastEntry());  // alphabetically last

        // ── sequencedKeySet, sequencedValues, sequencedEntrySet ──
        // Return SequencedCollection views of keys/values/entries
        System.out.println("  sequencedKeySet first: " + scores.sequencedKeySet().getFirst());
        System.out.println("  sequencedValues last : " + scores.sequencedValues().getLast());
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 3 — String Templates (Preview Java 21)
    // Type-safe string interpolation
    // ─────────────────────────────────────────────
    static void stringTemplates() {
        // String Templates are a PREVIEW feature in Java 21
        // Stable in Java 23 (with some design changes)
        // Shown here conceptually — actual syntax requires --enable-preview

        // THE PROBLEM with current string formatting:
        String name  = "Alice";
        int    score = 95;

        // Option 1: concatenation — verbose, error-prone
        String concat = "Hello " + name + ", your score is " + score + "!";

        // Option 2: String.format / formatted — disconnected placeholders
        String formatted = "Hello %s, your score is %d!".formatted(name, score);

        // Option 3: StringBuilder — very verbose
        System.out.println("  concat   : " + concat);
        System.out.println("  formatted: " + formatted);

        // ── String Templates syntax (preview — conceptual) ──
        // STR."Hello \{name}, your score is \{score}!"
        // \{expr} — embeds any expression inline
        // STR processor — performs simple string interpolation
        // FMT processor — like String.format, supports format specifiers
        // RAW processor — returns a StringTemplate object (for custom processing)

        // Why better than formatted():
        // - Expressions are inline — easier to read
        // - Type-safe — compiler validates expressions
        // - Custom processors — SQL, JSON, HTML with injection prevention

        System.out.println("  String Templates (preview Java 21, stable Java 23):");
        System.out.println("  STR.\"Hello \\{name}, score=\\{score}\" — inline interpolation");
        System.out.println("  FMT.\"Score: %.2f\\{score}\"            — with format spec");
        System.out.println("  Custom processor for SQL/HTML          — injection safe");

        // ── Why it matters for security ──
        // Building SQL/HTML by concatenation → injection vulnerabilities
        // String template processors can sanitise/escape by design
        // e.g. SQL."SELECT * FROM users WHERE id = \{userId}"
        //      → processor escapes userId → no SQL injection possible
        System.out.println("  Security: SQL/HTML processors prevent injection attacks");
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 4 — Math.clamp and Other Additions
    // ─────────────────────────────────────────────
    static void otherAdditions() {

        // ── Math.clamp — constrain value within range ──
        // Before: Math.min(max, Math.max(min, value)) — hard to read
        // Java 21: Math.clamp(value, min, max) — clean one-liner

        // int version
        System.out.println("  clamp(5, 0, 10)  : " + Math.clamp(5, 0, 10));   // 5 — in range
        System.out.println("  clamp(-5, 0, 10) : " + Math.clamp(-5, 0, 10));  // 0 — below min
        System.out.println("  clamp(15, 0, 10) : " + Math.clamp(15, 0, 10));  // 10 — above max

        // long version
        System.out.println("  clamp(long)      : " + Math.clamp(200L, 0L, 100L)); // 100

        // double version
        System.out.println("  clamp(0.7, 0.0, 1.0): " + Math.clamp(0.7, 0.0, 1.0)); // 0.7

        // Practical: normalise user input, constrain progress percentage, limit speed
        int userInput = 150;
        int percentage = Math.clamp(userInput, 0, 100); // always 0-100
        System.out.println("  progress %: " + percentage); // 100

        // ── Character additions ──
        // Character.isEmoji(codePoint) — check if codepoint is an emoji
        // Useful for text processing, input validation
        int smiley   = "😊".codePointAt(0);
        int letterA  = 'A';
        System.out.println("  isEmoji(😊) : " + Character.isEmoji(smiley));  // true
        System.out.println("  isEmoji('A'): " + Character.isEmoji(letterA)); // false

        // ── StringBuilder/StringBuffer additions ──
        // repeat(str, count) — repeat a string N times in builder
        StringBuilder sb = new StringBuilder();
        sb.repeat("ab", 3); // ababab
        System.out.println("  sb.repeat   : " + sb);

        sb.setLength(0);
        sb.repeat('-', 10); // ----------
        System.out.println("  sb.repeat('-',10): " + sb);
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 5 — Java 21 as LTS
    // What stabilised, migration context
    // ─────────────────────────────────────────────
    static void java21AsLts() {
        System.out.println("  Java 21 — LTS (Long Term Support)");
        System.out.println("  Support until: September 2031 (Oracle)");
        System.out.println();

        // ── What became stable in Java 21 ──
        System.out.println("  Stable in Java 21:");
        System.out.println("  ✅ Virtual threads            (preview 19, 20 → stable 21)");
        System.out.println("  ✅ Pattern matching switch     (preview 17-20 → stable 21)");
        System.out.println("  ✅ Record patterns             (preview 19, 20 → stable 21)");
        System.out.println("  ✅ Sequenced collections       (new in 21)");
        System.out.println("  ✅ Math.clamp                  (new in 21)");
        System.out.println();

        // ── Still preview in Java 21 ──
        System.out.println("  Preview in Java 21:");
        System.out.println("  🔜 String Templates            (stable in 23 with changes)");
        System.out.println("  🔜 Structured Concurrency      (stable in 23)");
        System.out.println("  🔜 Scoped Values               (stable in 23)");
        System.out.println("  🔜 Unnamed Classes             (stable in 23)");
        System.out.println();

        // ── Java 17 → 21 migration ──
        System.out.println("  Java 17 → 21 migration:");
        System.out.println("  □ Mostly source compatible — clean upgrade");
        System.out.println("  □ Enable virtual threads: spring.threads.virtual.enabled=true");
        System.out.println("  □ Replace if-instanceof chains with pattern switch");
        System.out.println("  □ Use SequencedCollection APIs for cleaner collection access");
        System.out.println("  □ Update build tools: Maven 3.9+, Gradle 8.4+");
        System.out.println();

        // ── Why Java 21 for new projects ──
        // Virtual threads = simple blocking code with reactive throughput
        // No more reactive/async complexity for I/O-bound microservices
        // Pattern switch = cleaner domain modeling, no if-instanceof chains
        // Spring Boot 3.2+ fully leverages Java 21
        System.out.println("  For new projects: Java 21 is the recommended choice");
        System.out.println("  Virtual threads alone justify the upgrade from Java 17");
        System.out.println("  Spring Boot 3.2+: full virtual thread support built-in");
    }

}