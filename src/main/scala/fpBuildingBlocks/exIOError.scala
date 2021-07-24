package fpBuildingBlocks

import java.io.IOException
import scala.util.Random

object exIOError:

  // The input needs to be within the range
  // 1-100.
  // This function will randomly throw an io
  // error n % of the time.
  def errorAtNPerc(n: Int) =
    if (n < 0 | n > 100)
      throw new Exception(
        "Invalid Input. Percent Out of Bounds"
      )
    else
      val rand = Random.nextInt(101)
      if ((0 to n).contains(rand))
        throw new IOException(
          "An unexpected IOException Occured!!!"
        )
end exIOError
