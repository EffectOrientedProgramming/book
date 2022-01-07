package fpBuildingBlocks

import scala.math.BigDecimal
import exIOError.errorAtNPerc
import java.io.IOException

object pureCore:
  // Here, we will be using Options as a way to
  // move effects out of a
  // pure functional core, and into an effectful
  // outside.
  // errorAtNPerc will model our example effect.

  // Non-Pure
  // (The function models a translation with cash)
  def transaction(
      cashPayment: Double,
      price: Double
  ): Double =
    errorAtNPerc(
      50
    ) // There will be a 50% chance of random failure to model an effect
    BigDecimal(cashPayment - price)
      .setScale(
        2,
        BigDecimal.RoundingMode.HALF_UP
      )
      .toDouble

  def statement(valid: Boolean): Unit =
    if (valid)
      println("Have a wonderful Day!")
    else
      println(
        "I'm very sorry, there was an error with our system..."
      )

  // This string of logic is considered impure.
  // The programmer is checking for issues
  // throughout the logic.
  @main
  def NonPure =
    val change =
      try
        transaction(20, 19.99)
      catch
        case e: IOException =>
          None

    val continue =
      change match
        case None =>
          println(
            "An Error occurred in the Transaction"
          );
          false
        case _ =>
          println(s"Your change is $change");
          true

    statement(continue)
  end NonPure
end pureCore

/* //Pure Form def
 * transaction2(cashPayment:Double,
 * price:Double):Option[Double] =
 * errorAtNPerc(50)//There will be a 50% chance
 * of random failure to model an effect
 * BigDecimal(cashPayment - price).setScale(2,
 * BigDecimal.RoundingMode.HALF_UP).toDouble
 *
 * def statement2(valid:Boolean):Unit =
 * if (valid) println("Have a wonderful Day!")
 * else println("I'm very sorry, there was an
 * error with our system...") */
