package com.javabackend.collections.queue;

import java.util.*;

/**
 *
 * PRIORITYQUEUE
 *
 * What's it?
 * PriorityQueue is a MIN-HEAP by default.
 * peek() and poll() always return the SMALLEST element.
 * Elements are NOT stored in sorted order — only the root (min) is guaranteed.
 *
 * Internal structure:
 *  - Binary heap stored in an array
 *  - Parent at index i → children at 2i+1 and 2i+2
 *  - Parent is always <= children (min-heap property)
 *  - Add/Remove: O(log n) — needs to "sift up/down" to restore heap property
 *  - Peek (min): O(1) — root is always minimum
 *
 * When to use?
 *  ✅ Always need to process the SMALLEST (or LARGEST) element next
 *  ✅ Task scheduling by priority
 *  ✅ Dijkstra's shortest path
 *  ✅ Top-K problems ("find K largest elements")
 *  ✅ Merge K sorted arrays
 *
 * Common mistakes:
 *  ❌ Iterating over PriorityQueue doesn't give sorted order!
 *     (Only peek/poll is guaranteed to be min/max)
 *  ❌ Forgetting it's a MIN-heap — for MAX, use Collections.reverseOrder()
 *
 */
public class PriorityQueueSamples {

    public static void main(String[] args) {
        System.out.println("--- Foundational ---");
        foundationalExample();

        System.out.println("\n--- Advanced Level ---");
        advLevelExample();
    }

    static class Patient {

        String name;
        int severity; // 1=critical, 5=minor

        Patient(String name, int severity) {
            this.name = name;
            this.severity = severity;
        }
        @Override
        public String toString() {
            return name + "(sev:" + severity + ")";
        }
    }

    // Foundational
    // Scenario: Hospital emergency room triage system
    static void foundationalExample() {
        // MIN-HEAP — default behavior
        // Smaller number = higher priority in our ER system (1=critical, 5=minor)
        PriorityQueue<Patient> erQueue = new PriorityQueue<>(
                Comparator.comparingInt(p -> p.severity)
        );

        erQueue.offer(new Patient("John", 3));    // Moderate
        erQueue.offer(new Patient("Sarah", 1));   // Critical
        erQueue.offer(new Patient("Mike", 5));    // Minor
        erQueue.offer(new Patient("Lisa", 2));    // Serious
        erQueue.offer(new Patient("Tom", 1));     // Critical (same as Sarah)

        System.out.println("ER Queue size: " + erQueue.size());

        // peek() — see next patient WITHOUT removing, O(1)
        System.out.println("Next to be seen (peek): " + erQueue.peek());

        // poll() — treat patients in priority order, O(log n) each
        System.out.println("\nTreating patients in priority order:");
        while (!erQueue.isEmpty()) {
            Patient p = erQueue.poll();
            System.out.printf("  Treating: %s (severity: %d)%n", p.name, p.severity);
        }

        System.out.println("\nBasic Integer PriorityQueue");
        // Default MIN-heap with integers
        PriorityQueue<Integer> minHeap = new PriorityQueue<>();
        minHeap.addAll(Arrays.asList(5, 1, 8, 3, 2, 9, 4));
        System.out.print("Polling from min-heap (ascending order): ");
        while (!minHeap.isEmpty()) {
            System.out.print(minHeap.poll() + " ");
        }
        System.out.println();

        // MAX-heap — use Collections.reverseOrder()
        PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
        maxHeap.addAll(Arrays.asList(5, 1, 8, 3, 2, 9, 4));
        System.out.print("Polling from max-heap (descending order): ");
        while (!maxHeap.isEmpty()) {
            System.out.print(maxHeap.poll() + " ");
        }
        System.out.println();
    }

    static class Task {

        String name;
        int priority; // 1=urgent, 5=low
        long deadline;

        Task(String name, int priority, long deadline) {
            this.name = name;
            this.priority = priority;
            this.deadline = deadline;
        }
        long getDeadline() {
            return deadline;
        }

    }

