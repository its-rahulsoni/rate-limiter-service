--[[
Token Bucket Rate Limiting Lua Script for Redis

Implements a distributed, atomic token bucket algorithm for rate limiting.

Argument contract:
  KEYS[1]   - Redis key for the bucket
  ARGV[1]   - capacity (max tokens, integer > 0)
  ARGV[2]   - refill_rate (tokens per second, float > 0)
  ARGV[3]   - now (current timestamp in seconds, integer > 0)

Return contract (for observability and monitoring):
  { allowed (0/1), remaining_tokens (int), retry_after (seconds), capacity, now }
    allowed         - 1 if request allowed, 0 if rate limited
    remaining_tokens- integer tokens left after this request
    retry_after     - seconds until next token is available (0 if allowed)
    capacity        - bucket capacity
    now             - current timestamp (seconds)

All time units are in seconds. Fractional tokens are used internally for refill math, but only integer tokens are stored and returned.
]]

-- =====================
-- Input Parsing & Validation
-- =====================
local bucket_key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

-- Strong input validation is critical in production systems to prevent runtime errors (e.g., division by zero)
-- and to ensure deterministic, safe behavior even if misconfigured or called with bad arguments.
if capacity == nil or capacity <= 0 or refill_rate == nil or refill_rate <= 0 then
    return {0, 0, -1, capacity or 0, now or 0}
end

-- =====================
-- State Fetch
-- =====================
local state = redis.call('HMGET', bucket_key, 'tokens', 'last_refill')
local tokens = tonumber(state[1])
local last_refill = tonumber(state[2])
if tokens == nil then tokens = capacity end
if last_refill == nil then last_refill = now end

local state_changed = false
local prev_tokens = tokens
local prev_last_refill = last_refill

-- =====================
-- Refill Logic
-- =====================
-- Prevent negative elapsed time due to clock drift or time inconsistencies in distributed systems
local elapsed = now - last_refill
if elapsed < 0 then
    -- In distributed systems, clocks may drift or be inconsistent between nodes.
    -- To avoid negative refill or token calculation, treat negative elapsed as 0 and reset last_refill to now.
    elapsed = 0
    last_refill = now
end
local refill = elapsed * refill_rate
if refill > 0 then
    tokens = tokens + refill
    last_refill = now
    state_changed = true
end
-- Ensure tokens never exceed capacity (even with large refill or burst traffic)
if tokens > capacity then tokens = capacity end
-- Ensure tokens never go below 0 (even with burst traffic or rounding)
if tokens < 0 then tokens = 0 end

-- =====================
-- Consumption Logic
-- =====================
local allowed = 0
local retry_after = 0
if tokens >= 1 then
    allowed = 1
    tokens = tokens - 1
    retry_after = 0
    state_changed = true
else
    allowed = 0
    -- Calculate retry_after based on token deficit
    -- deficit = 1 - tokens (tokens may be fractional)
    -- retry_after = time (in seconds) to accumulate enough tokens for 1 request
    local deficit = 1 - tokens
    if refill_rate > 0 then
        retry_after = math.ceil(math.max(0, deficit / refill_rate))
    else
        retry_after = 0 -- fallback: no refill possible
    end
    -- retry_after is always >= 0, and uses math.ceil for user-friendly value
end
-- Ensure tokens never go below 0 after consumption (final safeguard)
if tokens < 0 then tokens = 0 end
-- Ensure tokens never exceed capacity after all operations (final safeguard)
if tokens > capacity then tokens = capacity end

-- =====================
-- Persistence (Write to Redis)
-- =====================
-- Calculate TTL (expiry) based on refill time
-- refill_time = capacity / refill_rate (time to fully refill the bucket)
-- ttl = ceil(refill_time * 2), but always at least 1 and at most 3600 (1 hour)
local refill_time = capacity / refill_rate
local ttl = math.ceil(refill_time * 2)
if ttl < 1 then ttl = 1 end
if ttl > 3600 then ttl = 3600 end
-- This ensures the key expires after a period of inactivity, but not too soon or too late.

-- Only write to Redis if state changed (tokens or last_refill updated)
if state_changed or tokens ~= prev_tokens or last_refill ~= prev_last_refill then
    -- Store only floored integer tokens to avoid floating point drift
    redis.call('HMSET', bucket_key, 'tokens', math.floor(tokens), 'last_refill', last_refill)
    redis.call('EXPIRE', bucket_key, ttl)
end

-- Performance benefit: By avoiding unnecessary writes, we reduce Redis CPU and network usage, especially under high contention or when requests are rejected without state change.

-- =====================
-- Return (Observability)
-- =====================
-- Return structure for observability and monitoring:
-- { allowed (0/1), remaining_tokens (int), retry_after (seconds), capacity, current timestamp (now) }
-- All values are consistent and meaningful for logging/monitoring.
return {allowed, math.floor(tokens), retry_after, capacity, now}
