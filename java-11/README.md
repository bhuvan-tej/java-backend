# ☕ Java 11 Features

> Java 11 (September 2018) — LTS release.
> Key theme: String API polish, file I/O convenience, stable HttpClient.

---

## 🧠 What Changed

- **String methods** — `isBlank`, `strip`, `stripLeading`, `stripTrailing`, `lines`, `repeat`
- **Files additions** — `Files.readString`, `Files.writeString`
- **Predicate.not** — negate method references cleanly
- **var in lambdas** — enables annotations on lambda parameters
- **Collection.toArray** — `list.toArray(String[]::new)`
- **HttpClient** — stable API replacing `HttpURLConnection`, sync + async, HTTP/2

---

## 📄 Classes in this Module

### `Java11StringAndMisc.java`

| Example | What it covers |
|---------|----------------|
| String Methods | isBlank, strip vs trim, stripLeading/Trailing, lines, repeat |
| Files Additions | readString, writeString, charset support |
| Predicate.not | Negate method references, custom predicates |
| var in Lambdas | Annotations on lambda params, mixing rules |
| Collection.toArray | toArray(IntFunction), stream toArray |

### `Java11HttpClient.java`

| Example | What it covers |
|---------|----------------|
| Synchronous GET | build, send, statusCode, headers, BodyHandlers |
| POST with body | BodyPublishers, JSON body, Content-Type |
| Async GET | sendAsync, CompletableFuture pipeline |
| Multiple Async | Parallel requests, allOf, elapsed time |
| Headers and Timeout | Response headers, HttpTimeoutException, 4xx handling |

---

## ⚡ String Methods

```
// isBlank — true if empty or only whitespace (Unicode-aware)
"".isBlank()       // true
" ".isBlank()      // true
"hi".isBlank()     // false

// strip — Unicode-aware whitespace removal (prefer over trim)
"  hello  ".strip()          // "hello"
"  hello  ".stripLeading()   // "hello  "
"  hello  ".stripTrailing()  // "  hello"

// trim() only removes ASCII whitespace (≤ char 32) — not Unicode
"\u2000hello\u2000".trim()   // not stripped
"\u2000hello\u2000".strip()  // stripped ✓

// lines — splits into Stream<String>, handles \n \r \r\n
"a\nb\nc".lines().collect(toList())  // [a, b, c]

// repeat
"ab".repeat(3)   // ababab
"-".repeat(40)   // divider line
```

---

## ⚡ Files Additions

```
// readString — read entire file in one call
String content = Files.readString(path);
String content = Files.readString(path, StandardCharsets.UTF_8);

// writeString — write string to file in one call
Files.writeString(path, content);
Files.writeString(path, content, StandardCharsets.UTF_8);
Files.writeString(path, content, StandardOpenOption.APPEND);

// Before Java 11 — verbose
String old = new String(Files.readAllBytes(path), UTF_8);
Files.write(path, content.getBytes(UTF_8));
```

---

## ⚡ Predicate.not

```
// Before Java 11 — can't negate a method reference directly
stream.filter(s -> !s.isBlank())        // lambda required

// Java 11 — clean negation of method reference
stream.filter(Predicate.not(String::isBlank))
stream.filter(Predicate.not(Objects::isNull))
stream.filter(Predicate.not(collection::contains))
```

---

## ⚡ HttpClient

```
// ── Create client — reuse, thread-safe ──
HttpClient client = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_2)
    .connectTimeout(Duration.ofSeconds(5))
    .followRedirects(HttpClient.Redirect.NORMAL)
    .build();

// ── Build request ──
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://api.example.com/users"))
    .GET()
    .header("Accept", "application/json")
    .timeout(Duration.ofSeconds(10))
    .build();

// ── Sync send ──
HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
int status = response.statusCode();
String body = response.body();

// ── Async send ──
client.sendAsync(request, BodyHandlers.ofString())
    .thenApply(HttpResponse::body)
    .thenAccept(System.out::println);

// ── POST with body ──
HttpRequest post = HttpRequest.newBuilder()
    .uri(URI.create("https://api.example.com/users"))
    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
    .header("Content-Type", "application/json")
    .build();

// ── BodyHandlers ──
BodyHandlers.ofString()      // body as String
BodyHandlers.ofBytes()       // body as byte[]
BodyHandlers.ofFile(path)    // stream to file
BodyHandlers.ofLines()       // body as Stream<String>
BodyHandlers.discarding()    // ignore body

// ── BodyPublishers ──
BodyPublishers.ofString(s)        // send String
BodyPublishers.ofByteArray(bytes) // send byte[]
BodyPublishers.ofFile(path)       // send file
BodyPublishers.noBody()           // no body
```

**Important:** HttpClient does NOT throw on 4xx/5xx — always check `statusCode()`.

---

## 🔑 HttpClient vs HttpURLConnection

| | HttpURLConnection | HttpClient (Java 11) |
|--|--|--|
| API style | Verbose, imperative | Fluent builder |
| Async | ❌ | ✅ CompletableFuture |
| HTTP/2 | ❌ | ✅ |
| WebSocket | ❌ | ✅ |
| Thread-safe | ❌ | ✅ |
| Timeout | Manual | Built-in |
| Redirect | Manual | Built-in |

---

## 🔑 Common Mistakes

```
// ❌ trim() on Unicode content
"\u2000hello\u2000".trim()  // doesn't strip Unicode spaces
// ✅ always use strip()
"\u2000hello\u2000".strip() // correct

// ❌ Creating HttpClient per request — expensive
void callApi() {
    HttpClient client = HttpClient.newHttpClient(); // new client every call!
}
// ✅ Create once, reuse
static final HttpClient CLIENT = HttpClient.newHttpClient();

// ❌ Expecting HttpClient to throw on 4xx/5xx
HttpResponse<String> r = client.send(req, BodyHandlers.ofString());
// no exception even if status is 500!
// ✅ Always check status
if (r.statusCode() >= 400) throw new RuntimeException("HTTP " + r.statusCode());

// ❌ isEmpty() instead of isBlank() for whitespace check
" ".isEmpty()  // false — wrong
" ".isBlank()  // true  — correct
```

---