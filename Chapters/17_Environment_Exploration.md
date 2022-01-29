# Environmental Exploration

The Environment type parameter is one of the most distinguishing features of ZIO compared to other IO monads.
At first, it might seem like a complicated way of passing values to your ZIO instances - why can't they just be normal function arguments?


- The developer does not need to manually plug an instance of type `T` into every `ZIO[T, _, _]` in order to run them.
- `ZIO` instances can be freely composed, flatmapped, etc before ever providing the required environment. It's only needed at the very end when we want to execute the code!
- Environments can be arbitrarily complex, without requiring a super-container type to hold all of the fields.
- Compile time guarantees that you have 
  1. Provided everything required
  1. Have not provided multiple, conflicting instances of the same type

## Dependency Injection 

## ZLayer

## Powered by a TypeMap

## Shared by default

## Compile-time guarantees
