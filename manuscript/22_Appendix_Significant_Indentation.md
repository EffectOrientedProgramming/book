# Appendix: Significant Indentation
We have taken a bit of a risk with the style used in this book.
We are embracing significant indentation nearly to the max.
These syntastic features were some of the most contentious changes from Scala 3

## Motivations
- Reduce the space taken up by small demos
  - Often, closing parens/braces would *double* the vertical space
- Avoid getting lost scanning a long line

## Concessions / Acknowledgements
- This style will be new/unfamiliar to many programmers - even seasoned Scala 2 users!
- The syntax is not yet fully supported by all editors // TODO Confirm this
- Sometimes this style actually *increases* the vertical space

## Rules / examples
- Generally, when providing a block as an argument, use a colon and place the argument on the following line
```scala
def sendMessage(msg: String) =
  println("Sent: " + msg)
```

```scala
sendMessage:
  val name     = "Alice"
  val greeting = "Hello"
  s"$greeting, $name"
// Sent: Hello, Alice
```

However, when the function accepts:

- Multiple parameters
- Multiple lists of parameters

We use parentheses to group/collect/etc the arguments

```scala
multiply(7, 6)
// res1: Int = 42
```

This is necessary, because the alternative would just be evaluated as one block.
The first parameter is flagged as an unused value, and we only provide the 2nd parameter to the function.

```scala
multiply:
  7
  6
// error:
// A pure expression does nothing in statement position; you may be omitting necessary parentheses
//         "Hello"
//      ^
```


## Edge cases that are difficult to defend
```scala
runDemo:
  defer:
    ZIO.debug("Hello").run
    ZIO.debug("World").run
// Hello
// World
// ()
```
VS
```scala
runDemo:
  defer:
    ZIO
      .debug:
        "Hello"
      .run
    ZIO
      .debug:
        "World"
      .run
// Hello
// World
// ()
```


## Edit This Chapter
[Edit This Chapter](https://github.com/EffectOrientedProgramming/book/edit/main/Chapters/22_Appendix_Significant_Indentation.md)
