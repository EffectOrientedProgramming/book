# Why Functional?

> In your journey to this book, you have undoubtedly learned at least one way to think about programming, and possibly several.

You might have had reasonable success using that approach.

You might also have encountered programming constructs inspired by functional programming.
For example, Java 8 introduced lambdas along with library support for streams and functional primitives like `map`.
Python has always allowed functions to be passed into, be created by, and be returned from other functions, and includes other functional support.
C++, especially more recent versions, has added a number of features that support functional-style programming.

If, however, you are coming from an imperative programming background, these functional-style devices can seem arbitrarily complicated.
Why go to the trouble to use something like `fold` or `reduce` when a simple `for` loop will do the job, and be much easier to understand? 
Sometimes it seems like functional programmers write code like this just to be fancy.

To understand what's really behind this different way of thinking about programming, it helps to start with some history.

## A Different Goal

In the early days of programming, most code was written in an assembly language for a particular machine.
Assembly language had primitive function-like constructs called subroutines, but you had to do the work of setting up a call (storing arguments in registers or on the stack), then inside the subroutine you had to access those arguments, and then before returning from the subroutine you had to somehow pass the return value back to the caller (again, typically using either a register or the stack).
You had to do all this by hand.
It was worth it if you *knew* you were going to reuse that subroutine, but if you thought you were only ever going to use that code once then it was easier and more efficient to just insert the code inline.
Even if you wanted to create reusable code, it was often easier to just "goto" a piece of code and use global variables, rather than bothering with passing arguments and returning results.

In those early days, a "high level language" meant a language like C that passed arguments and returned results for you.
This suddenly made writing functions much easier, safer, and faster.
But the habits of assembly-language programmers didn't vanish overnight, and people were still prone to writing obtuse monolithic programs---often a single function for the whole program---and jumping around in their code using `goto`s.
Many programs were written and maintained by a small number of programmers, or even a single programmer, for whom that code made sense.
Anyone else reading the code would be baffled.

In addition, the idea of calling code written by other people was fairly foreign.
If you didn't write the code yourself, how would you know if it does what you want? (Documentation and testing were also primitive, if they existed at all).
Thus "code reuse" was a big hurdle; there were many attempts within companies to create cultures of code reuse because many programmers were rewriting the same functionality from scratch, over and over within the same organization.

Monolithic programs that didn't reuse code were also a maintenance nightmare.
It was not uncommon for such programs to be thrown away and rewritten just to add some new features.
Not surprisingly, writing everything from scratch also took a lot longer than reusing common functionality. 
The new question became: "how do we make code reuse easier?"

Because languages like C and Pascal made it easy to not only write functions but to call them, they were a big improvement over assembly language, although the monolithic habits of assembly-language programmers persisted into those new languages.
Libraries grew bigger and more complex, and using those libraries was not easy.
In C, for example, you'd often have to call `malloc` to allocate memory before calling a library function, and later `free` to release the memory when that function was done with it.
You also had to learn how to pass information from one library function to another.
You had to learn how a each library reported errors, which typically varied in strategy from one library to the next.
Code could be reused, but it wasn't easy.

At this point, object-oriented programming seemed like a good idea, because it combined a common data structure, automatic initialization and cleanup of that data structure, together with all the functions that act upon that data.
If you wanted to reuse some code, you just created an object with some initialization values and then sent messages to that object to produce the desired results.
This *did* make code reuse easier, and helped speed up program creation.
It also came with the distraction of inheritance polymorphism and an entire education and consulting industry explaining how to cram every design into an inheritance hierarchy (inheritance polymorphism does sometimes prove useful, but not everywhere, all the time).

C++ added object-oriented features from the Simula language while maintaining backward compatibility with the C language.
C++ had a strong emphasis on static type checking.
Java was created as a counterpoint to C++ and was heavily inspired by the Smalltalk language.

Smalltalk's success came from its ability to rapidly create systems by adding functionality to existing objects.
This introduced a conundrum, because Smalltalk is a dynamic language, and Java, like C++, is statically typed.
Smalltalk can be thought of as supporting an experimental style of programming: you send a message to an object and discover at runtime whether the object knows what to do with that message.
But C++ and Java ensure everything is valid, at compile time (along with escape mechanisms that effectively disable that type checking).
This conundrum is exemplified by the *Liskov Substitution Principle*, which says that you shouldn't add new methods to an inherited type---and yet that activity is the foundation of Smalltalk.

