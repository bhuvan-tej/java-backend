package com.javabackend.collections.iterator;

import java.util.*;

/**
 *
 * ITERATOR
 *
 * What's it?
 *  Iterator is the standard way to traverse any Collection.
 *  Every Collection implements Iterable, which means it can produce an Iterator via iterator().
 *
 * The for-each loop is just syntactic sugar for Iterator!
 * for (String s : list) { ... }
 * is compiled to:
 * Iterator<String> it = list.iterator();
 * while (it.hasNext()) { String s = it.next(); ... }
 *
 * ITERATOR types:
 *  - Iterator<E>       → forward only, all Collections
 *  - ListIterator<E>   → bidirectional, Lists only (can go back!)
 *
 * FAIL-FAST vs FAIL-SAFE:
 *  FAIL-FAST (most Java collections — ArrayList, HashMap, etc.):
 *   - Tracks a 'modCount' counter that increments on every structural change
 *   - Iterator saves expectedModCount at creation time
 *   - On every next(), checks: if modCount != expectedModCount → THROW ConcurrentModificationException
 *   - This prevents subtle bugs from modifying while iterating
 *   - Does NOT guarantee it will ALWAYS throw — it's "best effort"
 *
 *  FAIL-SAFE (java.util.concurrent collections):
 *   - CopyOnWriteArrayList, ConcurrentHashMap
 *   - Iterate over a COPY or snapshot of data → no exception
 *   - Trade-off: may not see latest updates during iteration
 *
 */
public class IteratorSamples {

    public static void main(String[] args) {
        System.out.println("======== FOUNDATIONAL ========\n");
        foundationalExample();

        System.out.println("\n======== ADV LEVEL ========\n");
        advLevelExample();

        System.out.println("\n======== FAIL-FAST vs FAIL-SAFE ========\n");
        failFastDemo();
    }


    // Foundational
    // Basic Iterator and ListIterator usage
    static void foundationalExample() {
        List<String> cities = new ArrayList<>(
                Arrays.asList("Mumbai", "Delhi", "Chennai", "Kolkata", "Bengaluru")
        );

        // Basic Iterator — forward traversal
        System.out.println("Forward with Iterator");
        Iterator<String> it = cities.iterator();
        while (it.hasNext()) {
            String city = it.next();
            System.out.print(city + " ");
        }
        System.out.println();

        // Safe removal using Iterator.remove()
        // This is the ONLY safe way to remove while iterating!
        System.out.println("\n Safe removal: cities with less than 6 chars");
        Iterator<String> removeIt = cities.iterator();
        while (removeIt.hasNext()) {
            String city = removeIt.next();
            if (city.length() < 6) {
                removeIt.remove(); // Safe! Doesn't throw ConcurrentModificationException
            }
        }
        System.out.println("Remaining cities: " + cities);

        // Reset for ListIterator demo
        cities = new ArrayList<>(Arrays.asList("Mumbai", "Delhi", "Chennai", "Kolkata", "Bengaluru"));

        // ListIterator — bidirectional, only for Lists
        System.out.println("\nBidirectional with ListIterator");
        ListIterator<String> lit = cities.listIterator(cities.size()); // Start from end
        System.out.print("Reverse: ");
        while (lit.hasPrevious()) {
            System.out.print(lit.previous() + " ");
        }
        System.out.println();

        // ListIterator.add() and set() — powerful operations
        System.out.println("\nListIterator: modify while iterating");
        ListIterator<String> modIt = cities.listIterator();
        while (modIt.hasNext()) {
            String city = modIt.next();
            // Replace each city name with uppercase
            modIt.set(city.toUpperCase()); // set() replaces last returned element
        }
        System.out.println("After uppercasing: " + cities);

        // Add element during iteration
        ListIterator<String> addIt = cities.listIterator();
        while (addIt.hasNext()) {
            String city = addIt.next();
            if (city.equals("DELHI")) {
                addIt.add("JAIPUR"); // Inserts AFTER current element
            }
        }
        System.out.println("After adding JAIPUR after DELHI: " + cities);
    }

