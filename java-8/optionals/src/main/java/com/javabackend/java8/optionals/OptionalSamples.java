package com.javabackend.java8.optionals;

import java.util.*;

/**
 *
 * Optional<T>
 *
 * Optional is a container that may or may not hold a value.
 * Its purpose: make the possibility of absence EXPLICIT in the API.
 *
 * USE Optional AS:
 *  ✅ Return type of methods that may not find a result
 *
 * DO NOT USE Optional AS:
 *  ❌ Field type in a class
 *  ❌ Method parameter
 *  ❌ Collection element
 *
 */
public class OptionalSamples {

    public static void main(String[] args) {
        System.out.println("━━━ EXAMPLE 1 — Creating Optionals ━━━\n");
        creating();

        System.out.println("\n━━━ EXAMPLE 2 — Retrieving Values ━━━\n");
        retrieving();

        System.out.println("\n━━━ EXAMPLE 3 — Conditional Operations ━━━\n");
        conditionalOps();

        System.out.println("\n━━━ EXAMPLE 4 — Anti-Patterns ━━━\n");
        antiPatterns();

        System.out.println("\n━━━ EXAMPLE 5 — Adv Level ━━━\n");
        advLevel();
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 1 — Creating
    // ─────────────────────────────────────────────
    static void creating() {
        // of — value guaranteed non-null, throws NPE if null passed
        Optional<String> name = Optional.of("Alice");
        System.out.println("of              : " + name);

        // ofNullable — safe wrapper, handles null
        String maybeNull = null;
        Optional<String> safe = Optional.ofNullable(maybeNull);
        System.out.println("ofNullable(null): " + safe);

        Optional<String> notNull = Optional.ofNullable("Bob");
        System.out.println("ofNullable(val) : " + notNull);

        // empty — explicitly absent
        Optional<String> empty = Optional.empty();
        System.out.println("empty           : " + empty);
        System.out.println("isEmpty         : " + empty.isPresent());
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 2 — Retrieving Values
    // ─────────────────────────────────────────────
    static void retrieving() {
        Optional<String> present = Optional.of("Alice");
        Optional<String> absent  = Optional.empty();

        // orElse — always evaluates fallback (even if present)
        System.out.println("orElse present  : " + present.orElse("default"));
        System.out.println("orElse absent   : " + absent.orElse("default"));

        // orElseGet — lazy, only evaluates supplier if absent
        System.out.println("orElseGet absent: " + absent.orElseGet(() -> "computed"));

        // orElseThrow — throw if absent (Java 10: no-arg version)
        System.out.println("orElseThrow     : " + present.orElseThrow(NoSuchElementException::new));
        try {
            absent.orElseThrow(() -> new RuntimeException("not found"));
        } catch (RuntimeException e) {
            System.out.println("orElseThrow ex  : " + e.getMessage());
        }

        // get — ONLY use after checking isPresent, otherwise NoSuchElementException
        if (present.isPresent()) {
            System.out.println("get (guarded)   : " + present.get());
        }
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 3 — Conditional Operations
    // ─────────────────────────────────────────────
    static void conditionalOps() {
        Optional<User> user = findUser("U1");

        // ifPresent — execute only if value exists
        user.ifPresent(u -> System.out.println("ifPresent       : " + u.name));

        // ifPresentOrElse
        if (user.isPresent()) {
            System.out.println("ifPresentOrElse : found " + user.get().name);
        } else {
            System.out.println("ifPresentOrElse : not found");
        }

        // filter — keep value only if it passes predicate
        Optional<User> active = user.filter(u -> u.active);
        System.out.println("filter active   : " + active.isPresent());

        // map — transform value if present
        Optional<String> email = user.map(u -> u.email);
        System.out.println("map to email    : " + email.orElse("no email"));

        // flatMap — when mapping returns another Optional (avoids Optional<Optional<T>>)
        Optional<String> domain = user
                .map(u -> u.email)
                .flatMap(OptionalSamples::extractDomain);
        System.out.println("flatMap domain  : " + domain.orElse("no domain"));

        // supply fallback Optional
        Optional<User> fallback = findUser("MISSING")
                .or(() -> findUser("U2"));
        fallback.ifPresent(u -> System.out.println("or fallback     : " + u.name));
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 4 — Anti-Patterns
    // ─────────────────────────────────────────────
    static void antiPatterns() {

        // ❌ isPresent + get — defeats the purpose of Optional
        Optional<User> user = findUser("U1");
        if (user.isPresent()) {
            System.out.println("BAD isPresent+get: " + user.get().name);
        }
        // ✅ use ifPresent, map, or orElse instead
        user.ifPresent(u -> System.out.println("GOOD ifPresent  : " + u.name));

        // ❌ orElse with expensive computation — always runs even if present
        String result = user.map(u -> u.name)
                .orElse(computeExpensive()); // computed even when user present!
        // ✅ orElseGet — lazy, only runs if absent
        String lazy = user.map(u -> u.name)
                .orElseGet(() -> computeExpensive());
        System.out.println("orElseGet lazy  : " + lazy);

        // ❌ Optional.of(null) — throws NPE immediately
        try {
            Optional.of(null);
        } catch (NullPointerException e) {
            System.out.println("of(null)        : NullPointerException ✓");
        }
        // ✅ Optional.ofNullable(null) — safe
        Optional<String> safe = Optional.ofNullable(null);
        System.out.println("ofNullable safe : " + safe.isPresent());
    }

    // ─────────────────────────────────────────────
    // EXAMPLE 5 — Adv Level
    //   Optional chaining in a service layer
    // ─────────────────────────────────────────────
    static void advLevel() {

        // Chain of operations — each step returns Optional
        // No null checks anywhere
        String greeting = findUser("U1")
                .filter(u -> u.active)
                .map(u -> u.email)
                .flatMap(OptionalSamples::extractDomain)
                .map(domain -> "Hello from " + domain)
                .orElse("Hello, guest");
        System.out.println("chain result    : " + greeting);

        // Missing user — chain short-circuits cleanly
        String missing = findUser("MISSING")
                .filter(u -> u.active)
                .map(u -> u.email)
                .flatMap(OptionalSamples::extractDomain)
                .map(domain -> "Hello from " + domain)
                .orElse("Hello, guest");
        System.out.println("missing chain   : " + missing);

        // Converting Optional to Stream
        // Useful in stream pipelines to unwrap Optionals
        List<String> ids = Arrays.asList("U1", "MISSING", "U2", "MISSING");
        List<String> names = ids.stream()
                .map(OptionalSamples::findUser)   // Stream<Optional<User>>
                .flatMap(Optional::stream)         // unwrap — only present values
                .map(u -> u.name)
                .toList();
        System.out.println("flatMap stream  : " + names);
    }

    // ── Helpers ──
    static Optional<User> findUser(String id) {
        Map<String, User> db = Map.of(
                "U1", new User("Alice", "alice@example.com", true),
                "U2", new User("Bob",   "bob@company.org",   true)
        );
        return Optional.ofNullable(db.get(id));
    }

    static Optional<String> extractDomain(String email) {
        if (email == null || !email.contains("@")) return Optional.empty();
        return Optional.of(email.substring(email.indexOf('@') + 1));
    }

    static String computeExpensive() {
        System.out.print("[expensive!] ");
        return "fallback";
    }

    // ── Model ──
    static class User {
        String name, email;
        boolean active;
        User(String name, String email, boolean active) {
            this.name = name; this.email = email; this.active = active;
        }
    }

}