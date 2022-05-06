# Built-in Services
Some Services are considered fundamental/primitive by ZIO.
They are built-in to the runtime and available to any program.

## 1.x
Originally, ZIO merely provided default implementations of these services.
There was nothing else special about them.
If you wanted to write to the console in your code, you needed a `ZIO[Console, _, _].
If you wanted to write random, timestamped numbers, accompanied by some system information to the console, 
you needed a `ZIO[Random with Clock with System with Console, _, _].
This is maximally informative, but it is also a lot of boilerplate code.

## 2.x
Late in the development of ZIO 2.0, the team decided to bake these deeper into the runtime.
Now you can use any of these services without an impact on your method signatures.