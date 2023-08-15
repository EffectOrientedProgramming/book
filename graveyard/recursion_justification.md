TODO This should be turned into a blog post, as it is no longer required by the style we are trying to teach.
## Immutability During Repetition

{{ This might be moved somewhere else... }}

Many functional programming tutorials begin by introducing recursion.
Such tutorials assume you will just accept that recursion is important.
This can make the reader wonder whether the entire language will be filled with what seems like theoretical exercises.

Any time you perform a repetitive task, you *could* use recursion, but why would you?
It's much easier to think about an ordinary looping construct.
You just count through the elements in a sequence and perform operations upon them.
Recursion seems to add needless complexity to an otherwise simple piece of code.

The problem is that recursion is not properly motivated in such tutorials.
You must first understand the need for immutability, then encounter the problem of repetition and see that your loop variable(s) mutate.
How do you get rid of this mutation?
By *initializing* values but never changing them.
To achieve this when you iterate through a sequence, you can create a new frame for each iteration, and what was originally a loop variable becomes a value that is initialized to the next step for each frame.

The *stack frame* of a function call is already set up to hold arguments and return the result.
Thus, by creating a stack frame for each iteration, we can initialize the next count of the loop value and never change it within that frame.
A recursive function is an excellent solution for the problem of iterating without a mutating loop variable.

This solution comes with its own problem.
By calling itself and creating a new stack frame for each recursion, a recursive function always has the possibility that it will exhaust (overflow) the stack.
The capacity of the stack depends on the size required for that particular function along with the hardware, operating system and often the load on the computer---all factors that make it effectively unpredictable.
Having an unpredictable error occur during recursion does not meet our goal of reliability.

The fix to this issue is a hack called *tail recursion*, which typically requires the programmer to organize their code such that the return expression for the recursive function does not perform any additional calculations, but simply returns a finished result.
When this criterion is met, the compiler is able to rewrite the recursive code so that it becomes simple imperative code, without the function calls that can lead to a stack overflow.
This produces code that is reliable from the safety standpoint (it doesn't overflow the stack) and from an immutability standpoint (there's no mutating loop variable).

At this point you might be wondering, "Wait, are you telling me that every time I want to perform some kind of operation on a sequence, I'm supposed to write recursive code rather than just an imperative loop?"
Although you would certainly get better at recursion with practice, it does sound exhausting.
Fortunately, functional programming goes one step further by implementing basic repetitive operations for you, using recursion.
This is why you see operations like `map`, `reduce`, `fold`, etc., instead of loops, in functional languages, or even languages that support a functional style of programming.
These operations allow you to benefit from the purity of recursion without implementing your own recursive functions except on rare occasions.

There's another fascinating factor that recursion exposes.
Under the covers, tail recursion uses mutation---which seems like a violation of functional programming's immutability goal.
However, because tail recursion is implemented by the compiler, it can be completely (and provably) invisible.
No other code can even know about any mutable state used to implement tail recursion, much less read or change it.
The concept of immutability only requires that storage be *effectively immutable*---if something is mutated (often for efficiency), it's OK as long as no other part of the program can be affected by that mutation.

