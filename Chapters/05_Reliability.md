# Reliability

> A reliable system does not break.

If you've been programming for a while, this sounds far-fetched or even impossible.

Most existing languages are built for rapid development.
You create a system as quickly as possible, then begin isolating areas of failure, finding and fixing bugs until the system is tolerable and can be delivered.
Throughout the lifetime of the system, bugs are regularly discovered and fixed.
There is no realistic expectation that you will ever achieve a completely bug-free system, just one that seems to work well enough to meet the requirements.
This is the reality we have come to accept as programmers.

If each piece of a traditional system is unreliable, when you combine these pieces you get a multiplicative effect -- the resulting parts are significantly less reliable than their component pieces.
[[[This seems right but it seems like there's probably some kind of existing analysis we can cite. Or something that shows that it's wrong.]]]
[[[I suspect we could get away with some basic statistics here, eg. If functionA has a 99% success rate, and
functionB has a 99% success rate, calling both of them has a (.99 * .99)% chance of succeeding.]]]

What if we could change our thinking around the problem of building software systems?
Imagine building small pieces that can each be reasoned about and made rock-solid.
Now suppose there is a way to combine these reliable pieces to make bigger parts that are just as reliable.
Each time you combine smaller parts to create a bigger part, the result inherits the reliability of its components.
Instead of multiplying unreliability, you combine reliability.
The resulting system is as reliable as any of its components.

This is what *functional programming* together with *effects management* can achieve.
This is what we want to teach you in this book.

The biggest impact on you as a programmer is the requirement for patience.
With most languages, the first thing you want to do is figure out how to write "Hello, World!" as quickly as possible, then start accumulating the other language features as standalone concepts.
In functional programming we start by examining the impact of each concept on reliability.
Eventually we combine the smaller concepts, again ensuring reliability at each step.
This mathematical rigor is what ultimately produces a reliable system.

This takes patience.

It can seem like a painfully long process before you can begin writing working programs. Most of us are used to the more
immediate feedback and satisfaction of getting something working, so this can be challenging. But would you rather
create an unreliable system quickly? Presumably you do not, otherwise you wouldn't be reading this book.


---

A reliable system isolates parts that are always the same (pure functions)
from the parts that can change (effects).
