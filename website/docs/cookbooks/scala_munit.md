---
title: Testing your code with Munit
sidebar_position: 4
---

You can use Scala CLI to test your code compatibility with various versions of `java`, with a key point being that manual installation of a JDK/SDK is not required(!).
Scala CLI automatically downloads the Java version you specify.

As an example, the following snippet uses the new method `Files.writeString` from Java 11:

```scala title=Main.scala
object cube{
  def double(i: Int) = i * i
}
```

```scala title=Test.scala
//> using lib "org.scalameta::munit::0.7.27"
//> using scala "2.13.8"
//> using nativeVersion "0.4.4"
import scala.concurrent.Future

class MyTests extends munit.FunSuite {

  implicit val ec = scala.concurrent.ExecutionContext.global

  test("foo") {
    assert(cube.double(2) == 4)
  }  
  
  test("foo-future") {
    Future {
      Thread.sleep(1000)
      assertEquals(cube.double(3), 8)
    }
  }

  override def afterAll(): Unit = {
    println("Finished!")
  }

  override def beforeAll(): Unit = {
    println("Started!")
  }
}
```

```bash ignore
scala-cli test .
```

<!-- ignored Expected:
Compiling project (Scala 2.13.8, JVM)
Compiled project (Scala 2.13.8, JVM)
MyTests:
Started!
  + foo 0.045s
==> X MyTests.foo-future  1.034s munit.ComparisonFailException: /home/tgodzik/Documents/workspaces/munit-cookbook/test.scala:16
15:      Thread.sleep(1000)
16:      assertEquals(cube.double(3), 8)
17:    }
values are not the same
=> Obtained
9
=> Diff (- obtained, + expected)
-9
+8
    at munit.FunSuite.assertEquals(FunSuite.scala:11)
    at MyTests.$anonfun$new$5(test.scala:16)
    at scala.concurrent.Future$.$anonfun$apply$1(Future.scala:678)
    at scala.concurrent.impl.Promise$Transformation.run(Promise.scala:467)
    at java.util.concurrent.ForkJoinTask$RunnableExecuteAction.exec(ForkJoinTask.java:1426)
    at java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:290)
    at java.util.concurrent.ForkJoinPool$WorkQueue.topLevelExec(ForkJoinPool.java:1020)
    at java.util.concurrent.ForkJoinPool.scan(ForkJoinPool.java:1656)
    at java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:1594)
    at java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:183)
Finished!

-->
