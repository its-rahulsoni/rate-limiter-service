Perfect — this is exactly how you should proceed 👌
We’ll build **rate-limiter-web (Spring Boot app)** first and keep it ready to plug into your core later.

Below is a **clean, Copilot-friendly `.md` file** in the same style you like.

---

# RateLimiterWebModule.md

## 🟢 Part 1: Objective

Develop the **rate-limiter-web module (Spring Boot application)** that:

* Accepts HTTP requests
* Applies rate limiting
* Uses file-based configuration
* Supports multiple key types (IP, Header, etc.)
* Is decoupled from core implementation (pluggable later)

---

## 🟢 Part 2: Implementation Instructions (for Copilot)

---

### 📌 Goal

Build a Spring Boot module that:

* Intercepts incoming requests
* Resolves rate limit key
* Fetches configuration
* Calls rate limiter (stub for now)

---

## 🧩 Step 1: Create Spring Boot Application

* Create main class with `@SpringBootApplication`
* Enable component scanning
* Ensure application runs independently

---

## 🧩 Step 2: Define Configuration (application.yml)

Create configuration structure:

```yaml
rateLimiter:
  rules:
    - keyType: IP
      limit: 100
      window: 60
      algorithm: SLIDING_WINDOW_LOG

    - keyType: HEADER
      headerName: X-API-KEY
      limit: 50
      window: 60
      algorithm: TOKEN_BUCKET
```

---

## 🧩 Step 3: Create Config Binding Classes

Copilot should:

* Create classes to map YAML → Java objects
* Use `@ConfigurationProperties`
* Load all rules into memory at startup

---

## 🧩 Step 4: Define KeyType Enum

Support:

```text
IP
HEADER
USER_ID
CUSTOM
```

---

## 🧩 Step 5: Implement KeyResolver System

Create:

* `KeyResolver` interface
* Implementations:

    * `IpKeyResolver`
    * `HeaderKeyResolver`

Responsibilities:

* Extract key from `HttpServletRequest`

---

## 🧩 Step 6: Create Resolver Factory

Copilot should:

* Map `KeyType → KeyResolver`
* Return correct resolver dynamically

---

## 🧩 Step 7: Create Config Provider

Create:

* `RateLimitConfigProvider`

Responsibilities:

* Return config based on rule
* Load rules from file (in-memory)

---

## 🧩 Step 8: Create RateLimiter Stub

For now:

* Create interface:

```text
allowRequest(String key, RateLimitConfig config)
```

* Return:

    * always allow (temporary)

---

## 🧩 Step 9: Implement Interceptor

Use:

* `HandlerInterceptor`

In `preHandle()`:

```text
1. Iterate rules
2. Resolve key using resolver
3. Fetch config
4. Call rate limiter
5. If rejected → return 429
```

---

## 🧩 Step 10: Register Interceptor

* Use `WebMvcConfigurer`
* Apply interceptor globally

---

## 🧩 Step 11: Create Sample Controller

Create:

* `/test` endpoint
* Return simple response

---

## 🔒 Step 12: Error Handling

* Return HTTP 429 for limit exceeded
* Include message:

    * “Rate limit exceeded”

---

## ⚠️ Step 13: Constraints

* Do NOT implement Redis yet
* Do NOT add core module dependency yet
* Keep design pluggable

---

## 🟢 Part 3: Copilot Prompts

---

### 🔥 Prompt 1 — Project Setup

```text
Create a Spring Boot application module named rate-limiter-web.

Requirements:
- Add main application class with @SpringBootApplication
- Enable component scanning
- Ensure project runs successfully
```

---

### 🔥 Prompt 2 — Configuration Setup

```text
Create configuration for rate limiter using application.yml.

Requirements:
- Define list of rules
- Each rule contains:
  - keyType (IP, HEADER, etc.)
  - limit
  - window
  - algorithm
  - optional headerName

Also create Java classes using @ConfigurationProperties to map this config.
```

---

### 🔥 Prompt 3 — KeyResolver System

```text
Implement KeyResolver system.

Requirements:
- Create KeyResolver interface
- Implement:
  - IpKeyResolver (extract client IP)
  - HeaderKeyResolver (extract header value)

Each resolver should take HttpServletRequest and return a key string.
```

---

### 🔥 Prompt 4 — Resolver Factory

```text
Create a factory that maps KeyType to corresponding KeyResolver.

Requirements:
- Return correct resolver dynamically
- Use Spring dependency injection
```

---

