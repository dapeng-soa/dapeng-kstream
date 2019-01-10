package com.lbb.test

import java.io.{File, FileOutputStream}
import java.time.Duration

import ammonite.ops.Path
import ammonite.util.Res
import ammonite.util.Res.Success
import com.dapeng.kstream.DapengInnerStreamBuilder
import org.apache.kafka.streams.scala.Serdes
import org.apache.kafka.streams.scala.kstream.{Consumed, Grouped, Materialized}
import org.apache.kafka.streams.kstream.internals.DapengKStreamEnhancer._

object KstreamTest {

  def main(args: Array[String]): Unit = {
    val input =
      """
        topic("test")
         .dapengFilter((_,v) => v.contains("ERROR") || v.contains("Exception"))
          .dapengMap((k,v) => (k, s"订单异常，请注意: $v") )
          .sendMail("bbliang@today36524.com.cn", "订单异常")
          .sendDingding("18588733858")
      """

    val header =
      """
         import com.dapeng.kstream.DapengInnerStreamBuilder
         import org.apache.kafka.streams.scala.Serdes
         import org.apache.kafka.streams.scala.kstream.{Consumed, Grouped, Materialized}
         import org.apache.kafka.streams.kstream.internals.DapengKStreamEnhancer._
          val builder = new DapengInnerStreamBuilder[String, String](Consumed.`with`(Serdes.String, Serdes.String))
         import builder._
      """

    val runner =
      """
        start("172.17.218.193:9092", "latest")
      """

    val func = header + input + runner
    val execFunction = wrapFunction(func)

    val file = new File("doAction.sc")
    val outputStream = new FileOutputStream(file)
    outputStream.write(execFunction.getBytes())
    outputStream.close()

    println(s" file absolutePath: ${file.getAbsolutePath}" )


    val result: (Res[Any], Seq[(Path, Long)]) = ammonite.Main().runScript(Path(file.getAbsolutePath), Seq(("args", Option.empty)))

    result._1 match {
      case Success(x: (() => Unit)) =>
        println("matched function. start to execute...")
        x()
      case Success(x: (Any => Any)) => x()
      case Success(x: (Any => Unit)) => x()
      case _ => throw new Exception(s"非法函数...${result._1}")
    }

  }


  private def wrapFunction(functionContent: String) = {
    val functionHeader =
      """
         import ammonite.main.Router.main

         @main
         def main(args: String) = {
           val func = () => {
      """

    val functionFooter =
      """}
         func
      }
      """


    functionHeader + functionContent + functionFooter
  }
}
