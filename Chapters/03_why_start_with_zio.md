# Superpowers

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

```scala mdoc
object Demo extends ZIOAppDefault:
  def run = ZIO.debug("Hi")

Demo.run
```
