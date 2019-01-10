package com.dapeng.kstream.util

object DapengKstreamUtil {

  def getHeaderContent =
    """
         import com.dapeng.kstream.DapengInnerStreamBuilder
         import org.apache.kafka.streams.scala.Serdes
         import org.apache.kafka.streams.scala.kstream.{Consumed, Grouped, Materialized}
         import org.apache.kafka.streams.kstream.internals.DapengKStreamEnhancer._
          val builder = new DapengInnerStreamBuilder[String, String](Consumed.`with`(Serdes.String, Serdes.String))
         import builder._

      """

   def getStarter =
      """

        start("172.17.218.193:9092", "latest")
      """

  def wrapFunction(functionContent: String) = {
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
