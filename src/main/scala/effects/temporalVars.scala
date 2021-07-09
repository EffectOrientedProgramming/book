package effects

import java.util.Calendar

object temporalVars {
//Time based functions will mainly be effectual, as they rely on a varaible that is constantly
  //changing.

  def sayTime() =
    val curTime = Calendar.getInstance()
    val curOption: Option[java.util.Calendar] =
      curTime match {
        case x: Null               => None
        case x: java.util.Calendar => Some(x)
      }
    val curMin = curOption match {
      case None    => println("oof")
      case Some(s) => s.get(Calendar.SECOND)
    }
    println(curMin)

  @main def temporalVarsEx =
    sayTime()
    Thread.sleep(3000)
    sayTime()

  //The input for the variable is the same, yet there is a differnt output.
  //The clock is thus considered an effect.
}
