package atomic.realZIO

import zio.*

class ChainingVsComposing:
  trait UserNotFound
  trait UserInfo

  def getInfo(
      user: String
  ): ZIO[Any, UserNotFound, UserInfo] = ???

  trait NetworkError
  trait HouseListings

  def zillow(
      zipCode: String
  ): ZIO[Any, NetworkError, HouseListings] = ???

  trait NoGoodHouseAvailable
  trait Home

  trait SomeOtherError

  def buyBestHouse(
      userInfo: UserInfo,
      houseListings: HouseListings
  ): ZIO[Any, NoGoodHouseAvailable, Home] = ???

  def buyDecentHouse(
      userInfo: UserInfo,
      houseListings: HouseListings
  ): ZIO[Any, Nothing, Home] = ???

  def yoloHouse(): ZIO[Any, Nothing, Home] = ???

  val userId  = "someUser"
  val zipCode = "81224"

  val exploreChaining: ZIO[
    Any,
    UserNotFound |
      NetworkError |
      NoGoodHouseAvailable,
    Home
  ] =
    for
      userInfo: UserInfo <- getInfo(userId)
      houseListings      <- zillow(zipCode)
      newHome <-
        buyBestHouse(userInfo, houseListings)
    yield newHome

  val combined =
    getInfo(userId).zipPar(zillow(zipCode))

//  val exploreChainingPar: ZIO[
//    Any,
//    UserNotFound |
//      NetworkError |
//      NoGoodHouseAvailable,
//    Home
//  ] =
// combined.flatMap((userInfo, houseListings)
  // =>
//      buyBestHouse(userInfo, houseListings)
//    )

  val finalHouse: ZIO[
    Any,
    UserNotFound |
      NetworkError |
      NoGoodHouseAvailable,
    Home
  ] =
    // This doesn't actually reduce the error
    // type
    exploreChaining.catchSome {
      case (_: NoGoodHouseAvailable) =>
        yoloHouse()
    }

  val getUserAndZillowInfo: (String, String) => (
      ZIO[Any, UserNotFound, UserInfo],
      ZIO[Any, NetworkError, HouseListings]
  ) = (x, y) => (getInfo(x), zillow(y))
end ChainingVsComposing

// val getUserAndZillowInfo: (String, String)
// => (UserInfo, HouseListings) =
// getInfo.combinify(zillow) //
// .andThen(buyBestHouse)

class NarrowError:

  trait UserInfo
  trait UserNotFound
  trait UserPrefError
  sealed trait UserPrefs
  case object NoUserPrefs extends UserPrefs

  def getInfo(
      user: String
  ): ZIO[Any, UserNotFound, UserInfo] = ???

  def userPrefs(
      userInfo: UserInfo
  ): ZIO[Any, UserPrefError, UserPrefs] = ???

  val wide: ZIO[
    Any,
    UserNotFound | UserPrefError,
    UserPrefs
  ] =
    for
      userInfo <- getInfo("asdf")
      prefs    <- userPrefs(userInfo)
    yield prefs
end NarrowError

/* // catchSome can't narrow the errors val
 * narrow: ZIO[Any, UserNotFound, UserPrefs] =
 * wide.catchSome { case _: UserPrefError =>
 * ZIO.succeed(NoUserPrefs) } */

class MatchError:

  trait UserInfo
  trait UserNotFound
  trait ProductInfo
  trait ProductNotFound
  trait Recommendations

  def getInfo(
      user: String
  ): ZIO[Any, UserNotFound, UserInfo] = ???

  def getProduct(
      product: String
  ): ZIO[Any, ProductNotFound, ProductInfo] = ???

  def recommended(
      userInfo: UserInfo,
      productInfo: ProductInfo
  ): ZIO[
    Any,
    UserNotFound | ProductNotFound,
    Recommendations
  ] = ???

  // why do we have to add
  // NoSuchElementException?

  val userAndProductInfo: ZIO[
    Any,
    UserNotFound |
      ProductNotFound |
      NoSuchElementException,
    (UserInfo, ProductInfo)
  ] = getInfo("asdf").zipPar(getProduct("zxcv"))

  val app: ZIO[
    Any,
    UserNotFound |
      ProductNotFound |
      NoSuchElementException,
    Recommendations
  ] =
    for
      (userInfo, productInfo) <-
        userAndProductInfo
      recommendations <-
        recommended(userInfo, productInfo)
    yield recommendations
end MatchError