### 🔥 Prompt 5 — Config Provider

```text
Create RateLimitConfigProvider.

Requirements:
- Load rules from configuration
- Provide method to retrieve applicable rules
- Keep implementation in-memory
```

---

### 🔥 Prompt 6 — RateLimiter Stub

```text
Create RateLimiter interface.

Method:
- allowRequest(String key, RateLimitConfig config)

Create a stub implementation that always allows requests.
```

---

### 🔥 Prompt 7 — Interceptor

```text
Implement a HandlerInterceptor for rate limiting.

In preHandle():
- Iterate through all rules
- Resolve key using KeyResolver
- Fetch config
- Call RateLimiter
- If request is rejected:
    return HTTP 429

Ensure clean and readable implementation.
```

---

### 🔥 Prompt 8 — Register Interceptor

```text
Register the rate limiting interceptor using WebMvcConfigurer.

Apply it globally to all endpoints.
```

---

### 🔥 Prompt 9 — Controller

```text
Create a sample REST controller.

Endpoint:
- GET /test

Return:
- "Request successful"
```

---

### 🔍 Prompt 10 — Validation

```text
Review the implementation and verify:

1. Configuration is loaded correctly
2. KeyResolvers work for IP and HEADER
3. Interceptor is applied globally
4. RateLimiter is invoked correctly
5. HTTP 429 is returned when needed

Suggest improvements.
```

---

### ⚠️ Prompt 11 — Edge Cases

```text
Analyze edge cases:

1. Missing header
2. Null or empty key
3. No matching rule
4. Multiple rules conflict
5. Invalid config values

Suggest handling strategies.
```

---

### 🧪 Prompt 12 — Unit Tests

```text
Write unit tests for:

1. KeyResolver implementations
2. ConfigProvider
3. Interceptor logic

Use JUnit and Mockito.
```

---

## 🧠 Part 4: Manual Sanity Checklist

* Config loads correctly
* Key extraction works
* Interceptor triggers
* No Redis dependency
* Clean separation of concerns

---

## 🚨 Common Mistakes

* Mixing resolver logic inside interceptor ❌
* Hardcoding config ❌
* Tight coupling with rate limiter ❌
* Not handling missing keys ❌

---

## 🎯 Key Insight

> You are building the **entry + orchestration layer first**,
> keeping rate limiting logic pluggable for the core module later.

---

## 🚀 Recommended Workflow

1. Setup Spring Boot app
2. Add config + binding
3. Build KeyResolver system
4. Add interceptor
5. Add stub limiter
6. Test end-to-end
7. Later → plug core module

---

If you want next:
👉 I can give you **expected package structure (very clean)**
👉 Or help you **plug this with core module seamlessly later**


---
---
Suggestions for Improvement
Thread Safety for HeaderKeyResolver: The current implementation sets the header name on the shared HeaderKeyResolver bean before each use. This is not thread-safe.
Solution: Refactor so that the header name is passed as a parameter to the resolver, or use a stateless resolver.
Null/Empty Key Handling: The interceptor skips rules if the resolved key is null or empty. This is correct, but consider logging these cases for easier debugging.
Multiple Rules: All rules are applied to each request. If any rule fails, the request is rejected. This is a reasonable default, but document this behavior clearly.
Extensibility: The design is pluggable and ready for future enhancements (e.g., Redis, user ID, custom keys).
Testing: Add unit tests for:
KeyResolver implementations
ConfigProvider
Interceptor logic
Validation: Add validation for configuration values (e.g., positive integers for limits and windows, non-null keyType).

---
🧠 8. Copilot Prompts for Fixes
🔥 Prompt — Fix Thread Safety
Refactor KeyResolver implementation to make it stateless.

Requirements:
- Remove any mutable state (e.g., headerName field)
- Pass RateLimiterRule as parameter to resolveKey()
- Ensure thread safety
- Update all implementations accordingly
  🔥 Prompt — Add Logging
  Add logging to interceptor for skipped rules.

Cases:
- key is null
- key is empty

Log rule type and reason for skipping.
🔥 Prompt — Add Validation
Add validation to RateLimiterRule.

Requirements:
- limit must be > 0
- window must be > 0
- keyType must not be null
- headerName required for HEADER type

Use annotation-based validation where possible.
🔥 Prompt — Add Unit Tests
Write unit tests for:

1. KeyResolver implementations
2. RateLimitConfigProvider
3. RateLimiterInterceptor

Use JUnit and Mockito.
Mock RateLimiter where needed.