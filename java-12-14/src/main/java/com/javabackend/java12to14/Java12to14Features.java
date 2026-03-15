package com.javabackend.java12to14;

import java.util.*;

/**
 *
 * Java 12-14 Features
 *
 * Java 12 (March 2019)     — non-LTS
 * Java 13 (September 2019) — non-LTS
 * Java 14 (March 2020)     — non-LTS
 *
 * Key theme: switch expressions, text blocks, pattern matching preview
 *
 * FEATURES COVERED
 *  1. Switch expressions       — arrow syntax, returns value, exhaustive (stable Java 14)
 *  2. Text blocks              — multiline strings with """ (stable Java 15, preview 13-14)
 *  3. String additions         — indent(), transform(), formatted()
 *  4. Pattern matching instanceof — instanceof String s (preview Java 14)
 *  5. Helpful NullPointerExceptions — better NPE messages (Java 14)
 *
 */

public class Java12to14Features {

    public static void main(String[] args) {
        System.out.println("━━━ EXAMPLE 1 — Switch Expressions ━━━\n");
        switchExpressions();

        System.out.println("\n━━━ EXAMPLE 2 — Text Blocks ━━━\n");
        textBlocks();

        System.out.println("\n━━━ EXAMPLE 3 — String Additions ━━━\n");
        stringAdditions();

        System.out.println("\n━━━ EXAMPLE 4 — Pattern Matching instanceof ━━━\n");
        patternMatchingInstanceof();

        System.out.println("\n━━━ EXAMPLE 5 — Helpful NullPointerExceptions ━━━\n");
        helpfulNullPointerExceptions();
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 1 — Switch Expressions
    // ─────────────────────────────────────────────
    static void switchExpressions() {
        // ── Old switch statement — fall-through, verbose, error-prone ──
        String day = "MONDAY";
        String type;
        switch (day) {
            case "MONDAY":
            case "TUESDAY":
            case "WEDNESDAY":
            case "THURSDAY":
            case "FRIDAY":
                type = "Weekday";
                break; // forget this → fall-through bug
            case "SATURDAY":
            case "SUNDAY":
                type = "Weekend";
                break;
            default:
                type = "Unknown";
        }
        System.out.println("  old switch: " + type);

        // ── New switch expression — arrow syntax, no fall-through, returns value ──
        String dayType = switch (day) {
            case "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY" -> "Weekday";
            case "SATURDAY", "SUNDAY" -> "Weekend";
            default -> "Unknown";
        };
        System.out.println("  new switch: " + dayType);

        // ── Switch expression with blocks and yield ──
        // Use yield when you need multiple statements in a case
        int numLetters = switch (day) {
            case "MONDAY", "FRIDAY", "SUNDAY" -> 6;
            case "TUESDAY"                    -> 7;
            case "THURSDAY", "SATURDAY"       -> 8;
            case "WEDNESDAY"                  -> {
                System.out.println("  (computing wednesday length)");
                yield 9; // yield returns value from a block
            }
            default -> -1;
        };
        System.out.println("  letters in " + day + ": " + numLetters);

        // ── Exhaustiveness — compiler enforces all cases covered ──
        // For enums: all values must be handled or default provided
        // Prevents missed cases that were silent bugs in old switch
        Season season = Season.SUMMER;
        String description = switch (season) {
            case SPRING -> "Warm and rainy";
            case SUMMER -> "Hot and sunny";
            case AUTUMN -> "Cool and windy";
            case WINTER -> "Cold and snowy";
            // no default needed — all enum values covered
        };
        System.out.println("  season: " + description);

        // ── Switch as statement (still valid) ──
        // Arrow syntax also works as a statement — no fall-through
        switch (day) {
            case "MONDAY" -> System.out.println("  start of week");
            case "FRIDAY" -> System.out.println("  end of week");
            default       -> System.out.println("  midweek: " + day);
        }

        // ── Evolution ──
        // Java 12: switch expressions as preview
        // Java 13: switch expressions second preview, yield keyword added
        // Java 14: switch expressions stable
        System.out.println("  switch expressions: stable since Java 14");
    }

    enum Season { SPRING, SUMMER, AUTUMN, WINTER }

    // ─────────────────────────────────────────────
    // EXAMPLE 2 — Text Blocks
    // ─────────────────────────────────────────────
    static void textBlocks() {
        // ── Old way — string concatenation or escape hell ──
        String jsonOld = "{\n" +
                "  \"name\": \"Alice\",\n" +
                "  \"role\": \"developer\"\n" +
                "}";
        System.out.println("  old JSON:\n" + jsonOld);

        // ── Text block — multiline string with """ ──
        // Opening """ must be followed by newline
        // Closing """ position controls indentation stripping
        String json = """
                {
                  "name": "Alice",
                  "role": "developer"
                }
                """;
        System.out.println("  text block JSON:\n" + json);

        // ── HTML ──
        String html = """
                <html>
                    <body>
                        <h1>Hello Java 13</h1>
                    </body>
                </html>
                """;
        System.out.println("  HTML text block:\n" + html);

        // ── SQL ──
        String sql = """
                SELECT u.name, u.email
                FROM   users u
                WHERE  u.active = true
                ORDER  BY u.name
                """;
        System.out.println("  SQL text block:\n" + sql);

        // ── Indentation control ──
        // Common leading whitespace is stripped based on closing """ position
        // Closing """ on same line as last content — no trailing newline
        String noTrailingNewline = """
                hello"""; // no newline at end
        System.out.println("  no trailing newline: '" + noTrailingNewline + "'");

        // ── Escape sequences in text blocks ──
        // \n — still works
        // \s — trailing space (Java 14) — prevents IDE from stripping trailing spaces
        // \ at end of line — line continuation, no newline inserted
        String multiLine = """
                line one \
                continues here
                line two
                """;
        System.out.println("  line continuation:\n" + multiLine);

        // ── Evolution ──
        // Java 13: text blocks preview
        // Java 14: text blocks second preview, \s and line continuation added
        // Java 15: text blocks stable
        System.out.println("  text blocks: stable since Java 15");
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 3 — String Additions (Java 12)
    // ─────────────────────────────────────────────
    static void stringAdditions() {
        // ── indent — add/remove leading whitespace, normalize line endings ──
        String text = "hello\nworld\njava";
        String indented = text.indent(4); // add 4 spaces to each line
        System.out.println("  indent(4):\n" + indented);

        String dedented = indented.indent(-4); // remove 4 spaces
        System.out.println("  indent(-4): '" + dedented.strip() + "'");

        // indent() also normalises line endings and adds trailing newline
        // Useful for formatting code output, log messages, display

        // ── transform — apply a function to the string ──
        // Allows chaining arbitrary transformations inline
        String result = "  hello world  "
                .transform(String::strip)
                .transform(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
                .transform(s -> s + "!");
        System.out.println("  transform: " + result);

        // Without transform — same result but less fluent
        String s = "  hello world  ";
        s = s.strip();
        s = s.substring(0, 1).toUpperCase() + s.substring(1);
        s = s + "!";
        System.out.println("  without transform: " + s);

        // ── formatted — instance version of String.format ──
        // Java 15 feature but commonly grouped here
        String template = "Hello %s, you have %d messages";
        String formatted = template.formatted("Alice", 5);
        System.out.println("  formatted: " + formatted);

        // vs String.format — same result, more fluent
        String old = String.format("Hello %s, you have %d messages", "Alice", 5);
        System.out.println("  String.format: " + old);
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 4 — Pattern Matching instanceof (Java 14 preview, stable 16)
    // ─────────────────────────────────────────────
    static void patternMatchingInstanceof() {
        Object obj = "Hello Java 14";

        // ── Old way — cast after instanceof ──
        if (obj instanceof String) {
            String str = (String) obj; // redundant cast
            System.out.println("  old instanceof: length=" + str.length());
        }

        // ── Pattern matching — bind variable in one step ──
        // instanceof check + cast + variable binding — all in one
        if (obj instanceof String str) {
            System.out.println("  pattern instanceof: length=" + str.length());
            System.out.println("  pattern instanceof: upper=" + str.toUpperCase());
        }

        // ── Scope of binding variable ──
        // str is only in scope where the pattern is guaranteed to match
        if (obj instanceof String str && str.length() > 5) {
            // str is in scope here — both conditions true
            System.out.println("  with guard: " + str.substring(0, 5));
        }

        // ── Useful in equals() implementation ──
        // Old — verbose, two casts
        // New — clean
        System.out.println("  in equals(): obj instanceof MyClass other && field.equals(other.field)");

        // ── Works with complex types ──
        Object number = 42;
        Object text   = "hello";
        Object list   = List.of(1, 2, 3);

        for (Object item : new Object[]{number, text, list}) {
            String desc = switch (item) {  // Java 17+ full pattern switch
                default -> {
                    if (item instanceof Integer i)    yield "Integer: " + i;
                    if (item instanceof String  s)    yield "String: "  + s;
                    if (item instanceof List<?> l)    yield "List size: " + l.size();
                    yield "unknown";
                }
            };
            System.out.println("  pattern match: " + desc);
        }

        // ── Evolution ──
        // Java 14: pattern matching instanceof as preview
        // Java 15: second preview
        // Java 16: stable
        System.out.println("  pattern instanceof: stable since Java 16");
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 5 — Helpful NullPointerExceptions (Java 14)
    // ─────────────────────────────────────────────
    static void helpfulNullPointerExceptions() {
        // Java 14 improved NPE messages to tell you EXACTLY what was null
        // Enabled by default from Java 15+

        // ── Before Java 14 ──
        // NullPointerException: null
        // (no info about what was null or where)

        // ── Java 14+ ──
        // NullPointerException: Cannot invoke "String.length()"
        //   because "<local1>" is null
        // or
        // NullPointerException: Cannot read field "street"
        //   because "user.address" is null

        // Demonstrate
        try {
            String name = null;
            int len = name.length(); // NPE
        } catch (NullPointerException e) {
            System.out.println("  NPE message: " + e.getMessage());
            // Java 14+: Cannot invoke "String.length()" because "name" is null
        }

        // ── Chained NPE — most valuable case ──
        try {
            User user = new User("Alice", null); // address is null
            String city = user.address.city;     // NPE on address
        } catch (NullPointerException e) {
            System.out.println("  chained NPE: " + e.getMessage());
            // Java 14+: Cannot read field "city" because "user.address" is null
        }

        // ── Method chain NPE ──
        try {
            String s = null;
            s.strip().toUpperCase(); // NPE on strip()
        } catch (NullPointerException e) {
            System.out.println("  chain NPE: " + e.getMessage());
        }

        // Before this improvement, debugging NPEs in chained calls required
        // breaking them up into separate lines to find the null.
        // Now the JVM tells you exactly what was null in the chain.
        System.out.println("  helpful NPEs: precise null identification ✓");
    }

    // Helper classes
    static class User {
        String name;
        Address address;
        User(String name, Address address) {
            this.name = name;
            this.address = address;
        }
    }

    static class Address {
        String city;
        Address(String city) { this.city = city; }
    }

}