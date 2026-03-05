# 🔁 Iterator

> Covers `Iterator`, `ListIterator`, `Iterable`, fail-fast vs fail-safe,
> and custom iterators. The foundation of how Java traverses every collection
> and how for-each loops actually work under the hood.

---

## 🧠 Mental Model

```
Iterator                          ListIterator (Lists only)
──────────────────────────        ──────────────────────────────────
[A] → [B] → [C] → [D]             [A] ⟷ [B] ⟷ [C] ⟷ [D]
 ↑                                      ↑
cursor (forward only)             cursor (forward AND backward)

hasNext() → is there a next?      hasNext()     hasPrevious()
next()    → return + advance      next()        previous()
remove()  → remove last returned  nextIndex()   previousIndex()
                                  set(e)        → replace last returned
                                  add(e)        → insert at cursor

Iterable                          Fail-Fast vs Fail-Safe
──────────────────────────        ──────────────────────────────────
interface Iterable<T> {           Fail-Fast (ArrayList, HashMap)
  Iterator<T> iterator();           modCount tracked on collection
}                                   CME thrown if modified outside iterator

Any class that implements           Fail-Safe (CopyOnWriteArrayList)
Iterable<T> can be used in          iterator works on a SNAPSHOT
for-each loops                      no CME, may miss live updates
```

---

## 📄 Classes in this Module

### `IteratorSamples.java`

| Example | What it covers |
|---------|----------------|
| Iterator Basics | Forward iteration, safe removal with iterator.remove(), IllegalStateException |
| ListIterator | Bidirectional traversal, set() in-place replace, add() at cursor, start from index |
| Custom Iterable | IntRange + PaginatedList — any class usable in for-each |
| Fail-Fast vs Fail-Safe | modCount mechanism, CME demo, CopyOnWriteArrayList snapshot |
| BST Inorder Iterator | Lazy O(h) space iterator using explicit stack — classic interview |

---

## ⚡ Key Methods

```
// ── Iterator ──────────────────────────────────────────────────
Iterator<E> it = list.iterator();
it.hasNext()          // true if more elements remain
it.next()             // return next element + advance cursor
it.remove()           // remove element returned by last next() — O(1) for ArrayList

// ── ListIterator ──────────────────────────────────────────────
ListIterator<E> lit = list.listIterator();       // start from index 0
ListIterator<E> lit = list.listIterator(n);      // start from index n
lit.hasNext()         // forward check
lit.hasPrevious()     // backward check
lit.next()            // return next + advance forward
lit.previous()        // return previous + advance backward
lit.nextIndex()       // index of element that next() would return
lit.previousIndex()   // index of element that previous() would return
lit.set(e)            // replace element returned by last next()/previous()
lit.add(e)            // insert at current cursor position
lit.remove()          // remove element returned by last next()/previous()

// ── Iterable ──────────────────────────────────────────────────
class MyCollection implements Iterable<T> {
    public Iterator<T> iterator() { return new MyIterator(); }
}
// Now usable in for-each:
for (T item : myCollection) { ... }
```

---

## 🔑 For-Each is Just an Iterator

```
// This for-each loop:
for (String s : list) {
    System.out.println(s);
}

// Compiles to exactly this:
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    String s = it.next();
    System.out.println(s);
}
```

---

## ⚡ Fail-Fast vs Fail-Safe

```
Fail-Fast                               Fail-Safe
──────────────────────────────────      ──────────────────────────────────
ArrayList, LinkedList, HashMap          CopyOnWriteArrayList, ConcurrentHashMap
HashSet, TreeMap, TreeSet               CopyOnWriteArraySet

How it works:                           How it works:
  modCount field on collection            Iterator takes snapshot at creation
  increments on every structural          Iterates over snapshot — not live data
  modification (add/remove/clear)
                                        
Iterator captures expectedModCount        
  at creation time                        
                                          
On every next():                              
  if modCount != expectedModCount          
   throw ConcurrentModificationException

Tradeoffs:                                Tradeoffs:  
  ✅ Catches bugs early                     ✅ No CME ever
  ❌ Not truly thread-safe (best-effort)    ✅ Safe to modify live list concurrently 
  ❌ CME can occur even single-threaded     ❌ May not see elements added after iterator was created
      if you remove inside for-each         ❌ Memory overhead — full copy on write
```

---

## 🔑 Common Mistakes

```
// ❌ WRONG — remove inside for-each → CME
for (String s : list) {
    if (s.startsWith("X")) list.remove(s); // ConcurrentModificationException!
}

// ✅ CORRECT — iterator.remove()
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    if (it.next().startsWith("X")) it.remove(); // safe
}

// ✅ CORRECT — removeIf (Java 8+, cleanest)
list.removeIf(s -> s.startsWith("X"));

// ❌ WRONG — remove() before next() → IllegalStateException
Iterator<String> it = list.iterator();
it.remove(); // must call next() first!

// ❌ WRONG — calling next() when hasNext() is false → NoSuchElementException
Iterator<String> it = list.iterator();
while (true) it.next(); // no hasNext() guard → exception

// ❌ WRONG — expecting ListIterator.add() to replace current element
// add() INSERTS at cursor — does NOT replace
lit.add("new"); // inserts, cursor advances past new element

// ✅ CORRECT — use set() to replace
lit.set("new"); // replaces element returned by last next()/previous()
```

---

## ⚡ Iterator vs ListIterator vs Enhanced For-Each

| | Iterator | ListIterator | Enhanced For-Each |
|---|---|---|---|
| Direction | Forward only | Both directions | Forward only |
| Works on | Any Iterable | List only | Any Iterable |
| Remove during iteration | ✅ it.remove() | ✅ lit.remove() | ❌ CME |
| Replace during iteration | ❌ | ✅ lit.set() | ❌ |
| Insert during iteration | ❌ | ✅ lit.add() | ❌ |
| Get current index | ❌ | ✅ nextIndex() | ❌ |
| Start from index | ❌ | ✅ listIterator(n) | ❌ |

---