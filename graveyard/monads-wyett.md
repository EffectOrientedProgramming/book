# Monads-Wyett
In the world of Functional Programming, *Monads* are a data type that can be used for handling errors. Monads 
can be though of as a way of wrapping existing variable types in an abstraction as to
better represent their possible uncertainty. 

An example of a common Monad in computer science is the variable type 'Maybe'.
This 'maybe variable' can be one of two types. A 'maybe variable' defined by Maybe(String, Int)
might be either a String or an Int at runtime. There is a level of uncertainty in the variable until the 
program is executed. 

When applied to a function, using a monad can represent a level of uncertainty that the function 
will be successful. When defining a function, if the programmer is unsure if there will be 
an error or fault in the process, they can wrap a monad around the function to represent that uncertainty. 
The monadic function may return an intended value on success, but it might return an error. By wrapping 
the intended functionality of an object with the possible error types, a programmer can create data types that
express all possibilities for the function. The monadic data type is thus consistent, safe, and conveys information
clearly. 

In addition to monadic data types containing uncertainty, monads must also have a 'flatmap' function. This flatmap function
will be able to identify and return one of the possibilities the monad represents as the 'favored' outcome. Once one of the outcomes is 
extracted from the monad, it can be passed into another function, which is given to flatmap as a parameter. At runtime, this other function 
will only be called when the monad represents the successful, or favored output. 
For example:

>//This is pseudocode
> monad = success | failure
> value = monad.flatmap(doSomething)

At runtime, `monad` will either end up being a success or a failure. If it is successful, the success output of `monad` will be
passed into `doSomething()`. If monad fails, then the function `doSomething()` will not be called at all. 

Monads allow programmers to access all the benefits of Functional Programming while being able to handle effects. 
By incorporating monads, programmers can still follow all the rules of FP, while being able to deal with uncertain
systems. By wrapping the intended output type of a function with the information of all the ways it could fail, monads
can present a data type that have the possibility of failing, but in a controlled way. This level of control facilitates FP.


