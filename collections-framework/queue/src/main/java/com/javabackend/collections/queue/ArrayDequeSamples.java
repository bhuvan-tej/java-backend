package com.javabackend.collections.queue;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Queue;

/**
 *
 * ARRAYDEQUE
 *
 * What's it?
 * ArrayDeque is a resizable circular array implementing Deque.
 * It can be used as:
 *  - Stack (LIFO): push() / pop() / peek()
 *  - Queue (FIFO): offer() / poll() / peek()
 *  - Deque (Double-Ended Queue): add/remove from both ends
 *
 * WHY PREFER ARRAYDEQUE OVER STACK / LINKEDLIST?
 *  - Stack class extends Vector → synchronized overhead on every op (bad!)
 *  - LinkedList: O(1) but poor cache performance (scattered heap nodes)
 *  - ArrayDeque: O(1) amortized, contiguous memory = cache friendly = FAST
 *
 * Javadocs literally say: "This class is likely to be faster than Stack
 * when used as a stack, and faster than LinkedList when used as a queue."
 *
 * When to use?
 *  ✅ Stack implementation → ArrayDeque.push()/pop()
 *  ✅ Queue implementation → ArrayDeque.offer()/poll()
 *  ✅ BFS/DFS algorithms
 *  ✅ Monotonic deque problems (sliding window)
 *  ✅ Expression evaluation, bracket matching
 *
 */
public class ArrayDequeSamples {

    public static void main(String[] args) {
        System.out.println("--- Foundational ---");
        foundationalExample();

        System.out.println("\n--- Advanced Level ---");
        advLevelExample();
    }

    // Foundational
    // Bracket matching and basic stack/queue usage
    static void foundationalExample() {
        // AS A STACK (LIFO)
        System.out.println("ArrayDeque as Stack");
        Deque<String> callStack = new ArrayDeque<>();
        callStack.push("main()");       // addFirst internally
        callStack.push("loadData()");
        callStack.push("fetchFromDB()");
        callStack.push("executeQuery()");

        System.out.println("Call stack (top = most recent): " + callStack);
        System.out.println("Top of stack (peek): " + callStack.peek());

        // Unwinding the call stack
        System.out.println("Unwinding:");
        while (!callStack.isEmpty()) {
            System.out.println("  Returning from: " + callStack.pop());
        }

        // AS A QUEUE (FIFO)
        System.out.println("\n ArrayDeque as Queue");
        Deque<String> requestQueue = new ArrayDeque<>();
        requestQueue.offer("Request-1");  // offerLast internally
        requestQueue.offer("Request-2");
        requestQueue.offer("Request-3");

        System.out.println("Processing queue (FIFO):");
        while (!requestQueue.isEmpty()) {
            System.out.println("  Processing: " + requestQueue.poll()); // pollFirst
        }

        // BRACKET MATCHING PROBLEM
        System.out.println("\n--- Bracket Matching using Stack ---");
        String[] expressions = {
                "({[]})",    // Valid
                "([)]",      // Invalid
                "(((",       // Invalid
                "{[]()}",    // Valid
                ""           // Valid (empty)
        };

        for (String expr : expressions) {
            System.out.println("\"" + expr + "\" → " + (isValidBrackets(expr) ? "✅ Valid" : "❌ Invalid"));
        }
    }

    // ---- Bracket Matching ----
    static boolean isValidBrackets(String s) {
        Deque<Character> stack = new ArrayDeque<>();
        for (char c : s.toCharArray()) {
            if (c == '(' || c == '[' || c == '{') {
                stack.push(c);
            } else {
                if (stack.isEmpty()) return false;
                char top = stack.pop();
                if (c == ')' && top != '(') return false;
                if (c == ']' && top != '[') return false;
                if (c == '}' && top != '{') return false;
            }
        }
        return stack.isEmpty();
    }

    // ADV LEVEL
    // BFS using Queue, DFS using Stack, and expression evaluator
    static void advLevelExample() {
        // === BFS (Breadth-First Search) using Queue ===
        System.out.println("--- BFS Level-Order Traversal ---");
        //       1
        //      / \
        //     2   3
        //    / \   \
        //   4   5   6
        int[][] tree = {{1, 2}, {3, 4}, {5}, {}, {}, {}}; // Adjacency list (0-indexed)
        bfsLevelOrder(tree, 6);

        // === DFS (Depth-First Search) using Stack (iterative) ===
        System.out.println("\n--- DFS Iterative using Stack ---");
        dfsIterative(tree, 6);

        // === Monotonic Deque — Next Greater Element ===
        System.out.println("\n--- Next Greater Element (Monotonic Stack) ---");
        int[] nums = {4, 6, 3, 5, 7, 1, 2};
        int[] nextGreater = nextGreaterElement(nums);
        System.out.println("Input:         " + Arrays.toString(nums));
        System.out.println("Next Greater:  " + Arrays.toString(nextGreater));
        // -1 means no greater element to the right
    }

    // ---- BFS using Queue ----
    static void bfsLevelOrder(int[][] adj, int n) {
        Queue<Integer> queue = new ArrayDeque<>(); // ✅ Use ArrayDeque, NOT LinkedList!
        boolean[] visited = new boolean[n];

        queue.offer(0); // Start from node 0
        visited[0] = true;
        int level = 0;

        while (!queue.isEmpty()) {
            int size = queue.size(); // Process level by level
            System.out.print("Level " + level++ + ": ");
            for (int i = 0; i < size; i++) {
                int node = queue.poll();
                System.out.print((node + 1) + " "); // +1 for 1-indexed display
                for (int neighbor : adj[node]) {
                    if (!visited[neighbor]) {
                        visited[neighbor] = true;
                        queue.offer(neighbor);
                    }
                }
            }
            System.out.println();
        }
    }

    // ---- DFS using Stack (iterative — avoids recursion stack overflow) ----
    static void dfsIterative(int[][] adj, int n) {
        Deque<Integer> stack = new ArrayDeque<>();
        boolean[] visited = new boolean[n];

        stack.push(0);
        System.out.print("DFS order: ");

        while (!stack.isEmpty()) {
            int node = stack.pop();
            if (visited[node]) continue;
            visited[node] = true;
            System.out.print((node + 1) + " ");
            // Push neighbors in reverse so left child is processed first
            for (int i = adj[node].length - 1; i >= 0; i--) {
                if (!visited[adj[node][i]]) {
                    stack.push(adj[node][i]);
                }
            }
        }
        System.out.println();
    }

    // ---- Next Greater Element using Monotonic Stack ----
    // For each element, find the first element to its RIGHT that is greater
    // int[] nums = {4, 6, 3, 5, 7, 1, 2};
    static int[] nextGreaterElement(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        Arrays.fill(result, -1); // Default: no greater element
        // Monotonic DECREASING stack (stores indices)
        Deque<Integer> stack = new ArrayDeque<>();
        for (int i = 0; i < n; i++) {
            // While current element is greater than element at stack top
            // → current is the "next greater" for the stack top
            while (!stack.isEmpty() && nums[i] > nums[stack.peek()]) {
                int idx = stack.pop();
                result[idx] = nums[i]; // Found next greater!
            }
            stack.push(i);
        }
        // Elements left in stack have no greater element (remain -1)
        return result;
    }

}