package adts.eitherand

import java.net.URL

enum Info:
  case Name(s: String)
  case Repo(url: URL) extends Info
  case NameAndRepo(name: Name, repo: Repo)
  // We only say "case" instead of "case class", because a case class is a separate thing,
  // that isn't necessarily one of the enum values

  extension (n: Info.Name) def fancyRepS() = n.s.toUpperCase

  def fancyRep() = this match {
    case name: adts.eitherand.Info.Name => println(name.fancyRepS())
//    case _: adts.eitherand.Info.Repo => println()
    case _ => println("other")
  }

@main def run =
  import Info.*
  val name2: Info = Info.Name("asdf")
  name2.fancyRep()

  val name: Info.Name = Info.Name("asdf")

  name.copy(s = "foo")
  val repo: Info.Repo = Info.Repo(URL("https://github.com"))

  def printNameRepo(nameRepo: Info) =
    nameRepo match
      case Info.Name(name) => println(s"name: $name")
      case Info.Repo(repo) => println(s"repo: $repo")
      case Info.NameAndRepo(Info.Name(name), Info.Repo(repo)) =>
        println(s"name: $name repo: $repo")

  printNameRepo(name)
  printNameRepo(repo)
  printNameRepo(Info.NameAndRepo(name, repo))