    // ADV LEVEL
    // Custom Iterable + Iterator implementation
    // This shows you deeply understand the pattern
    static void advLevelExample() {
        System.out.println("Custom Range Iterable\n");

        // Use our custom Range in a for-each loop!
        Range range = new Range(1, 10, 2); // 1, 3, 5, 7, 9 (step=2)
        System.out.print("Range(1,10,step=2): ");
        for (int val : range) {
            System.out.print(val + " ");
        }
        System.out.println();

        // Different range
        System.out.print("Range(0,100,step=10): ");
        for (int val : new Range(0, 100, 10)) {
            System.out.print(val + " ");
        }
        System.out.println();

        // Iterator for a binary tree (in-order traversal)
        System.out.println("\nCustom Tree Inorder Iterator\n");
        // Build a simple BST: 5, 3, 7, 1, 4, 6, 8
        //       5
        //      / \
        //     3   7
        //    / \ / \
        //   1  4 6  8
        TreeNode root = new TreeNode(5);
        root.left = new TreeNode(3);
        root.right = new TreeNode(7);
        root.left.left = new TreeNode(1);
        root.left.right = new TreeNode(4);
        root.right.left = new TreeNode(6);
        root.right.right = new TreeNode(8);

        BSTInorderIterator bstIt = new BSTInorderIterator(root);
        System.out.print("BST in-order traversal (should be sorted): ");
        while (bstIt.hasNext()) {
            System.out.print(bstIt.next() + " ");
        }
        System.out.println();
    }

    /**
     * Custom Iterable — represents an arithmetic range
     * Allows: for (int i : new Range(1, 10, 2)) { ... }
     */
    static class Range implements Iterable<Integer> {
        private final int start, end, step;

        Range(int start, int end, int step) {
            this.start = start;
            this.end = end;
            this.step = step;
        }

        @Override
        public Iterator<Integer> iterator() {
            return new Iterator<Integer>() {
                private int current = start;

                @Override
                public boolean hasNext() {
                    return current < end;
                }

                @Override
                public Integer next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    int val = current;
                    current += step;
                    return val;
                }
            };
        }
    }

    // Simple BST node
    static class TreeNode {
        int val;
        TreeNode left, right;
        TreeNode(int val) {
            this.val = val;
        }
    }

    /**
     * In-order iterator for BST using explicit stack
     * (avoids recursion, memory-efficient for large trees)
     */
    static class BSTInorderIterator implements Iterator<Integer> {
        private final Deque<TreeNode> stack = new ArrayDeque<>();

        BSTInorderIterator(TreeNode root) {
            pushLeft(root); // Push all left nodes from root
        }

        private void pushLeft(TreeNode node) {
            while (node != null) {
                stack.push(node);
                node = node.left;
            }
        }

        @Override
        public boolean hasNext() {
            return !stack.isEmpty();
        }

        @Override
        public Integer next() {
            TreeNode node = stack.pop();
            int val = node.val;
            // After visiting this node, push all left nodes of right subtree
            pushLeft(node.right);
            return val;
        }
    }

    // FAIL-FAST vs FAIL-SAFE
    static void failFastDemo() {
        List<String> list = new ArrayList<>(Arrays.asList("A", "B", "C", "D", "E"));

        // ❌ WRONG — this WILL throw ConcurrentModificationException
        System.out.println("--- Fail-Fast Demo ---");
        try {
            for (String s : list) {
                if (s.equals("C")) {
                    list.remove(s); // ❌ Modifying while for-each is running!
                }
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("Caught ConcurrentModificationException! (as expected)");
        }

        // ✅ CORRECT — use Iterator.remove()
        list = new ArrayList<>(Arrays.asList("A", "B", "C", "D", "E"));
        Iterator<String> safeIt = list.iterator();
        while (safeIt.hasNext()) {
            if (safeIt.next().equals("C")) {
                safeIt.remove(); // ✅ Safe — Iterator tracks its own state
            }
        }
        System.out.println("After safe removal of C: " + list);

        // ✅ ALSO CORRECT — Java 8+ removeIf (internally uses Iterator)
        list = new ArrayList<>(Arrays.asList("A", "B", "C", "D", "E"));
        list.removeIf(s -> s.equals("C"));
        System.out.println("After removeIf C: " + list);

        // FAIL-SAFE: CopyOnWriteArrayList — no exception but sees snapshot
        System.out.println("\n--- Fail-Safe with CopyOnWriteArrayList ---");
        List<String> cowList = new java.util.concurrent.CopyOnWriteArrayList<>(
                Arrays.asList("A", "B", "C", "D", "E")
        );
        System.out.print("Iterating CopyOnWriteArrayList (modifying during): ");
        for (String s : cowList) {
            System.out.print(s + " ");
            cowList.add("X"); // No exception, but iterator uses snapshot → won't see X
        }
        System.out.println();
        System.out.println("List size after: " + cowList.size() + " (added multiple X's)");
    }

}