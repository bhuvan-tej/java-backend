package com.javabackend.java11;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.*;

/**
 *
 * Java 11 — String, Files, Misc
 *
 * Java 11 (September 2018) — LTS release
 * Key theme: API polish, String improvements, Files convenience
 *
 * FEATURES COVERED
 *  1. String methods     — isBlank, strip, lines, repeat
 *  2. Files additions    — readString, writeString
 *  3. Predicate.not      — negate method references
 *  4. var in lambdas     — annotations on lambda parameters
 *  5. Collection.toArray — toArray(IntFunction)
 *
 */
public class Java11StringAndMisc {

    public static void main(String[] args) throws IOException {
        System.out.println("━━━ EXAMPLE 1 — String Methods ━━━\n");
        stringMethods();

        System.out.println("\n━━━ EXAMPLE 2 — Files Additions ━━━\n");
        filesAdditions();

        System.out.println("\n━━━ EXAMPLE 3 — Predicate.not ━━━\n");
        predicateNot();

        System.out.println("\n━━━ EXAMPLE 4 — var in Lambdas ━━━\n");
        varInLambdas();

        System.out.println("\n━━━ EXAMPLE 5 — Collection.toArray ━━━\n");
        collectionToArray();
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 1 — String Methods
    // ─────────────────────────────────────────────
    static void stringMethods() {
        // ── isBlank — true if empty or only whitespace ──
        // Different from isEmpty() — isEmpty only checks length == 0
        System.out.println("  \"\".isEmpty()      : " + "".isEmpty());       // true
        System.out.println("  \" \".isEmpty()     : " + " ".isEmpty());      // false
        System.out.println("  \"\".isBlank()      : " + "".isBlank());       // true
        System.out.println("  \" \".isBlank()     : " + " ".isBlank());      // true
        System.out.println("  \"  hi \".isBlank() : " + "  hi  ".isBlank()); // false

        // ── strip vs trim ──
        // trim() — removes ASCII whitespace (chars <= 32)
        // strip() — removes Unicode whitespace — aware of \u2000, \u3000 etc
        // Always prefer strip() over trim() for Unicode-safe code
        String padded = "  Hello World  ";
        System.out.println("  trim()           : '" + padded.trim() + "'");
        System.out.println("  strip()          : '" + padded.strip() + "'");
        System.out.println("  stripLeading()   : '" + padded.stripLeading() + "'");
        System.out.println("  stripTrailing()  : '" + padded.stripTrailing() + "'");

        // Unicode whitespace — trim() misses it, strip() handles it
        String unicode = "\u2000Hello\u2000"; // ideographic space
        System.out.println("  unicode trim()   : '" + unicode.trim() + "'");   // not stripped
        System.out.println("  unicode strip()  : '" + unicode.strip() + "'");  // stripped

        // ── lines — split into stream of lines ──
        // Handles \n, \r, \r\n — better than split("\\n")
        String multiline = "line one\nline two\nline three\n";
        List<String> lines = multiline.lines().collect(Collectors.toList());
        System.out.println("  lines()          : " + lines);

        // Practical: parse CSV or config block
        String config = """
            host=localhost
            port=8080
            debug=true
            """;
        Map<String, String> configMap = config.lines()
                .filter(l -> l.contains("="))
                .map(l -> l.split("=", 2))
                .collect(Collectors.toMap(a -> a[0].strip(), a -> a[1].strip()));
        System.out.println("  config map       : " + configMap);

        // ── repeat — repeat a string N times ──
        System.out.println("  \"ab\".repeat(3)   : " + "ab".repeat(3));   // ababab
        System.out.println("  \"-\".repeat(20)   : " + "-".repeat(20));   // separator line

        // Practical: padding, dividers, test data
        String divider = "═".repeat(40);
        System.out.println("  divider          : " + divider);
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 2 — Files Additions
    // ─────────────────────────────────────────────
    static void filesAdditions() throws IOException {
        Path tempFile = Files.createTempFile("java11-demo", ".txt");

        // ── Files.writeString — write string to file in one call ──
        // Before Java 11: Files.write(path, content.getBytes(charset))
        String content = "Hello from Java 11\nSecond line\nThird line";
        Files.writeString(tempFile, content);
        System.out.println("  writeString      : written to " + tempFile.getFileName());

        // ── Files.readString — read entire file as string ──
        // Before Java 11: new String(Files.readAllBytes(path), charset)
        String read = Files.readString(tempFile);
        System.out.println("  readString       : " + read.lines().findFirst().orElse(""));

        // With explicit charset
        Files.writeString(tempFile, "UTF-8 content: café");
        String utf8 = Files.readString(tempFile, java.nio.charset.StandardCharsets.UTF_8);
        System.out.println("  readString UTF-8 : " + utf8);

        // ── Files.isSameFile — check if two paths point to same file ──
        Path same = tempFile.toAbsolutePath();
        System.out.println("  isSameFile       : " + Files.isSameFile(tempFile, same));

        // Cleanup
        Files.deleteIfExists(tempFile);
        System.out.println("  temp file deleted");

        // ── Before vs after comparison ──
        // Before Java 11:
        // String content = new String(Files.readAllBytes(path));          // no charset
        // String content = new String(Files.readAllBytes(path), UTF_8);   // with charset
        // Files.write(path, content.getBytes(UTF_8));

        // Java 11:
        // String content = Files.readString(path);
        // String content = Files.readString(path, UTF_8);
        // Files.writeString(path, content);
        // Files.writeString(path, content, UTF_8);
        System.out.println("  readString/writeString — one-line file I/O ✓");
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 3 — Predicate.not
    // ─────────────────────────────────────────────
    static void predicateNot() {
        List<String> lines = List.of("hello", "", "  ", "world", "", "java");

        // ── Before Java 11 — negating a method reference was ugly ──
        // Cannot write: .filter(!String::isBlank) — doesn't compile
        // Had to write a lambda:
        List<String> beforeJava11 = lines.stream()
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
        System.out.println("  before Java 11   : " + beforeJava11);

        // ── Java 11 — Predicate.not wraps a method reference with negation ──
        List<String> java11 = lines.stream()
                .filter(Predicate.not(String::isBlank))
                .collect(Collectors.toList());
        System.out.println("  Predicate.not    : " + java11);

        // Works with any method reference
        List<String> withNulls = Arrays.asList("a", null, "b", null, "c");
        List<String> nonNull = withNulls.stream()
                .filter(Predicate.not(Objects::isNull))
                .collect(Collectors.toList());
        System.out.println("  not(isNull)      : " + nonNull);

        // Custom predicate negation
        Predicate<Integer> isEven = n -> n % 2 == 0;
        List<Integer> odds = List.of(1,2,3,4,5,6).stream()
                .filter(Predicate.not(isEven))
                .collect(Collectors.toList());
        System.out.println("  not(isEven)      : " + odds);
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 4 — var in Lambda Parameters
    // ─────────────────────────────────────────────
    static void varInLambdas() {
        // Java 10: var for local variables
        // Java 11: var for lambda parameters — enables annotations

        // ── Basic var in lambda ──
        // Same as (s) -> s.toUpperCase() — no practical difference without annotations
        List<String> names = List.of("alice", "bob", "carol");

        List<String> upper = names.stream()
                .map((var s) -> s.toUpperCase()) // var in lambda param
                .collect(Collectors.toList());
        System.out.println("  var in lambda    : " + upper);

        // ── The real purpose — annotations on lambda parameters ──
        // Without var, you cannot annotate lambda params with @NonNull etc.
        // This is important for static analysis tools (Checkstyle, SpotBugs, Lombok)

        // Example with annotation (shown as comment — requires annotation import)
        // List<String> result = names.stream()
        //     .map((@NonNull var s) -> s.toUpperCase()) // annotation on lambda param
        //     .collect(Collectors.toList());

        // ── Rules for var in lambdas ──
        // All parameters must use var or none — cannot mix
        // (var x, var y) -> x + y  ✅
        // (var x, y) -> x + y      ❌ — must be consistent
        // (x, y) -> x + y          ✅ — no var also fine

        System.out.println("  var in lambda: enables @NonNull and other annotations");
        System.out.println("  all params must use var or none — no mixing");
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 5 — Collection.toArray(IntFunction)
    // ─────────────────────────────────────────────
    static void collectionToArray() {
        List<String> names = List.of("Alice", "Bob", "Carol");

        // ── Before Java 11 — verbose and ugly ──
        String[] before = names.toArray(new String[0]); // pass empty array as type hint
        System.out.println("  before Java 11   : " + Arrays.toString(before));

        // ── Java 11 — method reference, cleaner ──
        String[] after = names.toArray(String[]::new); // IntFunction<String[]>
        System.out.println("  toArray(::new)   : " + Arrays.toString(after));

        // Works with any type
        List<Integer> numbers = List.of(3, 1, 4, 1, 5, 9);
        Integer[] arr = numbers.toArray(Integer[]::new);
        System.out.println("  Integer[]        : " + Arrays.toString(arr));

        // Stream also supports this form
        String[] fromStream = names.stream()
                .filter(n -> n.length() > 3)
                .toArray(String[]::new);
        System.out.println("  stream toArray   : " + Arrays.toString(fromStream));
    }

}