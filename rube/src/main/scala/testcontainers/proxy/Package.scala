package testcontainers.proxy

import zio.{Random, ZIO, Clock}
import zio.durationInt

val inconsistentFailuresZ =
  (
    for
      randomInt <- Random.nextInt
      _ <-
        ZIO.attempt {
          if (randomInt % 15 == 0)
            throw new RuntimeException(
              "Random failure"
            )
          else
            ()
        }
    yield ()
  )

val jitter =
  (
    for
      rand <- Random.nextIntBetween(1, 5)
      _ <-
        ZIO.debug(s"Sleeping for $rand seconds")
      _ <- ZIO.sleep(rand.seconds)
    yield ()
  )

val allProxies = jitter *> inconsistentFailuresZ
