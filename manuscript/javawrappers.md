## javawrappers
 Newer stuff!
 

### experiments/src/main/scala/javawrappers/InstantEOP.scala
```scala
package javawrappers

import zio.duration2DurationOps
import java.time.Instant

// TODO Consider deleting
object InstantOps:
  extension (i: Instant)
    def plusZ(duration: zio.Duration): Instant =
      i.plus(duration.asJava)

```


