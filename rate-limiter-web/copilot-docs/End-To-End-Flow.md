Explain the complete end-to-end flow of the rate-limiter-web module.

Context:
- The module is built using Spring Boot
- It supports both interceptor-based and annotation + AOP-based rate limiting
- Configuration is loaded from application.yml
- Rules are defined as:
    - Map<String, RateLimiterRule> rules (for annotation-based usage)
    - List<RateLimiterRule> global-rules (for interceptor-based usage)
- A "mode" flag controls whether INTERCEPTOR, ANNOTATION, or BOTH are active

What I want in the explanation:

1. High-level flow:
    - From incoming HTTP request → final response
    - Clearly show where interceptor and aspect come into play

2. Component-wise breakdown:
    - Controller
    - Interceptor
    - Aspect
    - KeyResolver / KeyResolverFactory
    - ConfigProvider
    - RateLimiter
    - Redis (if applicable)

3. Two separate flows:
    - Interceptor-based rate limiting
    - Annotation + AOP-based rate limiting

4. Explain how the system decides:
    - Which rule to apply
    - Which key to extract (IP, HEADER, etc.)
    - When to skip rate limiting

5. Include:
    - Step-by-step execution sequence
    - Clear data flow (what gets passed where)
    - Example request walkthrough

6. Highlight:
    - Thread safety considerations
    - Config-driven behavior
    - Differences between in-memory vs Redis approach (if relevant)

7. Keep the explanation:
    - Structured (sections + steps)
    - Concise but deep
    - Focused on understanding, not just code

Do not generate code unless necessary for explanation.