The Agile methodologies that began in the early 2000's were another attempt to produce software faster, but through a more bottom-up lens.
Agile was primarily focused on improving communication between stakeholders and developers, and producing more rapid round trips between needs and experiments.
This improves the chance that the stakeholders will get what they need, faster.
Agile helped the process of software development, but again, the focus is on developing software quickly, not on developing reliable software.

The most important thing to take away from this language history is that the fundamental goal of the various techniques was speed of creation.
There seems to be an underlying assumption that these approaches will somehow automatically create more reliable software.
As a result, we have languages that quickly create unreliable software.
And in many cases we've been able to get by with that.
For one thing, this approach has greatly advanced testing technology, because it was necessary.
Customers have learned to put up with buggy software.
They've often been willing to accept buggy software when the alternative is no software at all.

The world has changed.
Back then, the drive was to speed up activities that humans were doing.
Those humans could compensate for bugs.
Now, however, more and more software is doing things that humans can't do, so failures in software cannot be propped up by humans.
Unreliable software is no longer an inconvenience, but a serious problem.

Quickly creating unreliable software is no longer acceptable.
We must delve into the reasons that software fails---either it doesn't do what it's supposed to, or it just breaks.

## Reuse

How do we create software? 
When you first learned to program, you probably solved problems by writing code using the basic constructs of your language.
But at some point you began realizing that you could only produce and debug so much code by yourself.
If you could use code that was already written and debugged by other people, you could produce solutions faster.

You might have gone through a cut-and-paste phase before discovering that formalized libraries were easier and more reliable.
Even then, library ease of use depended on the sophistication of your language.
For the reasons mentioned, using a C library could be tricky and difficult.
C++ made this much easier and paved the way for the acceptance of languages like Java, Python, Scala, and Kotlin.
Indeed, any new language that doesn't support easy code reuse is not taken seriously.

But code reuse in object-oriented languages was still limited.
You could either use objects in a library directly, or you could add those library classes into new classes using *composition*.
This was a big step and it helped a lot.
In contrast, composing C libraries wasn't particularly realistic---it was just too messy and complicated.

The problem is reliability.
If you create a new class using composition, you combine problems with the existing class(es) with any bugs you introduced in your new class.
As you build up bigger systems, the problem of bugs multiplies.

To compose systems rapidly *and* reliably, we return to first principles and figure out how to:

1. Create basic components that are completely reliable.
2. Combine those components in a way that doesn't introduce new bugs.

To achieve these goals we must examine the fundamentals of how we think about software.

## Pure Functions

Composition in an object-oriented language doesn't attempt to manage bugs, so it ends up amplifying them.
If we want to compose pieces of software, we must discover what creates a fundamentally unbreakable piece, then how to assemble those pieces without producing a broken result.

First, what constitutes a reliable, unbreakable piece of software? We've already seen that objects are not inherently unbreakable, so we'll move back to a more basic software component: the function.
What are the characteristics of an unbreakable function?

What we want is the same kind of function we have in math.
This means that the function does *nothing* except produce a result from its arguments.
And given the same arguments, it always produces the same result.

This behavior imposes additional constraints: The function cannot affect its environment, and the environment cannot affect the function---otherwise, the function has a *history* and behaves differently at one point in time vs.
another.
Running that function doesn't necessarily produce the same results from one call to the next.

If a function affects its environment, we call that a *side effect*.
It's "on the side" because it's something other than just producing a result from the function.
Many programming languages have side effects built in, in the form of *statements*.
A statement doesn't return a result, so the only reason to execute a statement is for its side effect.
For example, "print" is typically a statement that returns nothing but causes the side effect of displaying text on a console.
On the other hand, an *expression* produces ("expresses") a result.
A functional language avoids statements and attempts to make everything an expression that produces a result.

What about the environment affecting the function? This is a bit more subtle, and requires that we think in more general terms than just basic imperative programming.
In particular, we must consider concurrency.
If multiple tasks are running in our program, then at any point another task might see variables in our function.
A variable can change, so that means this other task might see different values at different points in the function's execution.
And if that variable is modified by some other task, we have no way of predicting the result, and we don't get the reliable mathematical function that we want.

