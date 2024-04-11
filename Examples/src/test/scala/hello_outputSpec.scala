
object hello_outputSpec extends ZIOSpecDefault:
  def spec = suite("suite")(
    test("test0"):
      assertTrue:
        "asdf" == "zxcv"
      // Result: Test FAILED
    ,
    test("test1"):
      assertTrue:
        "asdf" == "asdf"
      // Result: Test PASSED
  )