    // ADV LEVEL
    // Scenario: Top-K problems and Merge K sorted arrays
    //           These are classic interview problems in senior roles
    static void advLevelExample() {

        // PROBLEM 1: Find K Largest Elements
        System.out.println("Top-K Problems\n");

        int[] sales = {45000, 12000, 87000, 33000, 92000, 5000, 67000, 28000, 55000, 41000};
        int k = 3;

        // APPROACH: Use a MIN-heap of size K
        // Why min-heap? Because we want to KICK OUT the smallest when heap is full.
        // After processing all elements, heap contains the K largest.
        PriorityQueue<Integer> topKHeap = new PriorityQueue<>(k); // min-heap

        for (int sale : sales) {
            topKHeap.offer(sale);
            if (topKHeap.size() > k) {
                topKHeap.poll(); // Remove the smallest — keeps only top K
            }
        }
        System.out.println("All sales: " + Arrays.toString(sales));
        System.out.println("Top " + k + " sales figures: " + topKHeap);
        // Note: topKHeap is a heap, not sorted. To get sorted, keep polling.
        List<Integer> topKSorted = new ArrayList<>();
        while (!topKHeap.isEmpty()) topKSorted.add(0, topKHeap.poll()); // poll gives ascending, add to front
        System.out.println("Top " + k + " sorted (highest first): " + topKSorted);

        // PROBLEM 2: Find K Smallest Elements
        // Use MAX-heap of size K, kick out largest when overflow
        PriorityQueue<Integer> kSmallestHeap = new PriorityQueue<>(Collections.reverseOrder());
        for (int sale : sales) {
            kSmallestHeap.offer(sale);
            if (kSmallestHeap.size() > k) {
                kSmallestHeap.poll(); // Remove the largest — keeps only K smallest
            }
        }
        System.out.println("Bottom " + k + " sales (smallest): " + kSmallestHeap);

        // PROBLEM 3: Merge K Sorted Arrays
        System.out.println("\nMerge K Sorted Arrays\n");

        // Classic problem — merge multiple sorted lists into one sorted list
        // Naive: merge one by one → O(n * k)
        // Optimal: min-heap → O(n log k) where n=total elements, k=number of lists
        int[][] sortedArrays = {
                {1, 5, 9, 13},
                {2, 6, 10, 14},
                {3, 7, 11, 15},
                {4, 8, 12, 16}
        };

        // Each heap entry: [value, arrayIndex, elementIndex]
        // Heap ordered by value
        PriorityQueue<int[]> mergeHeap = new PriorityQueue<>(
                Comparator.comparingInt(entry -> entry[0])
        );

        // Initialize heap with the FIRST element from each array
        for (int i = 0; i < sortedArrays.length; i++) {
            mergeHeap.offer(new int[]{sortedArrays[i][0], i, 0});
        }

        List<Integer> merged = new ArrayList<>();
        while (!mergeHeap.isEmpty()) {
            int[] smallest = mergeHeap.poll(); // Extract min
            int value = smallest[0];
            int arrayIdx = smallest[1];
            int elemIdx = smallest[2];

            merged.add(value);

            // If there are more elements in this array, add the next one to heap
            if (elemIdx + 1 < sortedArrays[arrayIdx].length) {
                mergeHeap.offer(new int[]{
                        sortedArrays[arrayIdx][elemIdx + 1], arrayIdx, elemIdx + 1
                });
            }
        }

        System.out.println("Input arrays:");
        for (int[] arr : sortedArrays) System.out.println("  " + Arrays.toString(arr));
        System.out.println("Merged sorted result: " + merged);

        // PROBLEM 4: Task Scheduler with Deadlines
        System.out.println("\n Task Scheduler by Deadline\n");

        PriorityQueue<Task> taskQueue = new PriorityQueue<>(
                // Primary: deadline ascending, Secondary: priority descending
                Comparator.comparingLong(Task::getDeadline)
                        .thenComparingInt(t -> -t.priority)
        );

        taskQueue.offer(new Task("Deploy Hotfix", 1, System.currentTimeMillis() + 1000));
        taskQueue.offer(new Task("Write Tests", 3, System.currentTimeMillis() + 5000));
        taskQueue.offer(new Task("Code Review", 2, System.currentTimeMillis() + 3000));
        taskQueue.offer(new Task("Update Docs", 4, System.currentTimeMillis() + 7000));
        taskQueue.offer(new Task("DB Migration", 1, System.currentTimeMillis() + 2000));

        System.out.println("Executing tasks by deadline:");
        int order = 1;
        while (!taskQueue.isEmpty()) {
            Task t = taskQueue.poll();
            System.out.printf("  %d. %s (priority: %d)%n", order++, t.name, t.priority);
        }
    }

}