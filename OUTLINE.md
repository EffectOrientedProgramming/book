## Outline

Notes:
- First 5 chapters are about the why - bring the wow.
- Chapters 6 on are about the how.

1. Superpowers with Effects
    1. OpeningHook (1-6)
        1. Note: Progressive enhancement through adding capabilities 
    1. Concurrency
        1. Race
        1. Hedge (to show the relationship to OpeningHook ie progressive enhancement)
    1. Sequential
        1. Note: Combine OpeningHook & Concurrency with ZIO Direct
    1. And so much more
        1. Note: And there are many capabilities you might want to express. In the future we will dive into these other capabilities.
1. Functions & Specialized Data Types Are Great
    1. Why Functional
1. But Functions & Specialized Data Types Don't Compose for Effects
    1. Composability
        1. Limitations of Functions & SDTs
        1. Some intro to Universal Effect Data Types ie ZIO
        1. The ways in which ZIOs compose (contrasted to limitations)
        1. Note: Merge chapters: composability, Unit, The_ZIO_Type
        1. Note: Avoid explicit anonymous sum & product types at this point
1. Dependency Injection with Effects
   1. Application startup uses the same tools that you utilize for the rest of your application
   1. App vs Test
   1. Composability

1. The ZIO data type
1. Running ZIOs
1. Layers / Environment
    1. Creating
    1. Composing
1. Tests -- Position depends on how concise we can make demo tests in mdoc
    1. `assertTrue`
    1. Test layers
1. Errors
    1. Creating & Handling
    1. Error composability
    1. Cause
    1. Retry
1. Concurrency - High Level
    1. forEachPar, collectAllPar
1. Concurrency - Low Level
    1. fork join
    2. Throwaway reference to STM
1. Concurrency - Interruption
    1. Timeout
    1. Race
1. Concurrency - State
   1. Ref
   1. Thundering Herds
1. Repeats
1. Resource Management
1. Logging
1. Streams
1. Configuration

