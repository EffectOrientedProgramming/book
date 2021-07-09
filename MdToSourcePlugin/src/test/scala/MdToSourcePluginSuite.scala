class MdToSourcePluginSuite extends munit.FunSuite {

  test("convert kotlin method signature to scala form") {
    val kotlinSignature =
      "def buildNumberToContactMap(contactList: List<Contact>): Map<String, Contact> {"

    assertEquals(
      KotlinConversion.convert(kotlinSignature),
      Some(
        "def buildNumberToContactMap(contactList: List<Contact>): Map<String, Contact> ="
      )
    )
  }

  test("convert line containing only a closing brace to a blank line") {
    assertEquals(
      KotlinConversion.convert("    }    "),
      None
    )

  }

  test("not affect this line") {
    val unaffectedLine =
      """// {{incomplete}} TODO Can we define some different behavior for examples that deliberately don't compile?"""
    assertEquals(
      KotlinConversion.convert(unaffectedLine),
      Some(unaffectedLine)
    )
  }

  test("converts a function that contains braced if/else") {

    val original =
      """
      |   private def crossBoundary(coordinate: Int): Int {
      |     val inBounds = coordinate % fieldSize
      |     return if (inBounds < 0) {
      |       fieldSize + inBounds
      |     } else {
      |       inBounds
      |     }
      |   }""".stripMargin

    val expected =
      """
      |   private def crossBoundary(coordinate: Int): Int =
      |     val inBounds = coordinate % fieldSize
      |     return if (inBounds < 0) 
      |       fieldSize + inBounds
      |     else
      |       inBounds""".stripMargin
    assertEquals(
      original.split("\n").flatMap(KotlinConversion.convert).mkString("\n"),
      expected
    )
    println(original)
  }

  test("doesn't affect quoted lines") {
    val original =
      """ "if (condition) 'a' else 'b'}")  // [2] """.stripMargin
      val expected =
        Some(""" "if (condition) 'a' else 'b'}")  // [2] """.stripMargin)

    assertEquals(KotlinConversion.convert(original), expected)
  }

  test("eliminate opening brace after a for") {
    val original = "  for (i in start..end) {"
    val expected = Some("  for (i in start..end) ")
    assertEquals(KotlinConversion.convert(original), expected)
  }

  test("doesn't affect lines that are part of a template") {
    val original =
      """**[2]** `$if(condition) 'a' else 'b'}` evaluates and substitutes the result"""
      val expected = Some(original)
      assertEquals(KotlinConversion.convert(original), expected)
  }

  test("converts the end of of a function signature on its own line") {
    val original =
      "): String {"

    val expected =
      "): String ="

    assertEquals(
      KotlinConversion.convert(original),
      Some(expected)
    )
  }

  test("leaves braces in empty function bodies") {
    val original = "def v(s: String, vararg d: Double) {}"
    val expected = "def v(s: String, vararg d: Double) = {}"
    assertEquals(
      KotlinConversion.convert(original),
      Some(expected)
    )
  }

  test("ignores opening brace in this funky line") {
    val original =
      """ contactsByNumber eq "{1-234-567890=Contact('Miffy', '1-234-567890'), """"
    val expected = original
    assertEquals(
      KotlinConversion.convert(original),
      Some(expected)
    )
  }

  test("convert first line of class definition to use a colon") {
    val original = " class Hamster(val name: String) {"
    val expected = " class Hamster(val name: String):"
    assertEquals(
      KotlinConversion.convert(original),
      Some(expected)
    )
  }

  test("convert else's where only the first part was braced") {
    val original = "} else"
    val expected = "else"
    assertEquals(
      KotlinConversion.convert(original),
      Some(expected)
    )
  }

  test("converts interfaces to traits while also removing braces") {
    val original = "interface Hotness {"
    val expected = "trait Hotness:"
    assertEquals(
      KotlinConversion.convert(original),
      Some(expected)
    )
  }

  test("convert while loops") {
    val original = "   while (condition(i)) {         // [2] "
    val expected = "   while (condition(i))          // [2] "
    assertEquals(
      KotlinConversion.convert(original),
      Some(expected)
    )

  }

  test("converts enums") {
    val original = " enum class Suit { "
    val expected = " enum Suit: "
    assertEquals(
      KotlinConversion.convert(original),
      Some(expected)
    )
  }

  test(
    "converts tricky angle brace situations to square braces without hitting lambdas or comparisons"
  ) {

    val original1 = "ReadOnlyProperty<Person, String> { _, _ ->"
    val original2 = "   property: KProperty<*>"
    val original3 = "  operator def List<String>.getValue("
    val original4 = "  private var list = listOf<E>()"
    val original5 = "  val info = mutableMapOf<String, Any?>("
    val original6 =
      """  var captain: String by observable("<0>") {""" // should leave this alone
    val original7 = "  def show(prop: KProperty1<Properties, *>) ="
    val original8 = "class CrateList<T> : ArrayList<Crate<T>>():"
    val original9 =
      "by upcasting from `Box<Cat>` to `OutBox<Any>` and from `Box<Any>` to"
  }

  test("should convert import wildcards from '*' to '_' ") {
    val original = " import atomictest.*"
    val expected = " import atomictest._"
  }

}
