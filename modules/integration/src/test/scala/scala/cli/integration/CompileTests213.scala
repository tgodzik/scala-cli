package scala.cli.integration

import scala.util.Properties

class CompileTests213 extends CompileTestDefinitions with Test213 {

  test("recompiling") {
    val initialInput =
      s"""|//> using scala ${Constants.scala213}
          |object Main extends App {
          |    val msg: String = "1"
          |    println("Hello!")
          |}
          |""".stripMargin
    TestInputs(
      os.rel / "Main.scala" -> initialInput
    ).fromRoot { root =>
      def compile() = os.proc(TestUtil.cli, "compile", "-v", "-v", "-v", ".").call(
        cwd = root,
        check = false,
        mergeErrIntoOut = true
      )

      val firstOutput = compile().toString.trim()
      assert(
        !firstOutput.contains("[error]"),
        "File did not compile succesfully:\n" + firstOutput
      )

      os.write.over(
        root / "Main.scala",
        s"""|//> using scala ${Constants.scala213}
            |object Main extends App {
            |    val msg: String = 1
            |    println("Hello!")
            |}
            |""".stripMargin
      )

      val secondOutput = compile().toString.trim()
      assert(
        secondOutput.contains("type mismatch"),
        "File did compiled succesfully, but errors were expected:\n" + secondOutput
      )

      os.write.over(
        root / "Main.scala",
        initialInput
      )

      val thirdOutput = compile().toString.trim()
      assert(
        !thirdOutput.contains("type mismatch"),
        "File did not compile succesfully:\n" + thirdOutput
      )

    }
  }

  test("test-macro-output") {
    val triple = "\"\"\""
    TestInputs(
      os.rel / "Main.scala" ->
        s"""|//> using scala ${Constants.scala213}
            |//> using dep org.scala-lang:scala-reflect:${Constants.scala213}
            |package example
            |import scala.reflect.macros.blackbox
            |import scala.language.experimental.macros
            |
            |object Scala2Example {
            |  def macroMethod[A](a: A): String =
            |    macro Scala2Example.macroMethodImpl[A]
            |
            |  def macroMethodImpl[A: c.WeakTypeTag](
            |    c: blackbox.Context
            |  )(a: c.Expr[A]): c.Expr[String] = {
            |    import c.universe._
            |    val output = s$triple$${show(a.tree)}
            |                  |$${showCode(a.tree)}
            |                  |$${showRaw(a.tree)}
            |                  |$${weakTypeTag[A]}
            |                  |$${weakTypeOf[A]}
            |                  |$${showRaw(weakTypeOf[A])}$triple.stripMargin
            |    c.echo(c.enclosingPosition, output)
            |    c.warning(c.enclosingPosition, "example error message")
            |    c.abort(c.enclosingPosition, "example error message")
            |  }
            |}
            |""".stripMargin,
      os.rel / "Test.test.scala" ->
        """|//> using test.dep org.scalameta::munit::1.0.0
           |package example
           |
           |class Tests extends munit.FunSuite {
           |  test("macro works OK") {
           |    Scala2Example.macroMethod(1 -> "test")
           |  }
           |}""".stripMargin
    ).fromRoot { root =>
      val result = os.proc(TestUtil.cli, "test", ".").call(
        cwd = root,
        check = false,
        // stdout = ProcessOutput.Readlines{ str => stringBuffer.append(str)},
        mergeErrIntoOut = true
      )
      val separator = if (Properties.isWin) "\\" else "/"

      val expectedOutput =
        s"""|Compiling project (Scala ${Constants.scala213}, JVM (17))
            |Compiled project (Scala ${Constants.scala213}, JVM (17))
            |Compiling project (test, Scala ${Constants.scala213}, JVM (17))
            |[info] .${separator}Test.test.scala:6:5
            |[info] scala.Predef.ArrowAssoc[Int](1).->[String]("test")
            |[info] scala.Predef.ArrowAssoc[Int](1).->[String]("test")
            |[info] Apply(TypeApply(Select(Apply(TypeApply(Select(Select(Ident(scala), scala.Predef), TermName("ArrowAssoc")), List(TypeTree())), List(Literal(Constant(1)))), TermName("$$minus$$greater")), List(TypeTree())), List(Literal(Constant("test"))))
            |[info] WeakTypeTag[(Int, String)]
            |[info] (Int, String)
            |[info] TypeRef(ThisType(scala), scala.Tuple2, List(TypeRef(ThisType(scala), scala.Int, List()), TypeRef(ThisType(java.lang), java.lang.String, List())))
            |[info]     Scala2Example.macroMethod(1 -> "test")
            |[info]     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
            |[error] .${separator}Test.test.scala:6:5
            |[error] example error message
            |[error]     Scala2Example.macroMethod(1 -> "test")
            |[error]     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
            |Error compiling project (test, Scala ${Constants.scala213}, JVM (17))
            |Compilation failed
            |""".stripMargin

      assertNoDiff(
        result.toString.trim().linesIterator.filterNot { str =>
          // these lines are not stable and can easily change
          val shouldNotContain = Set("Starting compilation server", "hint", "Download", "Result of")
          shouldNotContain.exists(str.contains)
        }.mkString("\n"),
        expectedOutput
      )
    }
  }
}
