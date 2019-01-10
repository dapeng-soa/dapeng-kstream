package com.dapeng.kstream

import org.apache.kafka.streams.scala.Serdes
import org.apache.kafka.streams.scala.kstream.Consumed

object DapengStreamBuidler {

  val dapengInnerBuilder =  new DapengInnerStreamBuilder[String, String](Consumed.`with`(Serdes.String, Serdes.String))

  def topic(topic:String):DapengKStream[String,String] = {
    dapengInnerBuilder.topic(topic)
  }

}
