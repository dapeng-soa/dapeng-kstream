package org.apache.kafka.streams.kstream.internals

import java.time.Duration

import com.dapeng.kstream.DapengKStream
import org.apache.kafka.streams.kstream.internals.graph.{ProcessorGraphNode, ProcessorParameters}
import org.apache.kafka.streams.processor.ProcessorSupplier
import org.apache.kafka.streams.scala.kstream.KStream

object DapengKStreamEnhancer {

  def main(args: Array[String]): Unit = {
    val lis = List(1,2,3)
    println(lis.filter(_ > 1))
  }

  implicit class KStreamImplEnhancer[K,V](kstream: KStream[K,V]) {

    def clockCountToWarn(duration: Duration, keyWord: String, countTimesToWarn: Int) = {
      toKstream(new DapengClockProcessor[K,V](duration, keyWord,countTimesToWarn),"KSTREAM-CLOCK-COUNT-TO-WARN-", false)
    }

    def clockToClockCountToWarn(timeFrom: Int, timeTo: Int, duration: Duration, keyWord: String, countTimesToWarn: Int) = {
      toKstream(new DapengClockToClockProcessor[K,V](timeFrom,timeTo,duration, keyWord,countTimesToWarn),"KSTREAM-CLOCK-TO-CLOCK-COUNT-TO-WARN-", false)
    }

    def toDapengKStream() = {
      new DapengKStream[K, V](kstream)
    }

    def sendDingding(user: String) = {
      val sendDingDingFunc = (user: String, msg: V) => sendDingDing(user, msg)
      toKstream(new DapengSendDingDingProcessor[K,V](user, sendDingDingFunc),"KSTREAM-SEND-DINGDING-", false)
    }

    def sendMail(user: String, subJect: String) = {
      val sendMailFunc = (user: String, subJect: String, msg: V) => sendMailPrivate(user,subJect,msg)
      toKstream(new DapengSendMailProcessor[K,V](user, subJect, sendMailFunc),"KSTREAM-SEND-MAIL-", false)
    }

    private def sendDingDing(user: String, msg: V) = {

    }

    //TODO: zhupeng 提供
    private def sendMailPrivate(user: String, subJect: String, msg: V) = {

    }

//    def doAction(f: String => String) = {
//      toKstream(new DapengDoActionProcessor[K,V](f), "KSTREAM-DOACTION-", false)
//    }

    private def toKstream(processor: ProcessorSupplier[K,V], name: String, needRepartition: Boolean) = {
      val innerKstreamImpl: KStreamImpl[K, V] = kstream.inner.asInstanceOf[KStreamImpl[K,V]]
      val parentGraphNode = innerKstreamImpl.streamsGraphNode
      val builder = innerKstreamImpl.builder
      val pName = builder.newProcessorName(name)
      val processorParameters = new ProcessorParameters[K,V](processor,pName)

      val processorNode = new ProcessorGraphNode(pName, processorParameters, needRepartition)
      builder.addGraphNode(parentGraphNode, processorNode)

      val kstreamJ = new KStreamImpl[K,V](pName,
        innerKstreamImpl.keySerde,
        innerKstreamImpl.valueSerde(),
        innerKstreamImpl.sourceNodes,
        true,
        processorNode,
        builder)

      new KStream[K,V](kstreamJ)
    }
  }




}
