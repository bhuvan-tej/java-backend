package com.javabackend.collections.realworld;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 *
 * TOPIC : Real World Problems
 *
 * Combines everything from the collections' framework.
 * Each problem is a production-grade scenario you would
 * actually encounter in a Java backend service.
 *
 * PROBLEMS COVERED
 *   1. Word Frequency Analyzer      — HashMap + merge + stream
 *   2. LRU Cache Service            — LinkedHashMap accessOrder
 *   3. Task Scheduler               — PriorityQueue + Comparator chaining
 *   4. Sliding Window Rate Limiter  — TreeMap + tailMap
 *   5. Graph BFS / DFS              — ArrayDeque + HashSet visited
 *   6. Top-K Products               — PriorityQueue min-heap trick
 *   7. Anagram Grouper              — HashMap + sorted key
 *   8. Stock Price Window Stats     — TreeMap NavigableMap ops
 *   9. Multithreaded Hit Counter    — ConcurrentHashMap + AtomicInteger
 *  10. Shopping Cart + Discount     — LinkedHashMap + TreeMap bracket lookup
 *
 */
public class RealWorldSamples {

    public static void main(String[] args) throws InterruptedException {
        sep("1. Word Frequency Analyser");
        wordFrequency();

        sep("2. LRU Cache Service");
        lruCacheService();

        sep("3. Task Scheduler");
        taskScheduler();

        sep("4. Sliding Window Rate Limiter");
        rateLimiter();

        sep("5. Graph BFS / DFS");
        graphTraversal();

        sep("6. Top-K Products by Sales");
        topKProducts();

        sep("7. Anagram Grouper");
        anagramGrouper();

        sep("8. Stock Price Window Stats");
        stockPriceStats();

        sep("9. Multi-threaded Hit Counter");
        hitCounter();

        sep("10. Shopping Cart + Discount Brackets");
        shoppingCart();
    }

