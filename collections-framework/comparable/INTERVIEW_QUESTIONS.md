# 🎯 Interview Questions — Comparable vs Comparator

---

> As we gain experience, Comparable/Comparator questions go beyond
> "what is the difference". Interviewers want to hear about the
> overflow bug, TreeSet uniqueness trap, Comparator chaining in
> production, and consistency with equals.

---

## Core Concepts

**Q1. What is the difference between `Comparable` and `Comparator`? When would you use each in a real project?**

> `Comparable` — the class defines its own natural ordering by implementing `compareTo()`. One ordering per class. Use when you own the class and there is one obvious sort order that always makes sense (e.g., `LocalDate` by date, `Employee` by id).
>
> `Comparator` — external ordering strategy defined outside the class via `compare()`. Multiple orderings possible. Use when:
> - You need multiple orderings (by name, by salary, by department)
> - You don't own the class (sorting `String` by length, not alphabetically)
> - The ordering is context-specific (ascending in one API, descending in another)
>
> Production example — a product listing service that sorts differently based on user preference:
> ```
> Map<String, Comparator<Product>> strategies = new HashMap<>();
> strategies.put("price_asc",   Comparator.comparingDouble(Product::getPrice));
> strategies.put("price_desc",  Comparator.comparingDouble(Product::getPrice).reversed());
> strategies.put("rating_desc", Comparator.comparingDouble(Product::getRating).reversed());
>
> products.sort(strategies.getOrDefault(userPreference,
>               Comparator.comparing(Product::getName)));
> ```

---

**Q2. What does `compareTo()` return and what are the rules? Why should you never use subtraction?**

> Returns an `int`:
> - Negative → this < other → this comes before other
> - Zero     → this == other → same position
> - Positive → this > other → this comes after other
>
> **Never use subtraction:**
> ```
> // WRONG — integer overflow
> public int compareTo(Employee other) {
>     return this.id - other.id;
>     // If this.id = Integer.MIN_VALUE (-2147483648) and other.id = 1
>     // result = -2147483648 - 1 = 2147483647 (positive!) — WRONG ORDER
> }
>
> // CORRECT — Integer.compare handles all edge cases
> public int compareTo(Employee other) {
>     return Integer.compare(this.id, other.id);
> }
> // Similarly: Double.compare(), Long.compare(), Boolean.compare()
> ```
>
> The overflow bug is silent — no exception, just wrong sort order in production for extreme values.

---

**Q3. What is "consistent with equals" and why does it matter for `TreeSet` and `TreeMap`?**

> Consistent with equals means: `a.compareTo(b) == 0` if and only if `a.equals(b) == true`.
>
> `TreeSet` and `TreeMap` use `compareTo()` for uniqueness — not `equals()`. If `compareTo()` returns 0 for two objects that `equals()` considers different, `TreeSet` silently drops one:
>
> ```
> // BigDecimal violates consistency — classic gotcha
> BigDecimal a = new BigDecimal("1.0");
> BigDecimal b = new BigDecimal("1.00");
>
> a.equals(b);        // false — different scale
> a.compareTo(b);     // 0 — same numeric value
>
> Set<BigDecimal> hashSet = new HashSet<>();
> hashSet.add(a); hashSet.add(b);
> System.out.println(hashSet.size()); // 2 — uses equals()
>
> Set<BigDecimal> treeSet = new TreeSet<>();
> treeSet.add(a); treeSet.add(b);
> System.out.println(treeSet.size()); // 1 — uses compareTo(), b dropped!
> ```
>
> The Javadoc strongly recommends keeping `compareTo()` consistent with `equals()`. When it is not (like `BigDecimal`), document it explicitly and avoid `TreeSet`/`TreeMap` for that class.

---

**Q4. How do you sort by multiple fields using `Comparator`? Give a production example.**

