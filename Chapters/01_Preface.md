# Preface

*Effects* are the unpredictable parts of systems.
Traditional programs do not isolate these unpredictable parts, making it hard to manage them.
*Effect Systems* partition the unpredictable parts and manage them separately from the predictable ones.

With Effect Systems, developers can more easily build systems that are reliable, resilient, and testable.

*Effect Oriented Programming* is a new paradigm for programming with Effect Systems.

Many bleeding-edge languages now have ways to manage Effects (e.g. OCaml, Unison, and Roc).
But not every programming language has an Effect System.
Some languages have built-in support for managing Effects while others have support through libraries.

## Who is the book is for

This book focuses on the concepts of Effect Systems, rather than language and library specifics.
Since Effect Systems are a new and emerging paradigm, we have limited choices.
We use Scala 3 and an Effect System library called ZIO to convey the concepts of Effect Oriented Programming.

If you use Scala, you can use an Effect System, of which there are a number of options (ZIO, Cats Effects, and Kyo).

If you are not using Scala, the concepts of Effect Systems may only be useful when your language or a library supports them.
However, learning the concepts now will help you prepare for that eventuality.

While Scala knowledge is not required to learn the concepts, this book assumes you are familiar with:

- Functions
- Strong static typing
- Chaining operations on objects (`"asdf ".trim.length`)

## Code examples

The code examples are available at: [github.com/EffectOrientedProgramming/examples](https://github.com/EffectOrientedProgramming/examples)

The code in this book uses a Scala 3 language syntax that might be unfamiliar, even to Scala developers.
Since our focus is on the concepts of Effect Oriented Programming we've tried to make the code examples very readable, even on mobile devices.
To accomplish this, when functions have single parameters we generally use Scala 3's Significant Indentation style.
For example:

```scala 3 mdoc:compile-only
Console.printLine:
  "hello, world"
```

The parameter to the `ZIO.debug` function is specified on a new line instead of the usual parens (`ZIO.debug("hello, world")`)
The colon (`:`) indicates that the function parameter will use the significant indentation syntax.
For multi-parameter functions and in cases where the single parameter is very short and does not contain nested function calls, we use the traditional syntax:

```scala 3 mdoc:compile-only
Console.printLine(1)
```

## Acknowledgements

Kit Langton, for being a kindred spirit in software interests and inspiring contributor to the open source world.

Wyett Considine, for being an enthusiastic intern and initial audience.

Hali Frasure, for cooking so many dinners and facilitating our book nights generally.
