package com.javabackend.collections.utils;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * java.util.Collections (utility class)
 * MODULE: utils
 *
 * ALL static methods — operates ON collection instances.
 *
 * Key categories:
 *   Sorting              — sort, reverseOrder, shuffle
 *   Searching            — binarySearch, min, max, frequency
 *   Modification         — reverse, rotate, swap, fill, copy, replaceAll
 *   Wrappers             — unmodifiableX, synchronizedX
 *   Factories            — nCopies, singletonList, emptyList
 *   Bulk                 — fill, copy, swap, disjoint
 *
 */
public class CollectionsUtilSamples {

    public static void main(String[] args) {
        System.out.println("━━━ Sorting & Searching ━━━\n");
        sortingAndSearching();

        System.out.println("\n━━━ Modification ━━━\n");
        modification();

        System.out.println("\n━━━ Wrapping & Defensive Copies ━━━\n");
        wrapping();

        System.out.println("\n━━━ Adv Level (Production Patterns) ━━━\n");
        advLevel();
    }

    // Sorting & Searching
    static void sortingAndSearching() {
        List<Integer> nums = new ArrayList<>(
                Arrays.asList(5, 2, 8, 1, 9, 3, 7, 4, 6));

        // sort — natural order (uses TimSort, stable)
        Collections.sort(nums);
        System.out.println("Sorted asc           : " + nums);

        // sort with Comparator — descending
        Collections.sort(nums, Comparator.reverseOrder());
        System.out.println("Sorted desc          : " + nums);

        // binarySearch — MUST be sorted first, O(log n)
        Collections.sort(nums);
        int idx = Collections.binarySearch(nums, 7);
        System.out.println("binarySearch(7)      : index " + idx);

        // binarySearch on unsorted → UNDEFINED behaviour, not an exception
        List<Integer> unsorted = Arrays.asList(5, 2, 8, 1);
        int bad = Collections.binarySearch(unsorted, 5);
        System.out.println("binarySearch unsorted: " + bad + " (undefined!)");

        // min and max — O(n) linear scan
        System.out.println("min                  : " + Collections.min(nums));
        System.out.println("max                  : " + Collections.max(nums));

        // min/max with Comparator — useful for custom objects
        List<String> words = Arrays.asList("banana", "apple", "cherry", "date");
        System.out.println("Shortest word        : "
                + Collections.min(words, Comparator.comparingInt(String::length)));
        System.out.println("Longest word         : "
                + Collections.max(words, Comparator.comparingInt(String::length)));

        // frequency — count occurrences, O(n)
        List<String> tags = Arrays.asList("java","spring","java","docker","java");
        System.out.println("frequency('java')    : "
                + Collections.frequency(tags, "java"));

        // shuffle — random permutation, useful for testing and card games
        List<Integer> deck = new ArrayList<>(Arrays.asList(1,2,3,4,5,6,7,8,9,10));
        Collections.shuffle(deck, new Random(42)); // seed for reproducibility
        System.out.println("Shuffled deck        : " + deck);
    }

    // Modification
    static void modification() {
        List<String> list = new ArrayList<>(
                Arrays.asList("A", "B", "C", "D", "E"));

        // reverse — in-place, O(n)
        Collections.reverse(list);
        System.out.println("After reverse        : " + list);

        // rotate — shift elements right by distance, O(n)
        // positive distance → shift right, negative → shift left
        Collections.rotate(list, 2);
        System.out.println("After rotate(2)      : " + list);
        Collections.rotate(list, -2);
        System.out.println("After rotate(-2)     : " + list);

        // swap — swap elements at two indices, O(1)
        Collections.swap(list, 0, 4);
        System.out.println("After swap(0,4)      : " + list);

        // fill — replace all elements with one value, O(n)
        List<String> filled = new ArrayList<>(Arrays.asList("X","X","X"));
        Collections.fill(filled, "EMPTY");
        System.out.println("After fill('EMPTY')  : " + filled);

        // copy — copy src into dest (dest must be at least as large as src)
        List<String> src  = Arrays.asList("1","2","3");
        List<String> dest = new ArrayList<>(Arrays.asList("A","B","C","D","E"));
        Collections.copy(dest, src);
        System.out.println("After copy           : " + dest);

        // nCopies — create immutable list of n copies of one element
        // Common use: pre-fill a list or initialize a board
        List<String> nCopies = Collections.nCopies(5, "PENDING");
        System.out.println("nCopies(5,'PENDING') : " + nCopies);

        // disjoint — true if two collections share NO common elements, O(n)
        List<String> evens = Arrays.asList("2","4","6");
        List<String> odds  = Arrays.asList("1","3","5");
        List<String> mixed = Arrays.asList("1","2","3");
        System.out.println("evens disjoint odds  : "
                + Collections.disjoint(evens, odds));   // true
        System.out.println("evens disjoint mixed : "
                + Collections.disjoint(evens, mixed));  // false
    }

