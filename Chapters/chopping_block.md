## from composability section about non-zio approaches and their downsides:

### Plain functions that block TODO: Remove?

- We can't indicate if they block or not
- Too many concurrent blocking operations can prevent progress of other operations
- Very difficult to manage
- Blocking performance varies wildly between environments


### Implicits TODO-Can this be show in AllTheThings?
  - Are not automatically managed by the compiler, you must explicitly add each one to your parent function
  - Resolving the origin of a provided implicit can be challenging

