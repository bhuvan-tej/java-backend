```
██████╗ ███████╗ █████╗ 
██╔══██╗██╔════╝██╔══██╗
██║  ██║███████╗███████║
██║  ██║╚════██║██╔══██║
██████╔╝███████║██║  ██║
╚═════╝ ╚══════╝╚═╝  ╚═╝
 
Data Structures & Algorithms
```

> Understand the pattern. Not the solution.
> Each problem includes inline diagrams, brute force → optimal walkthrough, and complexity analysis.
 
---

## 🧠 Patterns at a Glance

| Pattern | Use When | Problems |
|---|---|---|
| **HashMap** | Find pairs, count frequencies | Two Sum, Valid Anagram |
| **HashSet** | Detect duplicates, membership | Contains Duplicate |
| **Two Pointers** | Both ends, sorted input | Valid Palindrome |
| **Single Pass** | Track min / max in one scan | Best Time to Buy Stock |
| **Sliding Window** | Subarray / substring problems | *(coming next)* |
 
---

## ⏱ Complexity Cheat Sheet

```
O(1)        → Constant     — direct access, HashMap lookup
O(log n)    → Logarithmic  — binary search, halving the input
O(n)        → Linear       — single loop, one pass  ← target this
O(n log n)  → Sorting      — Arrays.sort(), Collections.sort()
O(n²)       → Quadratic    — nested loops            ← always ask: can I do better?
O(2ⁿ)       → Exponential  — uncached recursion      ← almost never acceptable
```

**The upgrade rule:** nested loop O(n²) → add a HashMap → O(n). Works for most array/string problems.
 
---

## 💡 How to Approach Each Problem

```
1. Read      → understand input, output, constraints
2. Clarify   → ask about edge cases before coding
3. Brute     → state the naive solution and its complexity
4. Optimise  → explain the insight, then code the better solution
5. Test      → run through examples + edge cases in main()
6. Complexity→ always state time AND space at the end
```
 
---