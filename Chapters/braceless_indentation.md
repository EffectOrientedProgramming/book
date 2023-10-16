# Significant Indentation
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
```scala mdoc
def sendMessage(msg: String) =
  println("Sent: " + msg)
```

```scala mdoc
sendMessage:
  val name     = "Alice"
  val greeting = "Hello"
  s"$greeting, $name"
```

However, when the function accepts:

- Multiple parameters
- Multiple lists of parameters

We use parentheses to group/collect/etc the arguments 
```scala mdoc:invisible
def multiply(x: Int, y: Int) = x * y
```

```scala mdoc
multiply(7, 6)
```

This is necessary, because the alternative would just be evaluated as one block.
The first parameter is flagged as an unused value, and we only provide the 2nd parameter to the function.

```scala mdoc:fail
multiply:
  7
  6
```


## Edge cases that are difficult to defend
```scala mdoc
runDemo:
  defer:
    ZIO.debug("Hello").run
    ZIO.debug("World").run
```
VS
```scala mdoc
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
```
