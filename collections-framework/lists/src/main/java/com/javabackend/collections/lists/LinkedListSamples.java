package com.javabackend.collections.lists;

import java.util.*;

/**
 *
 * LINKEDLIST — Deep Dive
 *
 * WHAT IS IT?
 * A doubly-linked list where each Node holds:
 *   - data (element)
 *   - reference to NEXT node
 *   - reference to PREVIOUS node
 *
 * Java's LinkedList also implements Deque, so it works as:
 *   - List  → ordered, indexed access (slow O(n))
 *   - Queue → FIFO via offer()/poll()
 *   - Stack → LIFO via push()/pop()
 *
 * WHEN TO USE?
 *   ✅ Frequent insertions/deletions at HEAD or TAIL — O(1)
 *   ✅ Implementing a Queue or Stack in a single structure
 *   ✅ When you don't need random index access
 *
 * WHEN NOT TO USE?
 *   ❌ Random access by index (get(5)) → O(n), unlike ArrayList's O(1)
 *   ❌ Memory-sensitive apps — each node has 2 extra pointers (prev, next)
 *   ❌ Cache performance matters — nodes are scattered in heap (not contiguous)
 *
 * REAL TRUTH (5yr perspective):
 *   In modern Java, ArrayDeque is almost always faster than LinkedList
 *   when used as a Queue/Stack because ArrayDeque is array-backed (cache friendly).
 *   Use LinkedList when you specifically need List + Deque in one object.
 *
 */
public class LinkedListSamples {

    public static void main(String[] args) {

        System.out.println("======== FOUNDATIONAL ========\n");
        foundationalExample();

        System.out.println("\n======== ADV LEVEL ========\n");
        seniorLevelExample();

    }

    // Foundational example: Using LinkedList as both a Stack and a Queue
    // Scenario: Browser history navigation (Back/Forward)
    static void foundationalExample() {
        // LinkedList declared as Deque — this is the idiomatic way
        // when you need stack/queue behavior
        Deque<String> browserHistory = new LinkedList<>();

        // addFirst() / push() — adds to HEAD, O(1)
        // Think of this like opening a new tab on top of your stack
        browserHistory.push("google.com");
        browserHistory.push("github.com");
        browserHistory.push("stackoverflow.com");
        // Stack (top → bottom): stackoverflow → github → google
        System.out.println("Current page (top of stack): " + browserHistory.peek());

        // pop() / removeFirst() — removes from HEAD, O(1)
        String currentPage = browserHistory.pop();
        System.out.println("Going back from: " + currentPage);
        System.out.println("Now on: " + browserHistory.peek());
        System.out.println("Full history (from top): " + browserHistory);

        // Now use it as a Queue (FIFO) — different behavior!
        Queue<String> printQueue = new LinkedList<>();
        printQueue.offer("Document1.pdf");  // offer = add to TAIL
        printQueue.offer("Report.pdf");
        printQueue.offer("Invoice.pdf");

        System.out.println("\nPrint queue: " + printQueue);
        // poll() — removes from HEAD (first in, first out)
        System.out.println("Printing: " + printQueue.poll());
        System.out.println("Printing: " + printQueue.poll());
        System.out.println("Remaining: " + printQueue);
    }

    // Undo/Redo Text Editor implementation
    static class TextEditor {

        private StringBuilder content = new StringBuilder();

        // Using Deque (backed by LinkedList) for O(1) push/pop at head
        // ArrayDeque would also work here and would actually be faster,
        // but LinkedList is shown to demonstrate its Deque capability
        private final Deque<String> undoStack = new LinkedList<>();
        private final Deque<String> redoStack = new LinkedList<>();

        /**
         * Type text — saves snapshot BEFORE change for undo capability
         */
        void type(String text) {
            // Save current state before modifying
            undoStack.push(content.toString());
            // Any new action clears the redo history (standard editor behavior)
            redoStack.clear();
            // Apply the change
            content.append(text);
            System.out.println("Typed: '" + text + "'");
        }

        /**
         * Undo — restores previous state, saves current to redo stack
         */
        void undo() {
            if (undoStack.isEmpty()) {
                System.out.println("Nothing to undo!");
                return;
            }
            // Before undoing, save current state for possible redo
            redoStack.push(content.toString());
            // Restore previous state from undo stack
            String previousState = undoStack.pop();
            content = new StringBuilder(previousState);
            System.out.println("Undone. Restored to: '" + content + "'");
        }

        /**
         * Redo — reapplies an undone action
         */
        void redo() {
            if (redoStack.isEmpty()) {
                System.out.println("Nothing to redo!");
                return;
            }
            // Before redoing, save current for undo
            undoStack.push(content.toString());
            // Restore redo state
            String redoState = redoStack.pop();
            content = new StringBuilder(redoState);
            System.out.println("Redone. Content: '" + content + "'");
        }

        boolean canRedo() { return !redoStack.isEmpty(); }

        void printContent() {
            System.out.println("Current content: '" + content + "'");
        }

    }

    // Scenario: Implementing an Undo/Redo system for a text editor
    // This is a classic LinkedList use case because:
    //   - O(1) push to head (new action)
    //   - O(1) pop from head (undo)
    //   - Two stacks pattern: undoStack + redoStack
    static void seniorLevelExample() {
        TextEditor editor = new TextEditor();

        // Simulate user typing and editing
        editor.type("Hello");
        editor.type(" World");
        editor.type("!");
        editor.printContent();

        System.out.println("\n--- User presses Ctrl+Z (Undo) ---");
        editor.undo();
        editor.printContent();

        System.out.println("\n--- User presses Ctrl+Z again ---");
        editor.undo();
        editor.printContent();

        System.out.println("\n--- User presses Ctrl+Y (Redo) ---");
        editor.redo();
        editor.printContent();

        System.out.println("\n--- User types new text (clears redo stack) ---");
        editor.type(" Java");
        editor.printContent();

        System.out.println("\n--- Redo is now cleared (new action invalidates redo) ---");
        System.out.println("Can redo? " + editor.canRedo());

        System.out.println("\n--- Multiple undo's ---");
        editor.undo();
        editor.undo();
        editor.printContent();

        // Demonstrate LinkedList specific methods
        System.out.println("\n\n--- LinkedList as both List and Deque demo ---");
        LinkedList<Integer> numbers = new LinkedList<>(Arrays.asList(1, 2, 3, 4, 5));

        // Deque operations — O(1)
        numbers.addFirst(0); // Add to head
        numbers.addLast(6);  // Add to tail
        System.out.println("After addFirst(0) and addLast(6): " + numbers);

        // List operations — O(n) for index access (traverses from nearest end)
        System.out.println("Element at index 3: " + numbers.get(3));

        // Peeking without removing
        System.out.println("First element (peekFirst): " + numbers.peekFirst());
        System.out.println("Last element (peekLast): " + numbers.peekLast());

        // descendingIterator — iterate from tail to head
        System.out.print("Reverse order: ");
        Iterator<Integer> descIter = numbers.descendingIterator();
        while (descIter.hasNext()) {
            System.out.print(descIter.next() + " ");
        }
    }

}