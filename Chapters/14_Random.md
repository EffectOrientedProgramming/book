# Random

We _need_ randomness in many domains.

- Cryptography
- Simulations
- Adding noise to images for further processing
- Adding jitter to processes to prevent clashes

Randomness might seem antithetical to functional programming.
We want pure functions that always give us the same output for the same input!

This conflict highlights that Randomness is special - it is an effect.

It is easy to miss the significance of Randomness when writing software in other languages and paradigms.
You want a random integer? Just call the `randomInt` function and move on.
It probably doesn't seem any more unusual than calling `squareRoot` or `sin`.

We use pseudorandom algorithms to produce output that is sufficiently random for some applications.
These are initialized with a seed value that determines all the following output.
```scala mdoc
class MutableRNG(var seed: Int):

  def nextInt(): Int =
    seed = mangleNumber(seed)
    seed

  private def mangleNumber(input: Int): Int =
    // *NOT* good pseudorandom logic
    input * 52357 % 1000
```

```scala mdoc
val rng = MutableRNG(1)
rng.nextInt()
rng.nextInt()
rng.nextInt()
```
This is good enough for many situations, but is not random enough for security applications.
Let's see what happens if we make a new instance with the same seed.

```scala mdoc
val rngDuplicate = MutableRNG(1)
rngDuplicate.nextInt()
rngDuplicate.nextInt()
rngDuplicate.nextInt()
```
Exactly the same.
If an adversary is able to determine what seed is used in your application, they can predict the future to exploit your system.

## Physical RNGs
Consider a Random Number Generator (RNG) that operates by tossing a coin into the air and sending the result to the CPU.
Assuming good conditions, this is actually a good source of randomness.

Unfortunately, producing large numbers this way is slow and energy-consuming.
Optimistically you can create 1 random bit per second.
Further, you must power and maintain your coin-flipping and reading mechanism.

You can produce random data more efficiently by 

- monitoring voltage on a wire
- Reading weather sensor data
- Monitoring stock markets

You can even subscribe to services that combine all of these techniques to produce random data.

## Predictable Randomness
When your program treats randomness as an effect, testing unusual scenarios becomes straightforward.
You can preload "Random" data that will result in deterministic behavior.
ZIO gives you built-in methods to support this.

```scala mdoc:silent
import zio.test.TestRandom
TestRandom.feedBooleans
TestRandom.feedBytes
TestRandom.feedChars
TestRandom.feedDoubles
TestRandom.feedFloats
TestRandom.feedInts
TestRandom.feedLongs
TestRandom.feedStrings
TestRandom.feedUUIDs
```

If needed, you can also clear out these values by calling the various `clear` methods.

```scala mdoc:silent
TestRandom.clearBooleans
TestRandom.clearBytes
// etc ...
```