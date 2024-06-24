# Preface

*Effects* are the unpredictable parts of a system.
Traditional programs do not isolate these unpredictable parts, making it hard to manage them.
*Effect Systems* partition the unpredictable parts and manage them separately from the predictable ones.

With Effect Systems, developers can more easily build systems that are reliable, resilient, testable, and most importantly, *extensible*.

*Effect Oriented Programming* is a new paradigm for programming with Effect Systems.

Many programming languages do not have an Effect System.
Some languages have built-in support for managing Effects while others have support through libraries.
New languages that incorporate Effect management include OCaml, Unison, and Roc.

## This book is for...

We focus on the concepts of Effect Systems, rather than language and library specifics.
Since Effect Systems are a new and emerging paradigm, we have limited choices.
In this book, we use Scala 3, which has several Effect System libraries including Cats Effects and Kyo.
These libraries (and others) have all contributed to the understanding of Effect Systems and how they are used.
We chose the ZIO library for this book because of both our satsifaction with it, and our experience---one 
  author (Bill) was an engineer at Ziverge, the company that created and maintains ZIO, for several years.

If you are using a different language, the concepts of Effect Systems may only be useful when your language or a library supports them.
However, learning the concepts will help prepare you.

While Scala knowledge is not required to learn the concepts, this book assumes you are familiar with:

- Functions
- Strong static typing
- Chaining operations on objects (`"asdf ".trim.length`)

## Code examples

The code examples are available at: [github.com/EffectOrientedProgramming/examples](https://github.com/EffectOrientedProgramming/examples)

The code in this book uses a Scala 3 language syntax that might be unfamiliar, even to Scala developers.
Since our focus is on the concepts of Effect Oriented Programming, we've tried to make the code examples very readable, even on mobile devices.
To accomplish this, when functions have single parameters we generally use Scala 3's *significant indentation*:

```scala 3 mdoc:compile-only
import zio.*
import zio.direct.*

Console.printLine:
  "hello, world"
```

The argument for `Console.printLine` is on a new line instead of using parentheses, as in: `Console.printLine("hello, world")`.
The colon (`:`) indicates that the function call uses significant indentation.
For multi-parameter functions and in cases where the single argument is very short and does not contain nested function calls, we use the traditional syntax:

```scala 3 mdoc:compile-only
import zio.*
import zio.direct.*

Console.printLine(1)
```

## Acknowledgements

Kit Langton, for being a kindred spirit in software interests and an inspiring contributor to the open source world.

Wyett Considine, for being an enthusiastic intern and initial audience.

Hali Frasure, for cooking so many dinners and facilitating our book nights generally.
