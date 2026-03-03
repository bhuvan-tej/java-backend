package com.javabackend.collections.set;

import java.util.*;

/**
 *
 * HASHSET
 *
 * What's it?
 * HashSet is backed internally by a HashMap.
 * When you add "Apple" to a HashSet, internally it does:
 * map.put("Apple", PRESENT)  // PRESENT is a dummy Object constant
 * No order guaranteed. No duplicates.
 *
 * This means HashSet inherits all of HashMap's O(1) lookup performance.
 *
 * How it finds duplicates?
 *  Step 1: Compute hashCode() of the element
 *  Step 2: Find the bucket (array slot) = hashCode % arraySize
 *  Step 3: If bucket is empty → just store it (unique)
 *  Step 4: If bucket has elements → call equals() to check for actual duplicate
 *
 * If two objects are EQUAL (equals() returns true), they MUST have the same HASHCODE.
 * But two objects with same hashCode don't have to be equal (collision is OK).
 *
 * When to use?
 *  ✅ Need to eliminate duplicates
 *  ✅ Fast membership check (contains) — O(1)
 *  ✅ Set operations: union, intersection, difference
 *
 * When not to use?
 *  ❌ Need elements in sorted order → TreeSet
 *  ❌ Need insertion order preserved → LinkedHashSet
 *  ❌ Need to access by index → there's no get(index) in Set!
 *
 */
public class HashSetSamples {

    public static void main(String[] args) {
        System.out.println("======== FOUNDATIONAL ========\n");
        foundationalExample();

        System.out.println("\n======== ADV LEVEL ========\n");
        advLevelExample();

        System.out.println("\n======== HASHCODE/EQUALS DEMO ========\n");
        hashCodeEqualsDemo();
    }

    // Foundational
    // Scenario: Tracking unique tags on blog posts
    static void foundationalExample() {
        Set<String> tags = new HashSet<>();

        // add() — O(1), returns true if element is NEW, false if duplicate
        System.out.println("Added 'java': " + tags.add("java"));
        System.out.println("Added 'python': " + tags.add("python"));
        System.out.println("Added 'java' again: " + tags.add("java")); // false — duplicate

        tags.add("spring");
        tags.add("microservices");
        tags.add("docker");

        System.out.println("\nAll unique tags: " + tags);
        // Note: Order is NOT guaranteed in HashSet! Don't rely on it.

        // contains() — O(1), the main reason to use HashSet
        System.out.println("Has 'java' tag? " + tags.contains("java"));
        System.out.println("Has 'react' tag? " + tags.contains("react"));

        // remove() — O(1)
        tags.remove("docker");
        System.out.println("\nAfter removing 'docker': " + tags);

        // SET OPERATIONS — this is where Sets shine!
        Set<String> post1Tags = new HashSet<>(Arrays.asList("java", "spring", "microservices"));
        Set<String> post2Tags = new HashSet<>(Arrays.asList("java", "docker", "kubernetes"));

        // UNION — all tags from both posts
        Set<String> union = new HashSet<>(post1Tags);
        union.addAll(post2Tags);
        System.out.println("\nUnion (all tags): " + union);

        // INTERSECTION — tags common to both posts
        Set<String> intersection = new HashSet<>(post1Tags);
        intersection.retainAll(post2Tags);
        System.out.println("Intersection (common tags): " + intersection);

        // DIFFERENCE — tags in post1 but NOT in post2
        Set<String> difference = new HashSet<>(post1Tags);
        difference.removeAll(post2Tags);
        System.out.println("Difference (post1 exclusive tags): " + difference);

        // Check if one set is a subset of another
        Set<String> javaSet = new HashSet<>(Arrays.asList("java"));
        System.out.println("\nIs 'java' a subset of post1Tags? " + post1Tags.containsAll(javaSet));
    }