> Use `Comparator` chaining with `thenComparing`:
> ```
> // Sort employees: department A-Z, then salary high-to-low, then name A-Z
> Comparator<Employee> sort = Comparator
>     .comparing(Employee::getDept)                    // primary
>     .thenComparingInt(Employee::getSalary).reversed() // secondary desc
>     .thenComparing(Employee::getName);                // tertiary
>
> employees.sort(sort);
> ```
>
> Production scenario — an order management dashboard where results are sorted by:
> 1. Status (PENDING before SHIPPED before DELIVERED)
> 2. Priority (HIGH before MEDIUM before LOW)
> 3. Created date (newest first)
> 4. Order id (stable tiebreaker)
>
> ```
> Map<String, Integer> statusOrder = Map.of(
>     "PENDING", 0, "SHIPPED", 1, "DELIVERED", 2);
>
> Comparator<Order> dashboardSort = Comparator
>     .comparingInt((Order o) -> statusOrder.get(o.getStatus()))
>     .thenComparingInt(o -> o.getPriority().ordinal())
>     .thenComparing(Comparator.comparing(Order::getCreatedAt).reversed())
>     .thenComparingLong(Order::getId);
> ```

---

**Q5. What is `Comparator.naturalOrder()` vs `Comparator.reverseOrder()`?**

> `Comparator.naturalOrder()` — returns a Comparator that uses the object's own `compareTo()`. Equivalent to `(a, b) -> a.compareTo(b)`. Useful when you need a Comparator reference but want natural order — for example, `Comparator.nullsLast(Comparator.naturalOrder())`.
>
> `Comparator.reverseOrder()` — returns a Comparator that reverses the natural order. Equivalent to `(a, b) -> b.compareTo(a)`. Used for descending sort on naturally comparable types:
> ```
> // Descending sort on integers
> list.sort(Comparator.reverseOrder());
>
> // Max-heap PriorityQueue
> PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Comparator.reverseOrder());
>
> // nullsLast with natural order — common defensive pattern
> list.sort(Comparator.nullsLast(Comparator.naturalOrder()));
> ```

---

## Design & Production

**Q6. You have a `List<Employee>` and need to sort it differently based on a user's selected column and direction in a REST API. How do you implement this cleanly?**

> Strategy map pattern — build all comparators once, look up at runtime:
> ```
> @Component
> public class EmployeeSortService {
>
>     private static final Map<String, Comparator<Employee>> SORTS;
>
>     static {
>         Map<String, Comparator<Employee>> m = new HashMap<>();
>         m.put("id_asc",      Comparator.comparingInt(Employee::getId));
>         m.put("id_desc",     Comparator.comparingInt(Employee::getId).reversed());
>         m.put("name_asc",    Comparator.comparing(Employee::getName));
>         m.put("name_desc",   Comparator.comparing(Employee::getName).reversed());
>         m.put("salary_asc",  Comparator.comparingInt(Employee::getSalary));
>         m.put("salary_desc", Comparator.comparingInt(Employee::getSalary).reversed());
>         SORTS = Collections.unmodifiableMap(m);
>     }
>
>     public List<Employee> sort(List<Employee> employees,
>                                String sortBy, String direction) {
>         String key = sortBy + "_" + direction.toLowerCase();
>         Comparator<Employee> comp = SORTS.getOrDefault(key,
>                 Comparator.comparingInt(Employee::getId));
>         return employees.stream()
>                 .sorted(comp)
>                 .collect(Collectors.toList());
>     }
> }
> ```
> Avoids if-else chains. Adding a new sort field = one line. Comparators are stateless and reusable — safe for concurrent use.

---

**Q7. How do you handle null values in sorting? What happens without null handling?**

> Without null handling, `Comparator.comparing(Employee::getDept)` throws `NullPointerException` when `compareTo()` is called on a null dept value.
>
> Three approaches:
>
> **Option 1 — `nullsLast` / `nullsFirst` wrappers:**
> ```
> // Nulls appear after all non-null values
> list.sort(Comparator.nullsLast(
>           Comparator.comparing(Employee::getDept)));
>
> // Nulls appear before all non-null values
> list.sort(Comparator.nullsFirst(
>           Comparator.comparing(Employee::getDept)));
> ```
>
> **Option 2 — null-safe key extractor:**
> ```
> list.sort(Comparator.comparing(Employee::getDept,
>           Comparator.nullsLast(Comparator.naturalOrder())));
> ```
>
> **Option 3 — chained with null-safe secondary:**
> ```
> Comparator<Employee> comp = Comparator
>     .comparing(Employee::getDept,
>                Comparator.nullsLast(Comparator.naturalOrder()))
>     .thenComparing(Employee::getName,
>                    Comparator.nullsLast(Comparator.naturalOrder()));
> ```
>
> In production APIs where any field could be null, always wrap with `nullsLast`. Surprises from NPE in sort are harder to debug than NPE in a null check.