    // 1. WORD FREQUENCY ANALYSER
    //    → HashMap merge() + stream sorted by count
    static void wordFrequency() {
        String text = "the quick brown fox jumps over the lazy dog " +
                "the fox was quick and the dog was lazy";

        // merge() — cleanest frequency counter
        Map<String, Integer> freq = new HashMap<>();
        for (String word : text.split("\\s+")) {
            freq.merge(word, 1, Integer::sum);
        }

        // Top 5 by frequency
        System.out.println("Top 5 words:");
        freq.entrySet().stream()
                .sorted(Map.Entry.<String,Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> System.out.printf("  %-10s %d%n", e.getKey(), e.getValue()));

        // Words appearing exactly once (hapax legomena)
        List<String> unique = freq.entrySet().stream()
                .filter(e -> e.getValue() == 1)
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());
        System.out.println("Unique words : " + unique);
    }

    // 2. LRU CACHE SERVICE
    //    → LinkedHashMap accessOrder=true
    static void lruCacheService() {
        LRUCache<String, String> cache = new LRUCache<>(3);

        cache.put("user:1", "Alice");
        cache.put("user:2", "Bob");
        cache.put("user:3", "Charlie");
        System.out.println("Initial : " + cache.keys());

        cache.get("user:1");           // user:1 → MRU
        cache.put("user:4", "Diana");  // user:2 evicted (LRU)
        System.out.println("After get(user:1) + put(user:4): " + cache.keys());
        System.out.println("user:2 evicted: " + (cache.get("user:2") == null));
    }

    // 3. TASK SCHEDULER
    //    → PriorityQueue: priority → deadline → name
    static void taskScheduler() {
        PriorityQueue<Task> queue = new PriorityQueue<>(
                Comparator.comparingInt(Task::getPriority)     // 1 = highest
                        .thenComparingLong(Task::getDeadline)
                        .thenComparing(Task::getName));

        long now = System.currentTimeMillis();
        queue.offer(new Task("Deploy prod",    1, now + 3000));
        queue.offer(new Task("Fix hotfix",     1, now + 1000));
        queue.offer(new Task("Write tests",    2, now + 5000));
        queue.offer(new Task("Update docs",    3, now + 2000));
        queue.offer(new Task("Code review",    2, now + 4000));

        System.out.println("Execution order:");
        while (!queue.isEmpty()) {
            Task t = queue.poll();
            System.out.printf("  [P%d] %-20s deadline+%ds%n",
                    t.getPriority(), t.getName(),
                    (t.getDeadline() - now) / 1000);
        }
    }

    // 4. SLIDING WINDOW RATE LIMITER
    //    → TreeMap: headMap().clear() removes expired
    static void rateLimiter() {
        RateLimiter limiter = new RateLimiter(5, 1000); // 5 req/sec

        System.out.println("Sending 7 requests in quick succession:");
        for (int i = 1; i <= 7; i++) {
            boolean allowed = limiter.isAllowed();
            System.out.printf("  Request %d: %s%n", i,
                    allowed ? "✅ ALLOWED" : "❌ RATE LIMITED");
        }
    }

    // 5. GRAPH BFS / DFS
    //    → ArrayDeque as Queue (BFS) and Stack (DFS)
    static void graphTraversal() {
        // Graph:  1 - 2 - 4
        //         |   |
        //         3 - 5 - 6
        Map<Integer, List<Integer>> graph = new LinkedHashMap<>();
        graph.put(1, Arrays.asList(2, 3));
        graph.put(2, Arrays.asList(1, 4, 5));
        graph.put(3, Arrays.asList(1, 5));
        graph.put(4, Arrays.asList(2));
        graph.put(5, Arrays.asList(2, 3, 6));
        graph.put(6, Arrays.asList(5));

        System.out.println("BFS from 1: " + bfs(graph, 1));
        System.out.println("DFS from 1: " + dfs(graph, 1));

        // Shortest path BFS
        List<Integer> path = shortestPath(graph, 1, 6);
        System.out.println("Shortest 1→6: " + path);
    }

    // 6. TOP-K PRODUCTS BY SALES
    //    → Min-heap of size K — O(n log k)
    static void topKProducts() {
        List<Product> products = Arrays.asList(
                new Product("Laptop",     1500),
                new Product("Phone",      3200),
                new Product("Headphones",  800),
                new Product("Monitor",    1100),
                new Product("Keyboard",    450),
                new Product("Mouse",       380),
                new Product("Webcam",      290),
                new Product("Tablet",     2100)
        );

        int k = 3;
        // Min-heap by sales — keeps K highest sales
        PriorityQueue<Product> topK = new PriorityQueue<>(
                Comparator.comparingInt(Product::getSales));

        for (Product p : products) {
            topK.offer(p);
            if (topK.size() > k) topK.poll(); // evict lowest sales
        }

        // Drain in descending order
        List<Product> result = new ArrayList<>(topK);
        result.sort(Comparator.comparingInt(Product::getSales).reversed());
        System.out.println("Top " + k + " products by sales:");
        result.forEach(p -> System.out.printf(
                "  %-15s %,d units%n", p.getName(), p.getSales()));
    }

    // 7. ANAGRAM GROUPER
    //    → HashMap: sorted chars → key, group anagrams
    static void anagramGrouper() {
        List<String> words = Arrays.asList(
                "eat", "tea", "tan", "ate", "nat", "bat",
                "listen", "silent", "enlist", "google", "gooegl"
        );

        // Key insight: anagrams have identical sorted characters
        Map<String, List<String>> groups = new HashMap<>();
        for (String word : words) {
            char[] chars = word.toCharArray();
            Arrays.sort(chars);
            String key = new String(chars);            // "aet" for eat/tea/ate
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(word);
        }

        System.out.println("Anagram groups:");
        groups.values().stream()
                .sorted(Comparator.<List<String>>comparingInt(List::size).reversed())
                .forEach(group -> System.out.println("  " + group));
    }

    // 8. STOCK PRICE WINDOW STATS
    //    → TreeMap: range queries, floor/ceiling

    static void stockPriceStats() {
        // timestamp (ms offset) → price
        TreeMap<Long, Double> prices = new TreeMap<>();
        prices.put(0L,    150.0);
        prices.put(1000L, 152.5);
        prices.put(2000L, 148.0);
        prices.put(3000L, 155.0);
        prices.put(4000L, 153.5);
        prices.put(5000L, 160.0);
        prices.put(6000L, 158.0);

        // Stats for last 3 seconds (3000ms window)
        long now    = 6000L;
        long window = 3000L;
        SortedMap<Long, Double> recent = prices.tailMap(now - window);

        DoubleSummaryStatistics stats = recent.values().stream()
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();

        System.out.printf("Last 3s window  : %s%n", recent.values());
        System.out.printf("  Min  : %.1f%n", stats.getMin());
        System.out.printf("  Max  : %.1f%n", stats.getMax());
        System.out.printf("  Avg  : %.2f%n", stats.getAverage());

        // Price nearest to a target (floor + ceiling)
        double target = 154.0;
        Map.Entry<Long, Double> below  = prices.floorEntry(3500L);
        Map.Entry<Long, Double> above  = prices.ceilingEntry(3500L);
        System.out.printf("Nearest below T=3500: T=%d %.1f%n",
                below.getKey(), below.getValue());
        System.out.printf("Nearest above T=3500: T=%d %.1f%n",
                above.getKey(), above.getValue());
    }

    // 9. MULTI-THREADED HIT COUNTER
    //    → ConcurrentHashMap + AtomicInteger

    static void hitCounter() throws InterruptedException {
        ConcurrentHashMap<String, AtomicInteger> hits =
                new ConcurrentHashMap<>();

        String[] endpoints = {"/api/users", "/api/orders", "/api/products"};
        int threadCount = 4, requestsPerThread = 250;
        CountDownLatch latch = new CountDownLatch(threadCount);
        Random rng = new Random();

        for (int t = 0; t < threadCount; t++) {
            new Thread(() -> {
                for (int i = 0; i < requestsPerThread; i++) {
                    String ep = endpoints[rng.nextInt(endpoints.length)];
                    hits.computeIfAbsent(ep, k -> new AtomicInteger())
                            .incrementAndGet();
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        int total = hits.values().stream().mapToInt(AtomicInteger::get).sum();
        System.out.println("Hit counts (" + threadCount + " threads × "
                + requestsPerThread + " requests = " + total + " total):");
        hits.entrySet().stream()
                .sorted(Map.Entry.<String,AtomicInteger>comparingByKey())
                .forEach(e -> System.out.printf("  %-20s %d%n",
                        e.getKey(), e.getValue().get()));
        System.out.println("Total correct: " + (total == threadCount * requestsPerThread));
    }

    // 10. SHOPPING CART + DISCOUNT BRACKETS
    //     → LinkedHashMap preserves add order
    //     → TreeMap floorKey() for discount lookup

    static void shoppingCart() {
        // Cart preserves insertion order for display
        LinkedHashMap<String, CartItem> cart = new LinkedHashMap<>();
        cart.put("Laptop",     new CartItem("Laptop",     75_000, 1));
        cart.put("Mouse",      new CartItem("Mouse",       1_500, 2));
        cart.put("Keyboard",   new CartItem("Keyboard",    3_500, 1));
        cart.put("Monitor",    new CartItem("Monitor",    22_000, 1));

        int subtotal = cart.values().stream()
                .mapToInt(i -> i.getPrice() * i.getQty()).sum();

        // Discount bracket lookup via TreeMap floorKey
        TreeMap<Integer, Double> discounts = new TreeMap<>();
        discounts.put(0,       0.00); // < ₹10k  : no discount
        discounts.put(10_000,  0.05); // ₹10k+   : 5%
        discounts.put(50_000,  0.10); // ₹50k+   : 10%
        discounts.put(1_00_000, 0.15); // ₹1L+   : 15%

        Integer bracket  = discounts.floorKey(subtotal);
        double  rate     = discounts.get(bracket);
        int     discount = (int)(subtotal * rate);
        int     total    = subtotal - discount;

        System.out.println("Cart items (insertion order):");
        cart.values().forEach(i -> System.out.printf(
                "  %-12s ₹%,6d × %d = ₹%,d%n",
                i.getName(), i.getPrice(), i.getQty(),
                i.getPrice() * i.getQty()));
        System.out.printf("Subtotal   : ₹%,d%n", subtotal);
        System.out.printf("Discount   : %.0f%% = ₹%,d%n", rate * 100, discount);
        System.out.printf("Total      : ₹%,d%n", total);
    }

    // HELPER METHODS

    static List<Integer> bfs(Map<Integer, List<Integer>> graph, int start) {
        List<Integer> order = new ArrayList<>();
        Set<Integer> visited = new LinkedHashSet<>();
        Queue<Integer> queue = new ArrayDeque<>();
        queue.offer(start);
        visited.add(start);
        while (!queue.isEmpty()) {
            int node = queue.poll();
            order.add(node);
            for (int neighbour : graph.getOrDefault(node, Collections.emptyList())) {
                if (visited.add(neighbour)) queue.offer(neighbour);
            }
        }
        return order;
    }

    static List<Integer> dfs(Map<Integer, List<Integer>> graph, int start) {
        List<Integer> order = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(start);
        while (!stack.isEmpty()) {
            int node = stack.pop();
            if (visited.add(node)) {
                order.add(node);
                List<Integer> neighbours = graph.getOrDefault(
                        node, Collections.emptyList());
                for (int i = neighbours.size() - 1; i >= 0; i--)
                    if (!visited.contains(neighbours.get(i)))
                        stack.push(neighbours.get(i));
            }
        }
        return order;
    }

    static List<Integer> shortestPath(
            Map<Integer, List<Integer>> graph, int src, int dst) {
        Map<Integer, Integer> parent = new HashMap<>();
        Queue<Integer> queue = new ArrayDeque<>();
        queue.offer(src);
        parent.put(src, -1);
        while (!queue.isEmpty()) {
            int node = queue.poll();
            if (node == dst) break;
            for (int nb : graph.getOrDefault(node, Collections.emptyList())) {
                if (!parent.containsKey(nb)) {
                    parent.put(nb, node);
                    queue.offer(nb);
                }
            }
        }
        // Reconstruct path
        List<Integer> path = new ArrayList<>();
        for (int at = dst; at != -1; at = parent.get(at)) path.add(at);
        Collections.reverse(path);
        return path;
    }

    static void sep(String title) {
        System.out.println("\n━━━ " + title + " ━━━\n");
    }

    // MODEL CLASSES

    static class LRUCache<K, V> extends LinkedHashMap<K, V> {
        private final int cap;
        LRUCache(int cap) { super(16, 0.75f, true); this.cap = cap; }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> e) {
            return size() > cap;
        }

        List<K> keys() { return new ArrayList<>(keySet()); }
    }

    static class Task {
        String name; int priority; long deadline;
        Task(String name, int priority, long deadline) {
            this.name = name; this.priority = priority; this.deadline = deadline;
        }
        String getName()     { return name; }
        int    getPriority() { return priority; }
        long   getDeadline() { return deadline; }
    }

    static class RateLimiter {
        private final int maxRequests;
        private final long windowMs;
        private final TreeMap<Long, Integer> requests = new TreeMap<>();

        RateLimiter(int maxRequests, long windowMs) {
            this.maxRequests = maxRequests;
            this.windowMs    = windowMs;
        }

        boolean isAllowed() {
            long now   = System.currentTimeMillis();
            long start = now - windowMs;
            requests.headMap(start).clear();           // evict expired
            int total = requests.values().stream()
                    .mapToInt(Integer::intValue).sum();
            if (total >= maxRequests) return false;
            requests.merge(now, 1, Integer::sum);
            return true;
        }
    }

    static class Product {
        String name; int sales;
        Product(String name, int sales) {
            this.name = name; this.sales = sales;
        }
        String getName()  { return name; }
        int    getSales() { return sales; }
    }

    static class CartItem {
        String name; int price, qty;
        CartItem(String name, int price, int qty) {
            this.name = name; this.price = price; this.qty = qty;
        }
        String getName()  { return name; }
        int    getPrice() { return price; }
        int    getQty()   { return qty; }
    }

}