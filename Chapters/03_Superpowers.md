# Superpowers

1. The superpowers are the strategies to deal with unpredictability 
1. OpeningHook (1-6)
    1. Note: Progressive enhancement through adding capabilities 
1. Concurrency
    1. Race
    1. Hedge (to show the relationship to OpeningHook ie progressive enhancement)
1. Sequential
    1. ZIO Direct
    1. Note: Combine OpeningHook & Concurrency with ZIO Direct
1. And so much more
    1. Note: And there are many capabilities you might want to express. In the future we will dive into these other capabilities.

- Racing
- Timeout
- Error-handling
  - Fallback
  - Retry
- Repeat
- Parallelism
- Resource Safety
- Mutability that you can trust
- Human-readable
- Cross-cutting Concerns / Observability / Regular Aspects
  - timed
  - metrics
  - debug
  - logging

# Underlying
- Composability
- Success VS failure
- Interruption/Cancellation
- Fibers
- Processor Utilization
  - Fairness
  - Work-stealing
- Resource Control/Management
- Programs as values

```scala mdoc
runDemo(ZIO.debug("Hi"))
```

