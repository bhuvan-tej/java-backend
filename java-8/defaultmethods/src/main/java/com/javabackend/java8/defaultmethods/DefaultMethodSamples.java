package com.javabackend.java8.defaultmethods;

import java.util.*;

/**
 *
 * Default & Static Methods in Interfaces
 *
 * BEFORE Java 8 — interfaces had only abstract methods
 *  Adding a new method to an interface = breaking change
 *  Every implementing class had to add the method
 *
 * AFTER Java 8 — interfaces can have:
 *  default methods — concrete method with body, inheritable
 *  static methods — utility methods on the interface itself
 *
 * WHY IT EXISTS
 *  Primary reason: backward compatibility in the JDK itself
 *  Stream API was added to Collection without breaking existing code
 *  Collection.stream(), List.sort(), Map.forEach() are all default methods
 *
 */
public class DefaultMethodSamples {

    public static void main(String[] args) {
        System.out.println("━━━ EXAMPLE 1 — Basic Default Method ━━━\n");
        basicDefault();

        System.out.println("\n━━━ EXAMPLE 2 — Static Methods in Interface ━━━\n");
        staticMethods();

        System.out.println("\n━━━ EXAMPLE 3 — Diamond Problem ━━━\n");
        diamondProblem();

        System.out.println("\n━━━ EXAMPLE 4 — Override Default Method ━━━\n");
        overrideDefault();

        System.out.println("\n━━━ EXAMPLE 5 — Real World Patterns ━━━\n");
        realWorld();
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 1 — Basic Default Method
    // ─────────────────────────────────────────────
    static void basicDefault() {
        Greeter english = new EnglishGreeter();
        Greeter formal  = new FormalGreeter();

        // uses default implementation
        System.out.println(english.greet("Alice"));
        System.out.println(english.greetLoudly("Alice")); // default method

        // FormalGreeter overrides greet but inherits greetLoudly
        System.out.println(formal.greet("Alice"));
        System.out.println(formal.greetLoudly("Alice")); // still uses default
    }

    interface Greeter {
        String greet(String name); // abstract — must implement

        // Default — concrete, inheritable, can be overridden
        default String greetLoudly(String name) {
            return greet(name).toUpperCase();
        }
    }

    static class EnglishGreeter implements Greeter {
        public String greet(String name) { return "Hello, " + name; }
    }

    static class FormalGreeter implements Greeter {
        public String greet(String name) { return "Good day, " + name; }
        // greetLoudly inherited — uses this.greet() which is overridden
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 2 — Static Methods in Interface
    // ─────────────────────────────────────────────
    static void staticMethods() {
        // Static methods belong to the interface — not inherited by implementors
        Validator<String> emailValidator = Validator.of(
                s -> s != null && s.contains("@") && s.contains("."),
                "Must be a valid email");

        System.out.println(emailValidator.validate("alice@co.com")); // valid
        System.out.println(emailValidator.validate("notanemail"));   // invalid

        // Chain validators
        Validator<String> combined = emailValidator
                .and(Validator.of(s -> s.length() <= 50, "Too long"));
        System.out.println(combined.validate("alice@co.com")); // valid
    }

    interface Validator<T> {
        ValidationResult validate(T value);

        // Static factory — lives on the interface, not inherited
        static <T> Validator<T> of(java.util.function.Predicate<T> predicate,
                                   String message) {
            return value -> predicate.test(value)
                    ? ValidationResult.valid()
                    : ValidationResult.invalid(message);
        }

        // Default — combinable validators
        default Validator<T> and(Validator<T> other) {
            return value -> {
                ValidationResult result = this.validate(value);
                return result.isValid() ? other.validate(value) : result;
            };
        }
    }

    record ValidationResult(boolean isValid, String message) {
        static ValidationResult valid()           { return new ValidationResult(true, "OK"); }
        static ValidationResult invalid(String m) { return new ValidationResult(false, m); }
        public String toString() { return isValid ? "✓ valid" : "✗ " + message; }
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 3 — Diamond Problem
    // ─────────────────────────────────────────────
    static void diamondProblem() {
        // Two interfaces provide default method with same signature
        // Class must override to resolve ambiguity — compile error otherwise

        MultiGreeter mg = new MultiGreeter();
        System.out.println(mg.greetAll("Alice")); // calls InterfaceA's version explicitly
        System.out.println(mg.hello());           // no conflict — different names
    }

    interface InterfaceA {
        default String greetAll(String name) { return "A: Hello " + name; }
        default String hello()              { return "Hello from A"; }
    }

    interface InterfaceB {
        default String greetAll(String name) { return "B: Hi " + name; }
        // no hello() — no conflict
    }

    // Must override greetAll — two default implementations conflict
    static class MultiGreeter implements InterfaceA, InterfaceB {
        @Override
        public String greetAll(String name) {
            // Explicitly delegate to one interface using InterfaceName.super.method()
            return InterfaceA.super.greetAll(name) + " | " +
                    InterfaceB.super.greetAll(name);
        }
        // hello() inherited unambiguously from InterfaceA
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 4 — Override Default Method
    // ─────────────────────────────────────────────
    static void overrideDefault() {
        Shape circle    = new Circle(5);
        Shape rectangle = new Rectangle(4, 6);
        Shape custom    = new CustomShape();

        // circle and rectangle use default describe()
        // CustomShape overrides it
        System.out.println(circle.describe());
        System.out.println(rectangle.describe());
        System.out.println(custom.describe());

        // abstract class wins over interface default — class always wins
        ConcreteLogged cl = new ConcreteLogged();
        System.out.println(cl.log("event")); // abstract class version wins
    }

    interface Shape {
        double area();
        default String describe() {
            return getClass().getSimpleName() + " area=" + String.format("%.2f", area());
        }
    }

    static class Circle implements Shape {
        double radius;
        Circle(double r) { radius = r; }
        public double area() { return Math.PI * radius * radius; }
    }

    static class Rectangle implements Shape {
        double w, h;
        Rectangle(double w, double h) { this.w = w; this.h = h; }
        public double area() { return w * h; }
    }

    static class CustomShape implements Shape {
        public double area() { return 42.0; }
        @Override
        public String describe() { return "CustomShape — special!"; } // override
    }

    interface Logged {
        default String log(String msg) { return "[INTERFACE] " + msg; }
    }

    static abstract class AbstractLogged implements Logged {
        // Abstract class method always wins over interface default
        public String log(String msg) { return "[ABSTRACT CLASS] " + msg; }
    }

    static class ConcreteLogged extends AbstractLogged implements Logged {
        // AbstractLogged.log wins — class always beats interface default
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 5 — Real World Patterns
    // ─────────────────────────────────────────────
    static void realWorld() {
        // ── Pattern: Mixin — add behavior without inheritance ──
        // Auditable adds created/updated tracking to any class
        Order order = new Order("O1", 500);
        order.markCreated();
        System.out.println("created  : " + order.getCreatedAt());
        order.markUpdated();
        System.out.println("updated  : " + order.getUpdatedAt());
        System.out.println("order    : " + order.summary());

        // ── JDK examples of default methods ──
        System.out.println("\nJDK default methods:");

        // List.sort() — default method added in Java 8
        List<String> names = new ArrayList<>(Arrays.asList("Charlie", "Alice", "Bob"));
        names.sort(Comparator.naturalOrder()); // List.sort is a default method
        System.out.println("sorted: " + names);

        // Map.forEach() — default method
        Map<String, Integer> scores = Map.of("Alice", 95, "Bob", 82);
        scores.forEach((k, v) -> System.out.println(k + ": " + v));

        // Map.getOrDefault() — default method
        System.out.println("getOrDefault: " + scores.getOrDefault("Charlie", 0));

        // Map.putIfAbsent() — default method
        Map<String, Integer> mutable = new HashMap<>(scores);
        mutable.putIfAbsent("Charlie", 70);
        System.out.println("putIfAbsent      : " + mutable.get("Charlie"));

        // Iterable.forEach() — default method
        names.forEach(n -> System.out.print(n + " "));
        System.out.println();
    }

    // ── Mixin interfaces ──────────────────────────
    interface Auditable {
        Map<String, Object> getMetadata();

        default void markCreated() {
            getMetadata().put("createdAt", java.time.Instant.now().toString());
        }
        default void markUpdated() {
            getMetadata().put("updatedAt", java.time.Instant.now().toString());
        }
        default String getCreatedAt() {
            return (String) getMetadata().getOrDefault("createdAt", "unknown");
        }
        default String getUpdatedAt() {
            return (String) getMetadata().getOrDefault("updatedAt", "unknown");
        }
    }

    static class Order implements Auditable {
        String id; int amount;
        private final Map<String, Object> metadata = new HashMap<>();

        Order(String id, int amount) {
            this.id = id;
            this.amount = amount;
        }

        @Override
        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public String summary() {
            return "Order(" + id + ", ₹" + amount + ")";
        }
    }
}