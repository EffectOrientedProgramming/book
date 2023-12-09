package errors

import zio.ZIOAppDefault

import java.lang.{
  IllegalArgumentException,
  NumberFormatException
}

object ThrowCompose extends ZIOAppDefault:

  def throwsAFoo() =
    throw IllegalArgumentException(
      "Your argument is invalid"
    )

  def throwsABar() =
    throw NumberFormatException(
      "Your number is trash"
    )

  def doThing() =
    throwsAFoo()
    throwsABar()

  override def run = ???