---

**Q8. What is the `reversed()` gotcha when chaining Comparators?**

> `reversed()` reverses the ENTIRE comparator built so far — not just the last field. This causes subtle ordering bugs:
>
> ```
> // WRONG — reversed() flips BOTH fields
> Comparator<Employee> wrong = Comparator
>     .comparing(Employee::getDept)
>     .thenComparingInt(Employee::getSalary)
>     .reversed();
> // Result: dept DESC, salary DESC — not what you wanted
>
> // CORRECT — reverse only the salary field
> Comparator<Employee> correct = Comparator
>     .comparing(Employee::getDept)                        // dept ASC
>     .thenComparing(
>         Comparator.comparingInt(Employee::getSalary)
>                   .reversed());                          // salary DESC only
>
> // OR more readable:
> Comparator<Employee> correct2 = Comparator
>     .comparing(Employee::getDept)
>     .thenComparingInt(e -> -e.getSalary());              // negate for desc
> ```

---

**Q9. `Comparable` is implemented on your `Order` class. A new requirement asks you to sort orders by customer name for one specific report. How do you handle this without changing the class?**

> Use a `Comparator` — never change the natural order of a class for a single use case:
> ```
> // Order's natural order is by orderId (via Comparable)
> // One-off report needs customer name order
>
> List<Order> reportOrders = orders.stream()
>     .sorted(Comparator.comparing(Order::getCustomerName)
>                       .thenComparingLong(Order::getOrderId)) // stable tiebreaker
>     .collect(Collectors.toList());
> ```
>
> Changing `compareTo()` to sort by customer name would break every `TreeMap`, `TreeSet`, `PriorityQueue`, and `Collections.sort()` call that relied on the original natural order — a ripple of breakage across the codebase for one report.

---

**Q10. Can a `Comparator` be used for a `TreeSet` even if the objects don't implement `Comparable`?**

> Yes — if a `Comparator` is provided to the `TreeSet` constructor, `compareTo()` is never called. The `Comparator` handles all ordering and uniqueness decisions:
> ```
> // Product does NOT implement Comparable
> class Product {
>     String name; double price;
> }
>
> // TreeSet with external Comparator — works fine
> TreeSet<Product> products = new TreeSet<>(
>     Comparator.comparingDouble(Product::getPrice)
>               .thenComparing(p -> p.name));
>
> products.add(new Product("Laptop", 75_000));
> products.add(new Product("Phone",  45_000));
> // Sorted by price, no Comparable needed
> ```
>
> This is a powerful pattern — you can use `TreeSet`/`TreeMap` with any class as long as you supply a `Comparator`. The class itself never needs to know it will be sorted.

---

**Q11. How does `Collections.sort()` / `List.sort()` implement a stable sort and why does stability matter?**

> Java uses **TimSort** — a stable sort. Stable means equal elements maintain their original relative order after sorting.
>
> Why it matters for multi-field sorting:
> ```
> // Sort by department first
> employees.sort(Comparator.comparing(Employee::getDept));
>
> // Then sort by salary — stable sort means within same salary,
> // department order from previous sort is preserved
> employees.sort(Comparator.comparingInt(Employee::getSalary));
> ```
>
> With an unstable sort, the second sort would scramble the department order within the same salary group. With TimSort, chaining sorts is safe and predictable.
>
> In practice, always prefer single `Comparator` chaining over multiple sequential sorts — it is clearer and guaranteed stable in one pass:
> ```
> employees.sort(Comparator.comparing(Employee::getDept)
>                           .thenComparingInt(Employee::getSalary));
> ```

---

**Q12. How would you sort a list of version strings like `"1.2.10"`, `"1.9.1"`, `"1.10.0"` correctly?**

