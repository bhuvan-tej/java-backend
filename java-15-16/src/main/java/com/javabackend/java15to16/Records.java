package com.javabackend.java15to16;

import java.util.*;

/**
 *
 * Java 15-16 — Records
 *
 * Java 14: Records preview
 * Java 15: Records second preview
 * Java 16: Records stable ✅
 *
 * A record is a concise immutable data carrier.
 * The compiler generates: constructor, accessors,
 * equals(), hashCode(), toString() automatically.
 *
 */
public class Records {

    static void main(String[] args) {
        System.out.println("━━━ EXAMPLE 1 — Record Basics ━━━\n");
        recordBasics();

        System.out.println("\n━━━ EXAMPLE 2 — Compact Constructor ━━━\n");
        compactConstructor();

        System.out.println("\n━━━ EXAMPLE 3 — Custom Methods ━━━\n");
        customMethods();

        System.out.println("\n━━━ EXAMPLE 4 — Records with Interfaces ━━━\n");
        recordsWithInterfaces();

        System.out.println("\n━━━ EXAMPLE 5 — Records as DTOs ━━━\n");
        recordsAsDtos();
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 1 — Record Basics
    // ─────────────────────────────────────────────

    // Minimal record declaration — compiler generates everything
    record Point(int x, int y) {}

    // Equivalent class would need:
    // - private final fields
    // - constructor
    // - accessors (x(), y())
    // - equals(), hashCode(), toString()
    // Records eliminate all that boilerplate

    static void recordBasics() {
        Point p1 = new Point(3, 4);
        Point p2 = new Point(3, 4);
        Point p3 = new Point(1, 2);

        // ── Accessors — component name, not getX() ──
        System.out.println("  x()          : " + p1.x());
        System.out.println("  y()          : " + p1.y());

        // ── toString — auto-generated ──
        System.out.println("  toString     : " + p1);          // Point[x=3, y=4]

        // ── equals — value-based, compares all components ──
        System.out.println("  p1.equals(p2): " + p1.equals(p2)); // true
        System.out.println("  p1.equals(p3): " + p1.equals(p3)); // false
        System.out.println("  p1 == p2     : " + (p1 == p2));    // false — different instances

        // ── hashCode — consistent with equals ──
        System.out.println("  p1.hashCode(): " + p1.hashCode());
        System.out.println("  p2.hashCode(): " + p2.hashCode()); // same as p1

        // ── Immutability — fields are final ──
        // p1.x = 5; // compile error — no setters, fields are final

        // ── Records in collections ──
        Set<Point> points = new HashSet<>();
        points.add(p1);
        points.add(p2); // duplicate — not added (equals + hashCode work correctly)
        points.add(p3);
        System.out.println("  set size     : " + points.size()); // 2, not 3

        // ── Records as map keys — safe because hashCode/equals are correct ──
        Map<Point, String> labels = new HashMap<>();
        labels.put(p1, "origin-ish");
        System.out.println("  map get      : " + labels.get(new Point(3, 4))); // works
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 2 — Compact Constructor
    // ─────────────────────────────────────────────

    // Standard canonical constructor — explicit params
    record Range(int min, int max) {
        Range {
            // Compact constructor — no parameter list, fields assigned automatically
            // Use for validation and normalization
            if (min > max) {
                throw new IllegalArgumentException(
                        "min " + min + " must be <= max " + max);
            }
            // No need to write: this.min = min; this.max = max;
            // Compact constructor assigns automatically after the block
        }
    }

    record Person(String name, int age) {
        Person {
            // Normalize and validate in compact constructor
            name = name == null ? "" : name.strip(); // can reassign params before assignment
            if (age < 0) throw new IllegalArgumentException("age cannot be negative");
        }
    }

    static void compactConstructor() {
        Range r1 = new Range(1, 10);
        System.out.println("  range        : " + r1);

        try {
            new Range(10, 1); // min > max
        } catch (IllegalArgumentException e) {
            System.out.println("  invalid range: " + e.getMessage());
        }

        Person p = new Person("  Alice  ", 30);
        System.out.println("  person name  : '" + p.name() + "'"); // stripped
        System.out.println("  person age   : " + p.age());

        try {
            new Person("Bob", -1);
        } catch (IllegalArgumentException e) {
            System.out.println("  invalid age  : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 3 — Custom Methods in Records
    // ─────────────────────────────────────────────

    record Circle(double radius) implements Shape {
        // Compact constructor for validation
        Circle {
            if (radius <= 0) throw new IllegalArgumentException("radius must be > 0");
        }

        // Custom instance methods — allowed
        public double area() { return Math.PI * radius * radius; }
        double circumference() { return 2 * Math.PI * radius; }
        boolean contains(Point p) {
            return Math.sqrt(p.x() * p.x() + p.y() * p.y()) <= radius;
        }

        // Custom static methods — allowed
        static Circle unit() { return new Circle(1.0); }

        // Override toString — allowed, replaces generated one
        // (usually not needed — generated one is good)

        // Custom accessor — allowed, override generated one
        // (rarely needed)
        @Override
        public double radius() {
            return Math.round(radius * 100.0) / 100.0; // round to 2dp
        }
    }

    static void customMethods() {
        Circle c = new Circle(5.0);
        System.out.println("  radius       : " + c.radius());
        System.out.printf("  area         : %.2f%n", c.area());
        System.out.printf("  circumference: %.2f%n", c.circumference());
        System.out.println("  contains(3,4): " + c.contains(new Point(3, 4)));
        System.out.println("  contains(6,0): " + c.contains(new Point(6, 0)));
        System.out.println("  unit circle  : " + Circle.unit());

        // ── What records CANNOT have ──
        // - Instance fields other than record components
        // - Non-static initializers
        // - abstract, native methods
        // - extend any class (records implicitly extend Record)
        // BUT they CAN implement interfaces
        System.out.println("  records: can add methods, cannot add instance fields");
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 4 — Records with Interfaces
    // ─────────────────────────────────────────────

    interface Shape {
        double area();
        default String describe() {
            return "Shape with area %.2f".formatted(area());
        }
    }

    interface Printable {
        void print();
    }

    // Records CAN implement interfaces
    record Rectangle(double width, double height) implements Shape, Printable {
        @Override public double area() { return width * height; }
        @Override public void print() {
            System.out.println("  Rectangle[" + width + "x" + height + "]");
        }
    }

    record Triangle(double base, double height) implements Shape {
        @Override public double area() { return 0.5 * base * height; }
    }

    static void recordsWithInterfaces() {
        List<Shape> shapes = List.of(
                new Rectangle(4, 5),
                new Triangle(3, 6),
                new Circle(2)
        );

        shapes.forEach(s -> System.out.println("  " + s.describe()));

        Rectangle rect = new Rectangle(4, 5);
        rect.print();

        // Records work naturally as polymorphic value objects
        double totalArea = shapes.stream()
                .mapToDouble(Shape::area)
                .sum();
        System.out.printf("  total area   : %.2f%n", totalArea);
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 5 — Records as DTOs and Value Objects
    // ─────────────────────────────────────────────

    // API response DTO
    record UserDto(String id, String name, String email) {}

    // Domain value object
    record Money(double amount, String currency) {
        Money {
            if (amount < 0) throw new IllegalArgumentException("amount cannot be negative");
            currency = currency.toUpperCase();
        }
        Money add(Money other) {
            if (!currency.equals(other.currency))
                throw new IllegalArgumentException("currency mismatch");
            return new Money(amount + other.amount, currency);
        }
        @Override public String toString() {
            return "%.2f %s".formatted(amount, currency);
        }
    }

    // Coordinates value object
    record GeoLocation(double lat, double lon) {
        double distanceTo(GeoLocation other) {
            double dLat = Math.toRadians(other.lat - lat);
            double dLon = Math.toRadians(other.lon - lon);
            return Math.sqrt(dLat * dLat + dLon * dLon) * 6371; // rough km
        }
    }

    static void recordsAsDtos() {
        // DTO usage
        UserDto user = new UserDto("U001", "Alice", "alice@example.com");
        System.out.println("  user dto     : " + user);
        System.out.println("  user email   : " + user.email());

        // Value object with behavior
        Money price    = new Money(100.00, "usd");
        Money tax      = new Money(18.00,  "USD");
        Money total    = price.add(tax);
        System.out.println("  price        : " + price);
        System.out.println("  total        : " + total);

        // Records in a list — sorted, filtered
        List<UserDto> users = List.of(
                new UserDto("U1", "Alice", "alice@example.com"),
                new UserDto("U2", "Bob",   "bob@example.com"),
                new UserDto("U3", "Carol", "carol@example.com")
        );
        users.stream()
                .filter(u -> u.name().startsWith("A"))
                .map(UserDto::email)
                .forEach(e -> System.out.println("  filtered     : " + e));

        // Records are perfect for:
        // - API request/response DTOs
        // - Value objects (Money, Coordinates, Range)
        // - Tuple-like return values
        // - Immutable configuration objects
        // - Map keys (correct equals + hashCode)
        System.out.println("  records: DTOs · value objects · tuples · map keys ✓");
    }

}