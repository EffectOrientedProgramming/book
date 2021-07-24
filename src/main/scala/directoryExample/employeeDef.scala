package directoryExample

case class Employee(
    ID: Int,
    firstName: String,
    lastName: String,
    department: String
):

  def getName: String =
    val name = s"$firstName $lastName"
    name

  override def toString =
    s"Name: $firstName $lastName. Department: $department. ID: $ID \n"

  def map = this
end Employee
