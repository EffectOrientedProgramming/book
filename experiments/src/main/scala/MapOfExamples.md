#Map Of Examples

This is a description of all the examples in this 
directory. 

This file will describe what each package contains, and a general
order to review the examples in. 

##Scala Basics
 This section will cover necessary Scala methods and functions used in 
 future sections. It includes using the `@main` format, the basics of a for-comprehension, and 
how to write tail recursions. The map and flatmap functions are in the context of non-monadics.
 >usingMains <br/>
 >forComprehension<br/>
 >tailEndRecursion<br/>
 >map<br/>
 >flatMap<br/>

##FP Building Blocks
This section has several examples that are necessary to understand in order to
start coding in a Functional Programming style. Pure core refers to the style of writing
functional programs that deal with effects. The 'pure core' is truly pure, or non-effectual.
The effectual outside is where all the effects/non-deterministic parts of the code are. 

>general functions <br/>
>higherOrderFunctions<br/>
> pureCore<br/>

##Monads
Also known as the "Schrödinger's Cheshire Kat" set, this section begins with the basic 
setup of a box that may contain a kat of varying states. The premise of the code is to create a
box with (maybe) a kat in it, then doing something to the box. This models how to deal with uncertain
data types that may have errored, but follows through the logic regardless. The example set writes the 
code several times with the same functionality, yet with different implementations. Teh first implementation
is with an adt. Then the implementation goes to using `either`s, then starts to use effects, and finally, the 
implementation writes its own monad. 

The progression shows the evolution, and building blocks that are required for a monadic structure. 


As far as I can tell, this is the best progression. Note, the directories in the 'monads' folder are not very clear 
on what the files actually add/include.

> adts/Kats <br/>
> either/Kats<br/>
> effects/Kats<br/>
> monads/monads/Kats<br/>
> monads/fors/Kats<br/>


##zioBasics
This section is currently composed of basic ZIO examples. It covers success, failure, using alias', the equalities
of ZIO, how to chain ZIO, and how to use the 'runtime' functionality.

>success<br/>
>fail<br/>
> Alias <br/>
> Equality<br/>
> chaining<br/>
> runtimeEx<br/>
> Simple<br/>

##HelloZio
This is where we start using ZIO examples. As mentioned above, there are several basic examples 
in the 'zioBasics' directory. 

> sayHello<br/>
> sayHelloTime<br/>
> runningAZIO<br/>
> CalculatorExample<br/>
 
**Note:** CalculatorExample has a lot of more advanced methods, such as error-handling and chaining. It would make more sense to
include this later, when said methods have already been explained.

##Scala Types to ZIO
There are several scala data types that can be easily converted to a ZIO. This section briefly gives examples on how to
convert the data types into ZIO data types.

>Either to ZIO<br/>
> Function To ZIO<br/>
> Future to ZIO<br/>
> Option to ZIO<br/>
> Try to ZIO<br/>

##Handling Errors
This section goes over several standard ways to handle errors in your code using ZIO. Each of the categories contains one or 
two approaches to handling errors.

>value (Using Either and Absolve Types)<br/>
>folding (logical folding)<br/>
> fallback (orElse())<br/>
> catching (easy ways to use .catch for ZIO)<br/>

##Built Up Examples
This is not itself a directory in the files, however, it would be a good place to put basic, but full ZIO
code examples. 
>Calculator Example<br/>

##Parallelism
This sections displays how Fibers work. It starts with a description, and very low level example of how to use Fibers.
It then works its way up to displaying the practical uses of the `Scheduler`. It also includes how to use finalizers in 
case of fatal errors.

> Basic Fiber<br/>
> Join<br/>
> Await<br/>
> Compose<br/>
> Interrupt<br/>
> Finalizers <br/>


##Crypto
This section describes a model of several scenarios.crypto-currency miners. It shows how to conduct multiple fibers at once, and some key scheduler methods.

>Mining<br/>


##Directory Example
This is a set of examples that build up to a mock database model. It models a firm's database holding employee information.
The basic code is a mock query and response between the user and the database. In reality, the 'database' is a CSV file.
The benefit of this example it that it pulls together all of what has been previously discussed. It covers finalizers,
several types of error handling(fatal and non-fatal errors), for comprehensions, and
functional programming style (ie. composition, recursion, pure core/ effectual outside, ect...)
It is also built in a modular way, so each additional aspect in the example is in its own file. 

> employeeDef <br/>
> CSVreader (with finalizers, and random IO errors)<br/>
> processingFunctions (parsing functions Str => emp)<br/>
> searchFunctions (Useful recursion styles, and overloading)<br/>
> userInputLookup (All the functions put together into a full example)<br/>
> directoryExample_full (Automated example of full example)<br/>

##Hubs
This section uses an example to demonstrate how to use ZIO-Hubs. 

>BasicHub<br/>
>HubExploration<br/>
