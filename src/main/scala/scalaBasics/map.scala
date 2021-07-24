package scalaBasics

object map:

  val nums = Vector(0, 2, 1, 4, 3)
  val letters = Vector('a', 'b', 'c', 'd', 'e')

  @main
  def mapEx =
    val combined =
      nums.map(letters) //re-order letters
    println(combined)

    val comb2 =
      nums.map(i =>
        i -> letters(i)
      ) //assign values of nums to letters
    println(comb2)
  end mapEx
end map
