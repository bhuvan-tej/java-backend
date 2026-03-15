package com.javabackend.java17;

import java.util.*;
import java.util.stream.*;
import java.util.random.*;

/**
 *
 * Java 17 Features
 *
 * Java 17 (September 2021) — LTS release
 * Key theme: stabilization of preview features from 14-16,
 * strong encapsulation, security improvements
 *
 * FEATURES COVERED
 *   1. Sealed classes stable       — full production use
 *   2. Records stable              — production patterns
 *   3. Pattern matching stable     — instanceof, combined patterns
 *   4. Strong encapsulation        — JDK internals inaccessible
 *   5. Context restore             — what Java 17 LTS means in practice
 *
 * NOTE: Pattern matching switch is preview in Java 17.
 *       Full coverage is in java-21 where it stabilizes.
 *
 */
public class Features {

    static void main(String[] args) {
        System.out.println("━━━ EXAMPLE 1 — Sealed Classes (Stable) ━━━\n");
        sealedClassesStable();

        System.out.println("\n━━━ EXAMPLE 2 — Records (Stable) ━━━\n");
        recordsStable();

        System.out.println("\n━━━ EXAMPLE 3 — Pattern Matching (Stable) ━━━\n");
        patternMatchingStable();

        System.out.println("\n━━━ EXAMPLE 4 — Strong Encapsulation ━━━\n");
        strongEncapsulation();

        System.out.println("\n━━━ EXAMPLE 5 — Java 17 as LTS ━━━\n");
        java17AsLts();

        System.out.println("\n━━━ EXAMPLE 6 — RandomGenerator API ━━━\n");
        randomGeneratorApi();
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 1 — Sealed Classes Stable
    // Full production patterns
    // ─────────────────────────────────────────────

    // ── Domain model: HTTP Response hierarchy ──
    sealed interface HttpResponse permits HttpResponse.Ok, HttpResponse.ClientError, HttpResponse.ServerError {
        int statusCode();

        record Ok(int statusCode, String body)          implements HttpResponse {}
        record ClientError(int statusCode, String reason) implements HttpResponse {}
        record ServerError(int statusCode, String trace) implements HttpResponse {}

        // Static factories
        static HttpResponse ok(String body)            { return new Ok(200, body); }
        static HttpResponse notFound(String reason)    { return new ClientError(404, reason); }
        static HttpResponse serverError(String trace)  { return new ServerError(500, trace); }

        // Default method on sealed interface
        default boolean isSuccess() { return this instanceof Ok; }
        default boolean isError()   { return !isSuccess(); }
    }

    static void sealedClassesStable() {
        var responses = List.of(
                HttpResponse.ok("{'user':'Alice'}"),
                HttpResponse.notFound("User not found"),
                HttpResponse.serverError("NullPointerException at line 42")
        );

        for (var response : responses) {
            String log = handleResponse(response);
            System.out.println("  " + log);
        }

        // Filter by type
        long errors = responses.stream()
                .filter(HttpResponse::isError)
                .count();
        System.out.println("  error count: " + errors);

        // Sealed classes in Java 17 — fully stable
        // No --enable-preview flag needed
        System.out.println("  sealed classes: stable, no preview flag needed ✓");
    }

    static String handleResponse(HttpResponse response) {
        if (response instanceof HttpResponse.Ok ok)
            return "[200] body length: " + ok.body().length();
        if (response instanceof HttpResponse.ClientError ce)
            return "[" + ce.statusCode() + "] client error: " + ce.reason();
        if (response instanceof HttpResponse.ServerError se)
            return "[" + se.statusCode() + "] server error: " + se.trace().substring(0, 20) + "...";
        throw new AssertionError("unreachable — sealed");
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 2 — Records Stable
    // Advanced production patterns
    // ─────────────────────────────────────────────

    // ── Nested records ──
    record Address(String street, String city, String country) {}
    record User(String id, String name, String email, Address address) {
        // Compact constructor with validation
        User {
            Objects.requireNonNull(id,    "id cannot be null");
            Objects.requireNonNull(name,  "name cannot be null");
            Objects.requireNonNull(email, "email cannot be null");
            email = email.toLowerCase();
        }

        // Derived accessor
        String domain() {
            return email.substring(email.indexOf('@') + 1);
        }

        // Wither pattern — return new record with one field changed
        User withEmail(String newEmail) {
            return new User(id, name, newEmail, address);
        }

        User withAddress(Address newAddress) {
            return new User(id, name, email, newAddress);
        }
    }

    // ── Generic record ──
    record Pair<A, B>(A first, B second) {
        static <A, B> Pair<A, B> of(A a, B b) { return new Pair<>(a, b); }
        Pair<B, A> swap() { return new Pair<>(second, first); }
    }

    // ── Record implementing Comparable ──
    record Version(int major, int minor, int patch) implements Comparable<Version> {
        @Override
        public int compareTo(Version other) {
            if (major != other.major) return Integer.compare(major, other.major);
            if (minor != other.minor) return Integer.compare(minor, other.minor);
            return Integer.compare(patch, other.patch);
        }

        @Override public String toString() {
            return "%d.%d.%d".formatted(major, minor, patch);
        }
    }

    static void recordsStable() {
        // Nested records
        var address = new Address("123 Main St", "Mumbai", "India");
        var user    = new User("U1", "Alice", "ALICE@EXAMPLE.COM", address);
        System.out.println("  user       : " + user.name() + " <" + user.email() + ">");
        System.out.println("  domain     : " + user.domain());

        // Wither pattern — immutable update
        var updated = user.withEmail("alice.new@example.com");
        System.out.println("  updated    : " + updated.email());
        System.out.println("  original   : " + user.email()); // unchanged

        // Generic record
        var pair = Pair.of("Alice", 95);
        System.out.println("  pair       : " + pair);
        System.out.println("  swapped    : " + pair.swap());

        // Record with Comparable
        var versions = List.of(
                new Version(2, 1, 0),
                new Version(1, 9, 5),
                new Version(2, 0, 3),
                new Version(1, 9, 6)
        );
        var sorted = versions.stream().sorted().toList();
        System.out.println("  versions   : " + sorted);
        System.out.println("  latest     : " + sorted.getLast());
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 3 — Pattern Matching instanceof Stable
    // Combined patterns, guards, negation
    // ─────────────────────────────────────────────

    sealed interface Animal permits Animal.Dog, Animal.Cat, Animal.Bird {
        String name();
        record Dog(String name, String breed) implements Animal {}
        record Cat(String name, boolean indoor) implements Animal {}
        record Bird(String name, boolean canFly) implements Animal {}
    }

    static void patternMatchingStable() {
        var animals = List.of(
                new Animal.Dog("Rex", "German Shepherd"),
                new Animal.Cat("Whiskers", true),
                new Animal.Bird("Tweety", true),
                new Animal.Dog("Max", "Labrador"),
                new Animal.Bird("Penguin", false)
        );

        // ── Pattern with guard (&&) ──
        System.out.println("  flying birds:");
        animals.stream()
                .filter(a -> a instanceof Animal.Bird b && b.canFly())
                .forEach(a -> System.out.println("    " + a.name()));

        // ── Describe each animal ──
        System.out.println("  all animals:");
        animals.forEach(a -> System.out.println("    " + describeAnimal(a)));

        // ── Negation pattern ──
        Object obj = "hello";
        if (!(obj instanceof String s)) {
            return; // not a string
        }
        // s in scope here — after negation guard
        System.out.println("  negation: length=" + s.length());
    }

    static String describeAnimal(Animal a) {
        if (a instanceof Animal.Dog d)
            return "Dog: %s (%s)".formatted(d.name(), d.breed());
        if (a instanceof Animal.Cat c)
            return "Cat: %s (%s)".formatted(c.name(), c.indoor() ? "indoor" : "outdoor");
        if (a instanceof Animal.Bird b)
            return "Bird: %s (%s)".formatted(b.name(), b.canFly() ? "can fly" : "flightless");
        throw new AssertionError("unreachable");
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 4 — Strong Encapsulation of JDK Internals
    // ─────────────────────────────────────────────
    static void strongEncapsulation() {
        // Java 17 enforces strong encapsulation of JDK internal APIs
        // Previously: --illegal-access=permit was the default
        // Java 17:    --illegal-access=deny is the only option
        //             accessing sun.*, com.sun.*, jdk.internal.* throws InaccessibleObjectException

        // ── What this means in practice ──
        // Libraries using reflection to access private JDK internals break
        // Common offenders: older Spring, Hibernate, serialization libs,
        //                   byte-buddy, cglib, some Lombok versions

        System.out.println("  strong encapsulation: JDK internals inaccessible by default");
        System.out.println("  --illegal-access flag removed in Java 17");
        System.out.println("  --add-opens still works for specific module/package pairs");

        // ── Checking module accessibility ──
        Module javaBase = String.class.getModule();
        System.out.println("  java.base module  : " + javaBase.getName());
        System.out.println("  is named module   : " + javaBase.isNamed());

        // ── How to fix if your dependency breaks ──
        // Short-term: --add-opens java.base/java.lang=ALL-UNNAMED
        // Long-term:  upgrade the offending library to a Java 17 compatible version
        System.out.println("  fix: --add-opens or upgrade to Java 17 compatible library");

        // ── Why this matters ──
        // Security: prevents exploit via internal reflection
        // Stability: JDK internals can change — encapsulation protects users
        System.out.println("  benefit: security + stability of JDK internal changes");
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 5 — Java 17 as LTS
    // What stabilised, migration context
    // ─────────────────────────────────────────────
    static void java17AsLts() {
        System.out.println("  Java 17 — LTS (Long Term Support)");
        System.out.println("  Support until: September 2029 (Oracle)");
        System.out.println();

        // ── What became stable in Java 17 ──
        System.out.println("  Stable in Java 17:");
        System.out.println("  ✅ Sealed classes         (preview 15, 16 → stable 17)");
        System.out.println("  ✅ Pattern instanceof      (preview 14, 15 → stable 16)");
        System.out.println("  ✅ Records                 (preview 14, 15 → stable 16)");
        System.out.println("  ✅ Text blocks             (preview 13, 14 → stable 15)");
        System.out.println("  ✅ Switch expressions      (preview 12, 13 → stable 14)");
        System.out.println("  ✅ Strong encapsulation    (enforced 17)");
        System.out.println();

        // ── Still preview in Java 17 ──
        System.out.println("  Preview in Java 17 (stable in 21):");
        System.out.println("  🔜 Pattern matching switch");
        System.out.println("  🔜 Virtual threads");
        System.out.println("  🔜 Structured concurrency");
        System.out.println();

        // ── Migration from Java 11 → 17 ──
        System.out.println("  Java 11 → 17 migration checklist:");
        System.out.println("  □ Check libraries for Java 17 compatibility");
        System.out.println("  □ Replace deprecated SecurityManager usage");
        System.out.println("  □ Fix --illegal-access violations");
        System.out.println("  □ Update build tools (Maven 3.8+, Gradle 7.3+)");
        System.out.println("  □ Embrace records, sealed, text blocks, switch expressions");

        // ── Why Java 17 is the migration target ──
        // Most enterprises migrated from Java 8/11 to Java 17
        // Spring Boot 3.x requires Java 17 minimum
        // Jakarta EE 10 requires Java 17
        System.out.println();
        System.out.println("  Spring Boot 3.x minimum: Java 17");
        System.out.println("  Jakarta EE 10 minimum  : Java 17");
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 6 — RandomGenerator API (Java 17)
    // ─────────────────────────────────────────────
    static void randomGeneratorApi() {
        // ── Before Java 17 — scattered, inconsistent APIs ──
        // java.util.Random          — basic, thread-safe but slow under contention
        // java.util.SecureRandom    — cryptographically secure, slow
        // java.util.concurrent.ThreadLocalRandom — fast, thread-local
        // java.util.SplittableRandom — for parallel streams

        // ── Java 17 — unified RandomGenerator interface ──
        // All RNG implementations now implement RandomGenerator
        // Write code against the interface — swap algorithm without changing logic

        // ── Default generator ──
        RandomGenerator rng = RandomGenerator.getDefault();
        System.out.println("  default algo   : " + rng.getClass().getSimpleName());
        System.out.println("  nextInt(100)   : " + rng.nextInt(100));
        System.out.println("  nextDouble()   : " + String.format("%.4f", rng.nextDouble()));
        System.out.println("  nextBoolean()  : " + rng.nextBoolean());

        // ── Specific algorithm ──
        RandomGenerator xo = RandomGenerator.of("Xoshiro256PlusPlus");
        System.out.println("  Xoshiro256++   : " + xo.nextInt(100));

        RandomGenerator l64 = RandomGenerator.of("L64X128MixRandom");
        System.out.println("  L64X128Mix     : " + l64.nextInt(100));

        // ── RandomGeneratorFactory — discover available algorithms ──
        System.out.println("  available algorithms:");
        RandomGeneratorFactory.all()
                .map(RandomGeneratorFactory::name)
                .sorted()
                .forEach(name -> System.out.println("    " + name));

        // ── Find by properties ──
        // Find a statistically strong, jumpable generator
        RandomGeneratorFactory.all()
                .filter(f -> f.isJumpable() && f.stateBits() >= 128)
                .findFirst()
                .ifPresent(f -> System.out.println("  jumpable 128bit: " + f.name()));

        // ── Stream of random values ──
        List<Integer> randoms = rng.ints(5, 1, 100) // 5 values between 1-99
                .boxed()
                .toList();
        System.out.println("  ints stream    : " + randoms);

        // ── ThreadLocalRandom and SplittableRandom now implement RandomGenerator ──
        RandomGenerator tlr = java.util.concurrent.ThreadLocalRandom.current();
        System.out.println("  ThreadLocalRandom implements RandomGenerator: "
                + (tlr instanceof RandomGenerator));
    }

}