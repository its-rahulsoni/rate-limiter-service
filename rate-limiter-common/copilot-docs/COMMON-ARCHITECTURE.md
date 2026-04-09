Perfect 👍 — this is a **clean, scalable decision**.
Creating a `rate-limiter-common` module will keep your architecture **decoupled and future-proof**.

Here’s a **well-structured `.md` file** with Copilot prompts for this migration 👇

---

# COMMON_MODULE_MIGRATION.md

## 🟢 Part 1: Objective

Refactor the project to introduce a new module:

```text
rate-limiter-common
```

This module will contain **shared models, enums, and contracts** used by both:

* `rate-limiter-web`
* `rate-limiter-core`

---

## 🟢 Part 2: What Should Move to Common Module

---

### 🔹 Models

```text
RateLimiterRule
RateLimitResult
```

---

### 🔹 Enums

```text
AlgorithmType
KeyType
RateLimiterMode
```

---

### 🔹 Interfaces (if shared)

```text
RateLimiter (optional, if used by web)
```

---

### 🔹 Constants (optional)

```text
RateLimitRules (rule name constants)
```

---

## ❌ What Should NOT Move

```text
Redis logic
Lua scripts
Spring components
Interceptors / Aspects
```

---

## 🟢 Part 3: Final Dependency Structure

```text
rate-limiter-common
        ↑
   ┌────┴────┐
   ↓         ↓
rate-limiter-core
rate-limiter-web
```

---

## 🟢 Part 4: Copilot Prompts

---

### 🔥 Prompt 1 — Create Common Module

```text
Create a new module named rate-limiter-common.

Requirements:
- Plain Java module (no Spring Boot)
- Contains shared models and enums
- Clean package structure:
    com.example.ratelimiter.common
```

---

### 🔥 Prompt 2 — Move Models

```text
Move shared models to rate-limiter-common module.

Move:
- RateLimiterRule
- RateLimitResult

Requirements:
- Update package names
- Ensure no Spring dependencies
- Keep models immutable where possible
```

---

### 🔥 Prompt 3 — Move Enums

```text
Move enums to rate-limiter-common module.

Move:
- AlgorithmType
- KeyType
- RateLimiterMode

Ensure:
- Clean enum definitions
- No module-specific dependencies
```

---

### 🔥 Prompt 4 — Move Constants (Optional)

```text
Create a constants class in common module.

Example:
- RateLimitRules

Purpose:
- Avoid hardcoded strings in annotations
```

---

### 🔥 Prompt 5 — Update Core Module

```text
Update core module to use common module.

Requirements:
- Replace local models with imports from common module
- Remove duplicate classes
- Ensure all algorithms use common models
```

---

### 🔥 Prompt 6 — Update Web Module

```text
Update web module to use common module.

Requirements:
- Replace local models with common module imports
- Update ConfigProvider, Interceptor, and Aspect
- Ensure compatibility with existing logic
```

---

### 🔥 Prompt 7 — Add Dependencies

```text
Update build configuration to include common module.

Requirements:
- Add dependency:
    core → common
    web → common
- Ensure no circular dependency exists
```

---

### 🔥 Prompt 8 — Refactor Imports

```text
Refactor all imports across project.

Requirements:
- Replace old model imports with common module imports
- Ensure consistent package usage
- Remove unused imports
```

---

### 🔥 Prompt 9 — Cleanup

```text
Clean up duplicated code.

Requirements:
- Remove old model classes from core and web
- Ensure only one source of truth exists (common module)
```

---

### 🔥 Prompt 10 — Validation

```text
Validate the refactoring.

Check:
- No circular dependencies
- All modules compile successfully
- Models are correctly shared
- No missing imports
```

---

### 🔥 Prompt 11 — Unit Tests Fix

```text
Update unit tests after refactoring.

Requirements:
- Fix imports
- Ensure tests use common models
- Validate no regression
```

---

## 🟢 Part 5: Manual Sanity Checklist

* [ ] Common module created
* [ ] Models moved
* [ ] Enums moved
* [ ] No duplicate classes
* [ ] Core depends only on common
* [ ] Web depends only on common
* [ ] No circular dependency

---

## 🚨 Common Mistakes

* Moving Spring classes to common ❌
* Keeping duplicate models ❌
* Incorrect dependency direction ❌

---

## 🎯 Final Insight

```text
Common module = shared language
Core module = execution engine
Web module = orchestration layer
```

---

## 🚀 Next Step

After this:

👉 Implement Token Bucket in core module using shared models