    // Wrapping & Defensive Copies
    static void wrapping() {

        // ── unmodifiableList ──
        List<String> mutable = new ArrayList<>(Arrays.asList("A","B","C"));
        List<String> readOnly = Collections.unmodifiableList(mutable);
        System.out.println("readOnly             : " + readOnly);
        try {
            readOnly.add("D"); // throws UnsupportedOperationException
        } catch (UnsupportedOperationException e) {
            System.out.println("add on readOnly      : UnsupportedOperationException ✓");
        }
        // TRAP: backing list is still mutable — readOnly reflects changes
        mutable.add("D");
        System.out.println("readOnly after mutable.add: " + readOnly); // shows D!

        // ── singleton collections ──
        // Immutable single-element collections
        List<String> single = Collections.singletonList("onlyOne");
        Set<String>  singSet = Collections.singleton("onlyOne");
        Map<String,Integer> singMap = Collections.singletonMap("key", 42);
        System.out.println("singletonList        : " + single);
        System.out.println("singleton set size   : " + singSet.size());
        System.out.println("singletonMap value   : " + singMap.get("key"));

        // ── empty collections ──
        // Immutable, reusable, no allocation (cached instances)
        List<String>        emptyList = Collections.emptyList();
        Set<String>         emptySet  = Collections.emptySet();
        Map<String,Integer> emptyMap  = Collections.emptyMap();
        System.out.println("emptyList size       : " + emptyList.size());
        System.out.println("emptySet size        : " + emptySet.size());
        // Better than null — callers can iterate safely without NPE

        // ── synchronizedList ──
        List<String> synced = Collections.synchronizedList(new ArrayList<>());
        synced.add("thread-safe-add"); // individual ops are thread-safe
        // BUT iteration still needs external synchronisation!
        synchronized (synced) {
            for (String s : synced) {
                System.out.println("synchronized iter    : " + s);
            }
        }
    }

    // Senior Level (Production Patterns)
    static void advLevel() {

        // ── Pattern 1: Return empty instead of null ──
        // Never return null collections from service methods
        // Callers can always iterate safely
        System.out.println("── Pattern: return empty not null ──");
        List<String> results = findByTag("missing");
        System.out.println("Results (safe to use): " + results);
        System.out.println("Is empty: " + results.isEmpty()); // no NPE

        // ── Pattern 2: Unmodifiable view from service ──
        System.out.println("\n── Pattern: defensive unmodifiable return ──");
        List<String> config = getConfig();
        try {
            config.add("hack"); // rejected
        } catch (UnsupportedOperationException e) {
            System.out.println("Cannot modify service config ✓");
        }

        // ── Pattern 3: nCopies for initialisation ──
        System.out.println("\n── Pattern: nCopies for board initialisation ──");
        // 3×3 board initialised to "."
        List<List<String>> board = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            board.add(new ArrayList<>(Collections.nCopies(3, ".")));
        }
        board.get(1).set(1, "X"); // place X in centre
        board.forEach(System.out::println);

        // ── Pattern 4: frequency for validation ──
        System.out.println("\n── Pattern: frequency for duplicate detection ──");
        List<String> submissions = Arrays.asList(
                "alice@co.com","bob@co.com","alice@co.com","carol@co.com");
        Set<String> seen = new HashSet<>();
        List<String> duplicates = submissions.stream()
                .filter(e -> !seen.add(e))
                .collect(Collectors.toList());
        System.out.println("Duplicate emails     : " + duplicates);

        // ── Pattern 5: rotate for scheduling ──
        System.out.println("\n── Pattern: rotate for round-robin scheduling ──");
        List<String> onCallRota = new ArrayList<>(
                Arrays.asList("Alice","Bob","Charlie","Diana"));
        System.out.println("Week 1 on-call: " + onCallRota.get(0));
        Collections.rotate(onCallRota, -1); // advance rotation
        System.out.println("Week 2 on-call: " + onCallRota.get(0));
        Collections.rotate(onCallRota, -1);
        System.out.println("Week 3 on-call: " + onCallRota.get(0));

        // ── Pattern 6: disjoint for permission intersection check ──
        System.out.println("\n── Pattern: disjoint for access control ──");
        Set<String> required  = new HashSet<>(Arrays.asList("READ","WRITE"));
        Set<String> userPerms = new HashSet<>(Arrays.asList("READ","DELETE"));
        boolean hasAccess = !Collections.disjoint(required, userPerms);
        System.out.println("Has any required permission: " + hasAccess);
    }

    // ── Helper methods ────────────────────────────
    static List<String> findByTag(String tag) {
        // Service method — returns empty list instead of null
        // Collections.emptyList() is immutable and uses zero memory (cached)
        return Collections.emptyList();
    }

    static List<String> getConfig() {
        List<String> internal = new ArrayList<>(
                Arrays.asList("server.port=8080", "db.url=localhost"));
        return Collections.unmodifiableList(internal);
    }

}