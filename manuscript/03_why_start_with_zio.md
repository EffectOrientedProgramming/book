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

```scala
runDemo(ZIO.debug("Hi"))
// Hi
// ()
```

```scala
object Demo extends ZIOAppDefault:
  def run = ZIO.debug("Hi")

Demo.run
// res1: ZIO[ZIOAppArgs & Scope, Any, Any] = Sync(
//   trace = "repl.MdocSession.MdocApp.Demo.run(03_why_start_with_zio.md:15)",
//   eval = zio.ZIOCompanionVersionSpecific$$Lambda$2024/0x0000000100a3c440@6870e039
// )
```
