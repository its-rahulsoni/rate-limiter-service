Perfect — now you’re moving toward a **complete, flexible rate limiting framework** 🔥

We’ll cover **two things clearly**:

1. 📄 `.md` file → Instructions + Copilot prompts for **Annotation + AOP**
2. 🧠 How consumer chooses between **Interceptor vs Annotation vs AOP** (VERY IMPORTANT)

---

# RateLimiterAnnotationAspect.md

## 🟢 Part 1: Objective

Extend the rate limiter system to support:

* **Annotation-based rate limiting**
* **Aspect-Oriented Programming (AOP) enforcement**

This enables **fine-grained, per-endpoint control** in addition to global interceptor-based limiting.

---

## 🟢 Part 2: Implementation Instructions (for Copilot)

---

### 🧩 Step 1: Create Custom Annotation

Define annotation:

```text
@RateLimit
```

---

### 📌 Annotation should support:

```text
- keyType
- limit
- window
- algorithm
- headerName (optional)
```

---

### 🧩 Step 2: Define Annotation Structure

* Use `@Target(ElementType.METHOD)`
* Use `@Retention(RetentionPolicy.RUNTIME)`

---

### 🧩 Step 3: Enable AOP

* Add `@EnableAspectJAutoProxy` in configuration
* Ensure Spring AOP dependency is present

---

### 🧩 Step 4: Create RateLimitAspect

Responsibilities:

* Intercept methods annotated with `@RateLimit`
* Extract annotation values
* Resolve key using KeyResolver
* Call RateLimiter
* Block execution if limit exceeded

---

### 🧩 Step 5: Implement Around Advice

```text
@Around("@annotation(rateLimit)")
```

---

### 🧩 Step 6: Extract HttpServletRequest

* Use `RequestContextHolder`
* Get current request

---

### 🧩 Step 7: Resolve Key

* Use KeyResolverFactory
* Based on annotation.keyType

---

### 🧩 Step 8: Call RateLimiter

```text
allowRequest(key, config)
```

---

### 🧩 Step 9: Handle Rejection

* Throw exception OR return error response
* Suggested: throw custom exception

---

### 🧩 Step 10: Create Exception Handler

* Handle rate limit exception globally
* Return HTTP 429

---

---

## 🟢 Part 3: Copilot Prompts

---

### 🔥 Prompt 1 — Create Annotation

```text
Create a custom annotation @RateLimit.

Requirements:
- Applicable on methods
- Retained at runtime
```

---

### 🔥 Prompt 2 — Enable AOP

```text
Enable Spring AOP in the project.

Requirements:
- Add necessary dependency
- Enable proxy support using @EnableAspectJAutoProxy
```

---

### 🔥 Prompt 3 — Create Aspect

```text
Create a RateLimitAspect.

Requirements:
- Intercept methods annotated with @RateLimit
- Use @Around advice
- Extract annotation values
- Integrate with KeyResolverFactory
- Call RateLimiter
```

---

### 🔥 Prompt 4 — Extract Request

```text
Inside the aspect, extract HttpServletRequest.

Use RequestContextHolder to access current request.
Ensure null safety.
```

---

### 🔥 Prompt 5 — Apply Rate Limiting

```text
Implement logic inside aspect:

- Resolve key based on annotation keyType
- Create RateLimitRule from annotation
- Call RateLimiter
- If not allowed:
    throw RateLimitExceededException
```

---

### 🔥 Prompt 6 — Exception Handling

```text
Create RateLimitExceededException and global exception handler.

Requirements:
- Return HTTP 429
- Return JSON response:
  { "message": "Rate limit exceeded" }
```

---

### 🔥 Prompt 7 — Sample Usage

```text
Create sample controller using @RateLimit.

Example:
- Apply IP-based rate limit
- Apply header-based rate limit

Ensure aspect is triggered correctly.
```

---

### 🔍 Prompt 8 — Validation

```text
Review AOP implementation and verify:

- Annotation is detected correctly
- Aspect intercepts method execution
- Key resolution works
- RateLimiter is invoked
- Exception handling works
```

---

### 🧪 Prompt 9 — Unit Tests

```text
Write unit tests for:

- RateLimitAspect
- Annotation processing
- Exception handling

Mock dependencies where needed.
```

---

# 🧠 Part 4: How All 3 Approaches Work Together

---

## 🔥 The Core Problem You Asked

> If interceptor is global… when will annotation/AOP run?

---

# 🧠 1. Execution Order

```text
Client
   ↓
Interceptor  ⭐ (always first)
   ↓
Controller method
   ↓
Aspect (@RateLimit) ⭐
   ↓
Business logic
```

---

# ⚠️ 2. Important Insight

👉 **Interceptor ALWAYS runs first**

👉 AOP runs **only if method executes**

---

# 🔥 3. So Do They Conflict?

👉 YES — if both are active blindly

---

# 🎯 4. How to Let Consumer Choose

You need **configurable strategy selection**

---

## ✅ Option 1 (Recommended)

### Add config:

```yaml
rateLimiter:
  mode: INTERCEPTOR | ANNOTATION | BOTH
```

---

## Behavior:

---

### 🔹 INTERCEPTOR

```text
Only global rate limiting
Annotation ignored
```

---

### 🔹 ANNOTATION

```text
Interceptor disabled
Only @RateLimit used
```

---

### 🔹 BOTH

```text
Interceptor + Annotation both apply
```

---

---

# 🧩 5. Implementation Strategy

---

## 🔹 Interceptor

```text
Check mode before applying logic
```

---

## 🔹 Aspect

```text
Check mode before applying logic
```

---

---

# 🧠 6. Example

---

### Mode = INTERCEPTOR

```text
All requests rate limited globally
```

---

### Mode = ANNOTATION

```text
Only annotated endpoints limited
```

---

### Mode = BOTH

```text
Global + per-endpoint limits
```

---

# 🔥 7. Best Practice Recommendation

---

## For your project:

👉 Default:

```text
ANNOTATION
```

---

## Why?

* Fine-grained control
* Clean design
* Industry usage

---

# 🧠 8. Final Mental Model

* Interceptor = **global guard**
* Annotation = **specific rules**
* AOP = **execution engine**

---

# 🎯 Final Answer

* Yes, you need all 3 approaches
* Interceptor = global
* Annotation + AOP = fine-grained
* Use config to let consumer choose

---

# 🚀 Next Step

If you want:

👉 I can help you:

* implement `mode` switching cleanly
* avoid duplicate rate limiting
* design production-grade AOP + interceptor coexistence

Just say 👍