> Lexicographic sort gives wrong order: `"1.10.0"` sorts before `"1.9.1"` because `'1'` < `'9'` as characters.
>
> Correct: parse into numeric parts and compare each segment:
> ```
> Comparator<String> versionComparator = (v1, v2) -> {
>     String[] parts1 = v1.split("\\.");
>     String[] parts2 = v2.split("\\.");
>     int len = Math.max(parts1.length, parts2.length);
>     for (int i = 0; i < len; i++) {
>         int n1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
>         int n2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
>         int cmp = Integer.compare(n1, n2);
>         if (cmp != 0) return cmp;
>     }
>     return 0;
> };
>
> List<String> versions = Arrays.asList("1.10.0", "1.9.1", "1.2.10", "2.0.0");
> versions.sort(versionComparator);
> // Result: [1.2.10, 1.9.1, 1.10.0, 2.0.0] ✅
> ```
>
> This pattern — custom Comparator for non-standard string ordering — comes up regularly in deployment pipelines, dependency management, and API versioning.

---

**Q13. You have an `enum Priority { LOW, MEDIUM, HIGH }`. How do you sort by priority in the correct order (HIGH first)?**

> Enum's natural order is declaration order — `ordinal()`. `LOW=0, MEDIUM=1, HIGH=2`. Natural order puts LOW first.
>
> To sort HIGH first:
> ```
> // Option 1 — reverse natural order
> tasks.sort(Comparator.comparing(Task::getPriority).reversed());
>
> // Option 2 — explicit ordinal mapping (more control)
> Map<Priority, Integer> order = Map.of(
>     Priority.HIGH, 0, Priority.MEDIUM, 1, Priority.LOW, 2);
> tasks.sort(Comparator.comparingInt(t -> order.get(t.getPriority())));
>
> // Option 3 — reorder enum declaration (cleanest if you own the enum)
> enum Priority { HIGH, MEDIUM, LOW } // HIGH.ordinal()=0 → sorts first
> tasks.sort(Comparator.comparing(Task::getPriority));
> ```
>
> Option 3 is cleanest but only works if changing the enum declaration order doesn't break switch statements or persisted ordinal values (common in JPA). In that case, use the explicit mapping.

---

**Q14. What is the difference between `list.sort(null)` and `Collections.sort(list)`?**

> Both sort using the natural order (Comparable) but:
>
> `Collections.sort(list)` — static method, calls `list.sort(null)` internally since Java 8. Requires elements to implement `Comparable` or throws `ClassCastException` at runtime.
>
> `list.sort(null)` — instance method on `List`. Passing `null` as Comparator signals "use natural order". Same requirement.
>
> `list.sort(comparator)` — use when you have a Comparator and don't want or need natural order.
>
> In modern Java (8+), prefer `list.sort(comparator)` over `Collections.sort(list, comparator)` — it is more readable and avoids the static import. `Collections.sort()` is mainly kept for backwards compatibility.

---

**Q15. How do `Comparator.comparing()` and `Comparator.comparingInt()` differ and when does the difference matter?**

> `Comparator.comparing(keyExtractor)` — uses the generic `Function<T, U>` extractor. The key must implement `Comparable`. The key is boxed if it is a primitive (int → Integer, double → Double).
>
> `Comparator.comparingInt(keyExtractor)` — uses `ToIntFunction<T>`. No boxing. Works directly with primitive int.
>
> The difference matters for performance under heavy sort load:
> ```
> // Comparing — boxes int → Integer on every comparison
> Comparator.comparing(Employee::getSalary); // salary is int, gets boxed
>
> // ComparingInt — no boxing, uses int directly
> Comparator.comparingInt(Employee::getSalary); // correct and faster
> ```
>
> For a list of 1 million employees, `comparingInt` avoids 1 million Integer allocations during sort. For small lists it makes no practical difference.
>
> Equivalents for other primitives:
> - `comparingInt()` → `ToIntFunction`
> - `comparingLong()` → `ToLongFunction`
> - `comparingDouble()` → `ToDoubleFunction`
>
> Always use the primitive-specialised version when the key is a primitive type.