    // ADV LEVEL
    // Scenario: API permission system — check user permissions efficiently
    static void advLevelExample() {
        System.out.println("API Permission Checker\n");

        // Permissions available in the system
        Set<String> adminPermissions = new HashSet<>(Arrays.asList(
                "READ_USER", "WRITE_USER", "DELETE_USER",
                "READ_ORDER", "WRITE_ORDER", "DELETE_ORDER",
                "READ_REPORT", "MANAGE_SYSTEM"
        ));

        Set<String> userPermissions = new HashSet<>(Arrays.asList(
                "READ_USER", "READ_ORDER", "READ_REPORT"
        ));

        Set<String> orderManagerPermissions = new HashSet<>(Arrays.asList(
                "READ_ORDER", "WRITE_ORDER", "READ_USER"
        ));

        // Simulate checking if a user can perform an action
        String requestedPermission = "DELETE_USER";

        // O(1) check — this is why Set is perfect for permissions
        System.out.println("Admin can DELETE_USER? " + adminPermissions.contains(requestedPermission));
        System.out.println("User can DELETE_USER? " + userPermissions.contains(requestedPermission));

        // Check if user has ALL required permissions for an operation
        Set<String> requiredForExport = new HashSet<>(Arrays.asList("READ_USER", "READ_REPORT"));
        System.out.println("\nCan user export? " + userPermissions.containsAll(requiredForExport));
        System.out.println("Can order manager export? " + orderManagerPermissions.containsAll(requiredForExport));

        // Find what EXTRA permissions admin has over user
        Set<String> extraAdminPerms = new HashSet<>(adminPermissions);
        extraAdminPerms.removeAll(userPermissions);
        System.out.println("\nPermissions only admin has: " + extraAdminPerms);

        // Combining role permissions (role aggregation pattern)
        Set<String> combinedRoles = new HashSet<>();
        combinedRoles.addAll(userPermissions);
        combinedRoles.addAll(orderManagerPermissions);
        System.out.println("\nCombined user+orderManager permissions: " + combinedRoles);

        // Converting List to Set to remove duplicates — very common pattern
        List<String> permissionsFromDB = Arrays.asList(
                "READ_USER", "READ_ORDER", "READ_USER", "READ_ORDER", "READ_REPORT"
        );
        Set<String> uniquePerms = new HashSet<>(permissionsFromDB);
        System.out.println("\nUnique permissions from DB (was " + permissionsFromDB.size()
                + " rows, now " + uniquePerms.size() + "): " + uniquePerms);
    }

    // The hashCode/equals contract DEMO
    // This is where bugs hide for even experienced devs!
    static void hashCodeEqualsDemo() {
        System.out.println("\nUsing a class WITHOUT proper hashCode/equals");

        // BrokenEmployee doesn't override hashCode() or equals()
        // So Java uses default: identity comparison (memory address)
        Set<BrokenEmployee> brokenSet = new HashSet<>();
        brokenSet.add(new BrokenEmployee(101, "Alice"));
        brokenSet.add(new BrokenEmployee(101, "Alice")); // Should be duplicate — but isn't!
        System.out.println("Broken set size (expected 1, got): " + brokenSet.size()); // 2 — BUG!

        System.out.println("\n--- Using a class WITH proper hashCode/equals ---");

        Set<ProperEmployee> properSet = new HashSet<>();
        properSet.add(new ProperEmployee(101, "Alice"));
        properSet.add(new ProperEmployee(101, "Alice")); // Correctly detected as duplicate
        System.out.println("Proper set size (expected 1, got): " + properSet.size()); // 1 — CORRECT!

        // Also works correctly with contains()
        System.out.println("Contains Alice (id=101)? "
                + properSet.contains(new ProperEmployee(101, "Alice")));
    }

    // BAD — doesn't override hashCode/equals
    // Two objects with same data will be treated as DIFFERENT by HashSet
    static class BrokenEmployee {
        int id;
        String name;
        BrokenEmployee(int id, String name) {
            this.id = id;
            this.name = name;
        }
        // No hashCode() — uses Object's default (memory address based)
        // No equals()   — uses Object's default (reference equality ==)
    }

    // GOOD — properly overrides hashCode and equals
    static class ProperEmployee {
        int id;
        String name;

        ProperEmployee(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ProperEmployee)) return false;
            ProperEmployee other = (ProperEmployee) o;
            // Two employees are equal if they have the same ID
            return this.id == other.id;
        }

        @Override
        public int hashCode() {
            // Must be consistent with equals()!
            // If equals() uses id, hashCode() must also use id.
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "Employee{" + id + ", " + name + "}";
        }
    }

}