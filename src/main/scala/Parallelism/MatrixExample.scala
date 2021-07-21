package Parallelism

import zio.*

object MatrixExample {

  case class matrix2D(
      sizeX: Int,
      sizeY: Int,
      twoDList: List[List[Int]]
  ):

    def mapInt(f: Int => Int): matrix2D =
      val newData = this.twoDList.map(i =>
        i.map(j => f(j))
      )
      matrix2D(this.sizeX, this.sizeY, newData)

    def printMat()
        : Unit = //Uses the map function to itterate
      this.mapInt(i =>
        println(i)
        i
      )

    def map(f: => Int): matrix2D =
      this

  def printMat(mat: matrix2D): Unit =
    mat.twoDList.foreach(i =>
      i.foreach(j => print(s"  $j  "))
      println("\n")
    )

  def makeMatrix2D(
      sizeX: Int,
      sizeY: Int
  ): matrix2D =
    val rows: List[Int] = List.range(0, sizeY)
    //println(rows)
    val matr = rows.map(i =>
      List.range(i * sizeX, i * sizeX + sizeX)
    )
    //println(matr)
    matrix2D(sizeX, sizeY, matr)

  def flipMatrix(matr: matrix2D): matrix2D =
    def flippedCol(col: List[Int]) =
      col.foldLeft(List(col(0)))((a, i) =>
        a.::(i)
      )
    val flippedMat = matr.twoDList.foldLeft(
      List(List.empty[Int])
    )((b, j) =>
      b.::(flippedCol(j).dropRight(1))
    )
    matrix2D(
      matr.sizeX,
      matr.sizeY,
      flippedMat
    )

}

@main def matricies =
  val mEx = MatrixExample
  val matrix = mEx.makeMatrix2D(4, 4)
  println("Normal Matrix:")
  mEx.printMat(matrix)
  //matrix.printMat()
  println("\n \n Flipped Matrix:")
  val flippedMat = mEx.flipMatrix(matrix)
  mEx.printMat(flippedMat)
//flippedMat.printMat()

/*
  Disabled to stop breaking build in Github Actions
  val bigMat = mEx.makeMatrix2D(6000, 6000) //from clean, takes around 15 sec
  //mEx.printMat(bigMat)
  val zbigMat = ZIO { bigMat }
  val logic =
    for
      flattened: mEx.matrix2D <- zbigMat
      zbigger: mEx.matrix2D <- ZIO { flattened.mapInt(i => i + 1) }
    yield zbigger

  logic.exitCode

 */
