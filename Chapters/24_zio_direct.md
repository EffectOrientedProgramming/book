We have been experimenting with the zio-direct style of writing ZIO applications.
Our theory is that it is easier to teach this style of code to beginners.
"Program as values" is a core concept when using ZIO. 
ZIOs are just unexecuted values until they are run.

If not using zio-direct, you must explain many language details in order to write ZIO code.

    - If you want to sequence ZIO operations, you need to `flatmap` them
    - To avoid indentation mountain, you should use a `for comprehension`
    - How a `for` comprehension turns into `flatMap`s followed by a final `map`
    - If you want to create a `val` in the middle of this, you need to use a `=` instead of `<-`

After you have accomplished all of that, you have trained your student to write concise, clean code... that only makes sense to those versed in this style.

With zio-direct, you can write ZIO code that looks like imperative code.

Here are the concepts you need to understand for `zio-direct`

    - If you want to sequence ZIO operations, you need to write them inside of a `defer` block
    - Code in `defer` will be captured in a ZIO
    - Inside defer you can use `.run` to indicate when effects should be executed
        Note- `.run` calls are *only* allowed inside `defer` blocks.


After you have accomplished _that_, you have trained your student to write slightly less concise code... that most programmers will be comfortable with.