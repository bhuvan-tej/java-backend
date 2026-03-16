package com.javabackend.java21;

import java.util.*;

/**
 *
 * Java 21 — Pattern Matching
 *
 * Java 14: instanceof pattern matching preview
 * Java 16: instanceof pattern matching stable
 * Java 17: pattern matching switch preview
 * Java 18: pattern matching switch second preview
 * Java 19: pattern matching switch third preview
 * Java 21: pattern matching switch stable ✅
 *          record patterns stable ✅
 *
 * FEATURES COVERED
 *  1. Pattern matching switch    — type patterns, exhaustiveness
 *  2. Guarded patterns           — when clause
 *  3. Record patterns            — destructuring records in patterns
 *  4. Null handling in switch    — explicit null case
 *  5. Real world — domain dispatch
 *
 */
public class PatternMatching {

    public static void main(String[] args) {
        System.out.println("━━━ EXAMPLE 1 — Pattern Matching Switch ━━━\n");
        patternMatchingSwitch();

        System.out.println("\n━━━ EXAMPLE 2 — Guarded Patterns ━━━\n");
        guardedPatterns();

        System.out.println("\n━━━ EXAMPLE 3 — Record Patterns ━━━\n");
        recordPatterns();

        System.out.println("\n━━━ EXAMPLE 4 — Null Handling in Switch ━━━\n");
        nullHandling();

        System.out.println("\n━━━ EXAMPLE 5 — Real World Domain Dispatch ━━━\n");
        domainDispatch();
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 1 — Pattern Matching Switch
    // ─────────────────────────────────────────────
    static void patternMatchingSwitch() {

        // ── Before Java 21 — if-instanceof chain ──
        // Verbose, no exhaustiveness check, easy to miss a type
        Object obj = 42;
        String oldResult;
        if (obj instanceof Integer i)      oldResult = "int: " + i;
        else if (obj instanceof String s)  oldResult = "str: " + s;
        else if (obj instanceof Double d)  oldResult = "dbl: " + d;
        else                               oldResult = "other";
        System.out.println("  old style: " + oldResult);

        // ── Java 21 — pattern matching switch ──
        // Compiler checks exhaustiveness — must cover all types or have default
        // Each case binds a typed variable — no cast needed
        // Cleaner, faster to read, compiler-verified
        Object[] samples = {42, "hello", 3.14, List.of(1, 2), null};

        for (Object o : samples) {
            String result = switch (o) {
                case Integer i  -> "Integer: " + i * 2;       // i is int, multiply it
                case String  s  -> "String: " + s.toUpperCase(); // s is String, use it
                case Double  d  -> "Double: " + String.format("%.2f", d);
                case List<?> l  -> "List of size: " + l.size();
                case null       -> "null value";               // explicit null handling
                default         -> "unknown: " + o.getClass().getSimpleName();
            };
            System.out.println("  " + result);
        }

        // ── With sealed types — no default needed ──
        // Compiler knows all permitted subtypes → exhaustiveness guaranteed
        Shape circle = new Circle(5.0);
        Shape rect   = new Rectangle(4.0, 6.0);

        for (Shape shape : new Shape[]{circle, rect}) {
            // No default needed — sealed Shape has only Circle and Rectangle
            String desc = switch (shape) {
                case Circle c    -> "Circle r=%.1f area=%.2f".formatted(c.radius(), c.area());
                case Rectangle r -> "Rect %.1fx%.1f area=%.2f".formatted(r.w(), r.h(), r.area());
            };
            System.out.println("  " + desc);
        }
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 2 — Guarded Patterns
    // Add conditions to pattern cases with 'when'
    // ─────────────────────────────────────────────
    static void guardedPatterns() {
        // ── 'when' clause — extra condition after type match ──
        // Only matches if BOTH the type check AND the when condition are true
        // More expressive than a nested if inside the case block

        Object[] values = {-5, 0, 42, 150, "hello", "hi", 3.14};

        for (Object v : values) {
            String result = switch (v) {
                // Integer matched AND negative
                case Integer i when i < 0    -> "negative int: " + i;
                // Integer matched AND zero
                case Integer i when i == 0   -> "zero";
                // Integer matched AND in range
                case Integer i when i <= 100 -> "small int: " + i;
                // Integer matched — must be > 100 (previous cases handled rest)
                case Integer i               -> "large int: " + i;
                // String with length condition
                case String s when s.length() > 3 -> "long string: " + s;
                case String s                -> "short string: " + s;
                // Default for anything else
                default                      -> "other: " + v;
            };
            System.out.println("  " + result);
        }

        // ── Order matters — more specific cases FIRST ──
        // 'when i < 0' must come before plain 'case Integer i'
        // Otherwise the plain case catches everything first — guarded case unreachable
        // Compiler warns about unreachable cases

        // ── Practical: categorise HTTP status codes ──
        int[] statuses = {200, 201, 301, 404, 500, 503};
        for (int status : statuses) {
            // Box to Integer for pattern switch
            String category = switch ((Integer) status) {
                case Integer s when s >= 500 -> "Server Error";
                case Integer s when s >= 400 -> "Client Error";
                case Integer s when s >= 300 -> "Redirect";
                case Integer s when s >= 200 -> "Success";
                default                      -> "Informational";
            };
            System.out.println("  HTTP " + status + " → " + category);
        }
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 3 — Record Patterns
    // Destructure records directly in pattern match
    // ─────────────────────────────────────────────

    // Records used for destructuring
    record Point(int x, int y) {}
    record Line(Point start, Point end) {}
    record ColoredPoint(Point point, String color) {}

    static void recordPatterns() {
        // ── Basic record pattern — extract components inline ──
        // Before: instanceof Point p, then p.x(), p.y() separately
        // Java 21: instanceof Point(int x, int y) — extracts both at once
        Object obj = new Point(3, 4);

        // Old way
        if (obj instanceof Point p) {
            System.out.println("  old: x=" + p.x() + " y=" + p.y());
        }

        // Java 21 — record pattern destructuring
        if (obj instanceof Point(int x, int y)) {
            // x and y are directly in scope — no need for p.x(), p.y()
            System.out.println("  record pattern: x=" + x + " y=" + y);
            System.out.println("  distance from origin: " +
                    String.format("%.2f", Math.sqrt(x * x + y * y)));
        }

        // ── Nested record patterns — destructure recursively ──
        Object line = new Line(new Point(0, 0), new Point(3, 4));

        if (line instanceof Line(Point(int x1, int y1), Point(int x2, int y2))) {
            // All four components extracted in one pattern
            double length = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
            System.out.println("  line from (%d,%d) to (%d,%d) length=%.2f"
                    .formatted(x1, y1, x2, y2, length));
        }

        // ── Record patterns in switch ──
        Object[] shapes = {
                new Point(0, 0),
                new Point(3, 4),
                new ColoredPoint(new Point(1, 2), "red"),
                new Line(new Point(0, 0), new Point(5, 5))
        };

        for (Object s : shapes) {
            String desc = switch (s) {
                // Destructure Point — check if origin
                case Point(int x, int y) when x == 0 && y == 0
                        -> "origin point";
                // Destructure Point — general
                case Point(int x, int y)
                        -> "point at (%d,%d)".formatted(x, y);
                // Destructure ColoredPoint — including nested Point
                case ColoredPoint(Point(int x, int y), String color)
                        -> "%s point at (%d,%d)".formatted(color, x, y);
                // Destructure Line
                case Line(Point start, Point end)
                        -> "line from %s to %s".formatted(start, end);
                default -> "unknown";
            };
            System.out.println("  " + desc);
        }
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 4 — Null Handling in Switch
    // ─────────────────────────────────────────────
    static void nullHandling() {
        // ── Before Java 21 — switch threw NullPointerException on null ──
        // Had to null-check before entering switch
        String value = null;

        // Old approach — null check outside
        if (value == null) {
            System.out.println("  old null check: value was null");
        } else {
            switch (value) {
                case "A" -> System.out.println("A");
                default  -> System.out.println("other");
            }
        }

        // ── Java 21 — explicit null case in switch ──
        // null is handled as just another case — no NPE, no external check
        Object[] values = {null, "hello", 42, null, 3.14};

        for (Object o : values) {
            String result = switch (o) {
                case null       -> "got null";          // handle null explicitly
                case String s   -> "string: " + s;
                case Integer i  -> "integer: " + i;
                default         -> "other: " + o;
            };
            System.out.println("  " + result);
        }

        // ── null + default can be combined ──
        // When null and default should do the same thing
        for (Object o : values) {
            String result = switch (o) {
                case String s  -> "string: " + s;
                case null, default -> "not a string"; // null falls into default
            };
            System.out.println("  combined: " + result);
        }
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 5 — Real World Domain Dispatch
    // Sealed + records + pattern switch together
    // ─────────────────────────────────────────────

    // ── Event hierarchy ──
    sealed interface DomainEvent permits
            DomainEvent.UserRegistered,
            DomainEvent.OrderPlaced,
            DomainEvent.PaymentProcessed,
            DomainEvent.OrderShipped {

        record UserRegistered(String userId, String email)       implements DomainEvent {}
        record OrderPlaced(String orderId, String userId, double amount) implements DomainEvent {}
        record PaymentProcessed(String paymentId, double amount, boolean success) implements DomainEvent {}
        record OrderShipped(String orderId, String trackingId)   implements DomainEvent {}
    }

    static void domainDispatch() {
        var events = List.of(
                new DomainEvent.UserRegistered("U1", "alice@example.com"),
                new DomainEvent.OrderPlaced("O1", "U1", 150.00),
                new DomainEvent.PaymentProcessed("P1", 150.00, true),
                new DomainEvent.PaymentProcessed("P2", 75.00, false),
                new DomainEvent.OrderShipped("O1", "TRACK-XYZ")
        );

        System.out.println("  Processing events:");
        for (var event : events) {
            processEvent(event);
        }

        // ── Aggregate stats using pattern switch ──
        double totalRevenue = events.stream()
                .mapToDouble(e -> switch (e) {
                    // Only count successful payments
                    case DomainEvent.PaymentProcessed(var id, var amount, var success)
                            when success -> amount;
                    // All other events contribute 0
                    default -> 0.0;
                })
                .sum();
        System.out.printf("  Total revenue: $%.2f%n", totalRevenue);

        long failedPayments = events.stream()
                .filter(e -> e instanceof DomainEvent.PaymentProcessed p && !p.success())
                .count();
        System.out.println("  Failed payments: " + failedPayments);
    }

    static void processEvent(DomainEvent event) {
        // Sealed + pattern switch = exhaustive, no default needed
        // Compiler enforces all DomainEvent subtypes are handled
        // Adding a new subtype → compile error here → can't miss it
        String action = switch (event) {
            case DomainEvent.UserRegistered(var userId, var email)
                    -> "Send welcome email to " + email;

            case DomainEvent.OrderPlaced(var orderId, var userId, var amount)
                    -> "Reserve inventory for order " + orderId
                    + " ($" + amount + ")";

            case DomainEvent.PaymentProcessed(var paymentId, var amount, var success)
                    when success
                    -> "Confirm order, send receipt for $" + amount;

            case DomainEvent.PaymentProcessed(var paymentId, var amount, var success)
                    -> "Payment failed for $" + amount + " — notify user";

            case DomainEvent.OrderShipped(var orderId, var trackingId)
                    -> "Send tracking " + trackingId + " for order " + orderId;
        };
        System.out.println("  [EVENT] " + action);
    }

    // ── Sealed shapes for Example 1 ──
    sealed interface Shape permits Circle, Rectangle {}
    record Circle(double radius)       implements Shape {
        double area() { return Math.PI * radius * radius; }
    }
    record Rectangle(double w, double h) implements Shape {
        double area() { return w * h; }
    }

}