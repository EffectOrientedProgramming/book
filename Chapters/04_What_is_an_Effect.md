# What is an Effect?

## Effects: The Impure World

An `Effect` is the term for any computational interaction with the world outside your CPU.
There are an infinite number of `Effects` that might need to be modeled in an application.
Unauthoritatively, the authors of this book consider them in these categories.

- Observing the World
- Changing the World
- Failures

TODO {{Explain these Effects: Optionality, Failure, Asynchronicity, Blocking}}

### Observing the World

Observation can be very basic:

- Accepting user input from the `Console`
- Getting the current time from the system `Clock`
- Taking the output of a `Random` number generator

ZIO provides built-in `Services` for many of these common programming tasks.
We will examine these pieces in detail, contrasting them with historic approaches, and highlighting some of their benefits.

However, ZIO-core does not try to cover all every possible situation.
Observation can also be arbitrarily complex and domain-specific:

- Sensing slippage in an anti-lock braking system
- Getting the current price of a stock
- Detecting the current passing through your heart for a pace-maker
- Checking the temperature of a nuclear reactor

We will explore situations like this

### Changing the World

Mirroring observations, changes can be basic:
- Printing to the `Console`
- Writing to a file
- Mutating a variable
- Saving to a `Database`

They can be advanced:
- Triggering an alarm
- Stabilizing an airplane
- Detonating explosives

One important aspect these things actions is that, generally, they cannot be undone.
It is possible to undo special cases, eg. provide Database deletion behavior

## Effects VS Side-Effects
`Effects` and `Side-Effects` are not slight verbiage preferences.
They are a fundamentally different way of modeling our programs.

`Side-effecting` code observes or changes the world in some way that is not apparent in the type signature.
`Effectful` code will signal this in the type signature.
If your 

## The Advent of ZIO
