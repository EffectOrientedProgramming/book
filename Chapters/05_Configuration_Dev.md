# Configuration Dev


```scala mdoc
import zio.config.*
import zio.config.magnolia.*

case class RetryConfig(times: Int)

val configDescriptor: Config[RetryConfig] = deriveConfig[RetryConfig]

generateDocs:
  configDescriptor
.toTable
.toGithubFlavouredMarkdown

val configLayer: Layer[Config.Error, RetryConfig] =
  ZLayer.fromZIO:
    read:
      configDescriptor.from:
        ConfigProvider.fromMap:
          Map("times" -> "1")

val logic =
  defer:
    val retryConfig = ZIO.service[RetryConfig].run
    retryConfig.times
  .provide:
    configLayer

runDemo:
  logic

//
//runDemo:
//  Bread2
//    .fromFriend
//    .retry:
//      Schedule.recurs:
//        1
//    .orElse:
//      Bread.storeBought
//    .build // TODO Stop using build, if possible
//    .debug
```