We solve this problem through *immutability*.
That is, instead of using variables, we create values that *cannot change*.
This way, it doesn't matter if an external task sees our values, because it will only see that one value and not something that is different from one moment to the next.
And the external task cannot change the value and cause the function to produce a different result.

Functions that behave mathematically, that always produce the same results from the same inputs and have no side effects, are called *pure functions*.
When we add the additional constraint of immutability, we produce functions that compose without introducing points of breakage.
We can reliably reason about such functions.

## Composability

If functions `g` and `h` are pure, we should be able to combine them directly (assuming the types agree) to produce a new function `f`:

```text
f(a) = g(h(a))
```

This assumes that all functions involved are *complete*, meaning that they produce a legitimate result for every possible value of `a`.
This is not always true.
For example, dividing a number by zero is undefined, and so cannot produce a reasonable number as a result.
Using a key to look up a value in a map is undefined if that key doesn't exist in the map.

An incomplete function requires more operations when using it, to handle the problematic inputs.
You can think of the solution as *stepwise composability*.
Instead of calling `g(h(a))`, we break the process into steps: `x = h(a)`, then check the success of the operation.
If successful, pass the result to `g`.
These extra steps make composability sound like it could get tedious, and languages like Scala that provide more thorough support for functional programming provide syntax to make this kind of programming feasible.
We will look at this support in the [Monads]{{???}} chapter.

## Effects

Now we have created this perfect world of pure functions that behave just like the functions in theoretical mathematics.
They have no side effects and cannot be affected by other functions, and can be neatly and safely composed.

"But," you wonder, "if all I can do with the result of one pure function is pass it as an argument to another pure function, what's the point of all these pure function calls? 
If these functions have no effect on the world, they seem like an intellectual exercise that merely heats up the CPU."

This is absolutely true.
A program that never affects the world is pointless.
For a program to be useful, it must be affected by the world, and it must have effects upon the world.

The phrase "side effect" implies an incidental or accidental impact on the world.
What we need to do is formalize this idea and bring it under our control.
We can then call it simply an "effect," without the "side." 
The solution is to manage these effects so they are under our control.

This bridge between pure functions and practical programs with controlled and managed effects is the reason for the title of this book.

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

## Core Differences Between OO and Functional

An OO language worries about managing state.
It "encapsulates" a data structure in privacy and surrounds it with custom methods (aka member functions) which are ideally the only way to access and modify the state of that data structure.
This is important because an OO data structure is typically mutable.
This OO ceremony attempts to create predictability by knowing how the data structure can be mutated.

Functional programming abstracts common behavior into reusable functional components. 
These components are adapted to specific needs using other functions. 
This is why lambdas are so important, because you constantly need to adapt general code to specific purposes, often with a brief amount of code that would otherwise be awkward and intrusive to right as a standalone function.

Functions in a functional language don't need to be tied to a particular data structure.
Thus, they can often be written for more general use and to reduce duplication.
Functional languages come with a general set of well-tested, reusable operations that can be applied almost mathematically in many situations.

A functional language relies on immutability.
An immutable data structure doesn't need privacy because it is safe for any task to read, and it cannot be written (only initialized).
Because immutability dramatically simplifies everything, objects in functional languages are simply naked data structures along with constructors.
When everything is immutable, there is no need for private properties or methods to maintain the state of an object.

## Summary: Style vs Substance

Functional programming abstracts common behavior into reusable functional components.
These components are adapted to specific needs using other functions. 
This is why lambdas are so important, because you constantly need to adapt general code to specific purposes, often with a brief amount of code that would otherwise be awkward and intrusive to write as a standalone function.

The two things we do with functions is compose them to make more complex functions, and adapt to them to our specific problem.

We assume that many readers are attracted to this book because they have some experience with functional programming constructs in other languages such as Java (version 8 or newer), Kotlin, Python or some other language that provides a modicum of support.
However, we also assume you have heard---or you have a sense---that there could be significantly more than, for example, a function's ability to create other functions, or putting elements into a stream and acting upon that stream with `map`, or parallelizing stream operations.
Those are indeed important benefits, but they just dip into the possibilities.
Adopting some of the styles found in functional programming does not make a language functional.

In this book we want to get to the heart of what it means to be functional.
In particular, we want to show what it takes to make *reliable* functional code that can be composed without propagating or amplifying flaws in its components.
A core way this is accomplished in ZIO is through the use of *monads*, which we gently introduce in the next chapter.
