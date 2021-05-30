package adts.hmmm

object Info:

  import java.net.URL

  opaque type Name = String

  object Name:
    def apply(s: String): Name = s

  extension (n: Name) def fancyRep() = n.toUpperCase

  opaque type Repo = URL

  object Repo:
    def apply(s: String): Repo = new URL(s)

  // We can't pattern match on ADTs that are implemented with opaque types
  type NameOrRepo = Name | Repo

  type NameAndRepo = Name & Repo

  type NameRepo = NameOrRepo | NameAndRepo

@main def run =
  Info.Name("name").fancyRep()

  val name: Info.NameOrRepo = Info.Name("asdf")

  // This doesn't work, because we don't know the types at runtime
  /*
    name match
      case name: Info.Name => println("I'm a name!")
      case repo: Info.Repo => println("I'm a repo!")
  
   */

  def printName(n: Info.Name) =
    println(n)

//  printName(name)
  // printName("zxcv") // does not compile because a String is not a Name

  val repo = Info.Repo("https://github.com")

/*
  // This doesn't work, because we don't know the types at runtime
  def printNameRepo(nameRepo: Info.NameOrRepo) =
    nameRepo match
      //case _: Info.NameOrRepo => println(s"name: $nameRepo")
      case name: Info.Name => println(s"name: $name")
      case repo: Info.Repo => println(s"repo: $repo")
      //case _ => println(s"name = repo = ")
      //case _: Info.NameAndRepo => println(s"name = $nameRepo repo = ")

  printNameRepo(name)
  printNameRepo(repo)

 */

//printNameRepo(name & repo)
