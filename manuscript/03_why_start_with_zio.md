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
//   eval = zio.ZIOCompanionVersionSpecific$$Lambda$2086/0x0000000100a7ec40@4f3a8e3e
// )
```

## Edit This Chapter
[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/03_why_start_with_zio.md)
