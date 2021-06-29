#Map Of Examples

This is a description of all the examples in this 
directory. 

This file will describe what each package contains, and a general
order to review the examples in. 

##Scala Basics
 This section will cover necessary Scala methods and functions used in 
 future sections:
 >usingMains
 >forComprehension
 >tailEndRecursion
 >map
 >flatMap

##effects
This section is currently composed of basic ZIO examples, however, it will
have more examples specific to non-ZIO effects.

>success
>fail
> Equality
> chaining
> runtimeEx
> Simple

##FP Building Blocks
This section has several examples that are necessary to understand in order to
start coding in a Functional Programming style.

>general functions
>higherOrderFunctions
> pureCore

##Monads
Also known as the "Schrodinger's Cheshire Kat" set, this section begins with the basic 
setup of a box that may contain a kat, and several relating functions. The set starts
the implementations using general adt, and builds up through several data types
until the example writes its own monad. 

As far as I can tell, this is the best progression. Note, the directories in the 'monads' folder are not very clear 
on what the files actually add/include.

>adts/Kats
>either/Kats
>effects/Kats
> monads/monads/Kats
> monads/fors/Kats

##HelloZio
This is where we start using ZIO examples. As mentioned above, there are several basic examples 
in the 'effects' directory. 

>sayHello
> sayHelloTime
> runningAZIO
> CalculatorExample
 
**Note:** CalculatorExample has a lot of more advanced methods, such as error-handling and chaining. It would make more sense to
include this later, when said methods have already been explained.

##Scala Types to ZIO
There are several scala data types that can be easily converted to a ZIO. This section briefly gives examples on how to
convert the data types into ZIO data types.

>Either to ZIO
> Function To ZIO
> Future to ZIO
> Option to ZIO
> Try to ZIO

##Handling Errors
This section goes over several standard ways to handle errors in your code using ZIO. Each of the categories contains one or 
two approaches to handling errors.

>value (Using Either and Absolve Types)
>folding (logical folding)
> fallback (orElse())
> catching (easy ways to use .catch for ZIO)

##Built Up Examples
This is not itself a directory in the files, however, it would be a good place to put basic, but full ZIO
code examples. 
>Calculator Example

##Parallelism
This sections displays how Fibers work. It starts with a description, and very low level example of how to use Fibers.
It then works its way up to displaying the practical uses of the Scheduler. It also includes how to use finalizers in 
case of fatal errors.

> Basic Fiber
> Join
> Await
> Compose
> Interrupt
> Finalizers 


##Crypto
This section describes a model of several crypto-currency miners. It shows how to conduct multiple fibers at once, and
some key scheduler methods. 

>Mining


##Directory Example
This is a set of examples that build up to a mock database model. It models a firm's database with employee information.
The basic code is a mock query and response from the user to the database. In reality, the 'database' is a CSV file.
The benefit of this example it that it pulls together all of what has been currently discussed. It covers finalizers,
several types of error handling(fatal and non-fatal errors), for comprehensions, and
functional programming style (ie. composition, recursion, pure core/ effectual outside, ect...)
It is also built in a modular way, so each additional aspect in the example is in its own file. 

> employeeDef
> CSVreader (with finalizers, and random IO errors)
> processingFunctions (parsing functions Str => emp)
> searchFunctions (Useful recursion styles, and overloading)
> userInputLookup (All the functions put together into a full example)
> directoryExample_full (Automated example of full example)

##Hubs
This section uses an example to demonstrate how to use ZIO-Hubs. 

>BasicHub
>HubExploration
