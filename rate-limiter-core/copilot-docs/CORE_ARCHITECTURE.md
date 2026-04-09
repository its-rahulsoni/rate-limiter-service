Here you go — **clean `.md` file content** with architecture + Copilot prompts, following your preferred formatting style 👇

---

# CORE_ARCHITECTURE.md

## 🟢 1. Objective

Build a **stateless, pluggable rate limiting core module** that:

* Supports multiple algorithms
* Uses **Redis + Lua** for state management
* Exposes a clean API to web module
* Starts with **Token Bucket implementation**

---

## 🟢 2. High-Level Architecture

```text id="coreflow1"
Web Module
   ↓
RateLimiter (Core API)
   ↓
RateLimiterService
   ↓
AlgorithmFactory
   ↓
TokenBucketAlgorithm
   ↓
LuaScriptExecutor
   ↓
Redis
   ↓
RateLimitResult
```

---

## 🟢 3. Module Components

---

### 🔹 3.1 API Layer

```text id="api1"
RateLimiter (interface)
    → allow(key, rule)
```

---

### 🔹 3.2 Service Layer

```text id="service1"
RateLimiterService
    → delegates to algorithm
```

---

### 🔹 3.3 Model Layer

```text id="model1"
RateLimiterRule
RateLimitResult
```

---

### 🔹 3.4 Algorithm Layer

```text id="algo1"
RateLimitingAlgorithm
TokenBucketAlgorithm (initial)
```

---

### 🔹 3.5 Factory Layer

```text id="factory1"
AlgorithmFactory
    → returns algorithm based on type
```

---

### 🔹 3.6 Redis Layer

```text id="redis1"
RedisClient (Lettuce)
LuaScriptExecutor
```

---

### 🔹 3.7 Script Layer

```text id="script1"
token_bucket.lua
```

---

### 🔹 3.8 Utility Layer

```text id="util1"
KeyBuilder
```

---

## 🟢 4. Core Flow

```text id="coreflow2"
1. Web module calls RateLimiter.allow(key, rule)
2. RateLimiterService receives request
3. AlgorithmFactory selects algorithm
4. TokenBucketAlgorithm executes logic
5. LuaScriptExecutor runs script in Redis
6. Redis returns result
7. Result mapped to RateLimitResult
8. Response returned to web module
```

---

## 🟢 5. Redis Key Strategy

```text id="rediskey1"
rate_limit:{rule}:{key}
```

---

## 🟢 6. MVP Scope

### ✅ Implement

```text id="mvp1"
RateLimiter interface
RateLimiterService
TokenBucketAlgorithm
LuaScriptExecutor
Redis integration
RateLimitResult
KeyBuilder
```

---

### ❌ Skip (for now)

```text id="mvp2"
Other algorithms
Advanced metrics
Complex configs
```

---

## 🟢 7. Package Structure

```text id="pkg1"
core
 ├── api
 ├── service
 ├── model
 ├── algorithm
 │     └── tokenbucket
 ├── factory
 ├── redis
 ├── script
 └── util
```

---

# 🟡 Part 2: Copilot Prompts

---

## 🔥 Prompt 1 — Core Interface

```text id="cp1"
Create RateLimiter interface.

Requirements:
- Method:
    allow(String key, RateLimiterRule rule)
- Return RateLimitResult
- Keep interface simple and extensible
```

---

## 🔥 Prompt 2 — RateLimitResult

```text id="cp2"
Create RateLimitResult model.

Fields:
- boolean allowed
- long remaining
- long retryAfter
- long capacity

Ensure immutability and clean design.
```

---

## 🔥 Prompt 3 — Algorithm Interface

```text id="cp3"
Create RateLimitingAlgorithm interface.

Method:
- allow(String key, RateLimiterRule rule)

Return RateLimitResult.
```

---

## 🔥 Prompt 4 — Token Bucket Algorithm

```text id="cp4"
Implement TokenBucketAlgorithm.

Responsibilities:
- Build Redis key using KeyBuilder
- Call LuaScriptExecutor
- Pass parameters:
    capacity
    refillRate
    current timestamp
- Parse Redis response into RateLimitResult
```

---

## 🔥 Prompt 5 — Algorithm Factory

```text id="cp5"
Create AlgorithmFactory.

Responsibilities:
- Map algorithm type to implementation
- Return correct RateLimitingAlgorithm
- Support future extensibility
```

---

## 🔥 Prompt 6 — RateLimiterService

```text id="cp6"
Implement RateLimiterService.

Responsibilities:
- Accept key and rule
- Use AlgorithmFactory to select algorithm
- Delegate execution
- Return RateLimitResult
```

---

## 🔥 Prompt 7 — KeyBuilder

```text id="cp7"
Create KeyBuilder utility.

Method:
- build(String rule, String key)

Format:
- rate_limit:{rule}:{key}

Ensure safe key formatting.
```

---

## 🔥 Prompt 8 — Redis Integration

```text id="cp8"
Integrate Redis using Lettuce.

Requirements:
- Create Redis client configuration
- Support script execution
- Handle connection and errors properly
```

---

## 🔥 Prompt 9 — Lua Script Executor

```text id="cp9"
Create LuaScriptExecutor.

Responsibilities:
- Load Lua script
- Execute script with keys and args
- Return parsed result
```

---

## 🔥 Prompt 10 — Token Bucket Lua Script

```text id="cp10"
Write Lua script for Token Bucket algorithm.

Logic:
- Refill tokens based on elapsed time
- Cap at capacity
- If tokens >= 1 → allow and decrement
- Else → reject
- Return:
    allowed
    remaining tokens
    retryAfter
```

---

## 🔥 Prompt 11 — Validation

```text id="cp11"
Review implementation and verify:

- Correct Redis key generation
- Correct algorithm selection
- Lua script correctness
- Thread safety
- Clean architecture
```

---

## 🔥 Prompt 12 — Unit Tests

```text id="cp12"
Write unit tests for:

- TokenBucketAlgorithm
- KeyBuilder
- RateLimiterService

Mock Redis interactions.
```

---

# 🧠 Final Insight

```text id="finalcore"
Core module = execution engine
Web module = orchestration layer
Redis = state store
Lua = atomic logic
```

---

# 🚀 Next Step

Implement Token Bucket → validate → extend to other algorithms
