# Info for Book Developers

## Code Fences

| Fence                                    | Code                                    | Manuscript               | Examples                    |
|------------------------------------------|-----------------------------------------|--------------------------|-----------------------------|
| `scala 3 mdoc:compile-only`              | Must Compile                            | Only Original Code       | No code generated           |
| `scala 3 mdoc:invisible`                 | Must Compile                            | Excluded from manuscript | Original Code -> `src/main` |
| `scala 3 mdoc:invisible manuscript-only` | Must Compile                            | Excluded from manuscript | No code generated           |
| `scala 3 mdoc:silent`                    | Must Compile                            | Only Original Code       | Original Code -> `src/main` |
| `scala 3 mdoc:silent testzio`            | Must Compile                            | Only Original Code       | Original Code -> `src/test` |
| `scala 3 mdoc:fail`                      | Must not compile                        | Code & Compile Error     | No code generated           |
| `scala 3 mdoc:runzio`                    | Must contain `def run`                  | Code & Output            | Code & Output -> `src/main` |
| `scala 3 mdoc:runzio:liveclock`          | Must contain `def run` & Uses LiveClock | Code & Output            | Code & Output -> `src/main` |
| `scala 3 mdoc:testzio`                   | Must contain `def spec`                 | Code & Output            | Code & Output -> `src/test` |
