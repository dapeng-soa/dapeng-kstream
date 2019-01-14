package org.apache.kafka.streams.kstream.internals

import java.time.Duration

import com.dapeng.kstream.DapengKStream
import com.dapeng.kstream.util.dingding.DispatcherDDUtils
import com.dapeng.kstream.util.mail.MailUtils
import org.apache.kafka.streams.kstream.internals.graph.{ProcessorGraphNode, ProcessorParameters, StatefulProcessorNode}
import org.apache.kafka.streams.processor.ProcessorSupplier
import org.apache.kafka.streams.scala.Serdes
import org.apache.kafka.streams.scala.kstream.KStream
import org.apache.kafka.streams.state.Stores

object DapengKStreamEnhancer {

  implicit class KStreamImplEnhancer[K,V](kstream: KStream[K,V]) {

    def clockCountToWarn(duration: Duration, keyWord: String, countTimesToWarn: Int, userTag: String, subject: String) = {
      toStatefulKStream(new DapengClockProcessor[K,V](duration, keyWord,countTimesToWarn, s"CLOCK-${keyWord}", userTag, subject),
        "KSTREAM-CLOCK-COUNT-TO-WARN-",
        false,
        s"CLOCK-${keyWord}")
    }

    def clockToClockCountToWarn(timeFrom: Int, timeTo: Int, duration: Duration, keyWord: String, countTimesToWarn: Int) = {
      toStatefulKStream(
        new DapengClockToClockProcessor[K,V](timeFrom,timeTo,duration, keyWord,countTimesToWarn, s"CLOCK-TO-CLOCK-${keyWord}"),
        "KSTREAM-CLOCK-TO-CLOCK-COUNT-TO-WARN-",
        false,
        s"CLOCK-TO-CLOCK-${keyWord}")
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

    private def sendDingDing(tag: String, msg: V) = {
      val mailUser = MailUtils.acquireToUserInfoByTag(tag)
      DispatcherDDUtils.sendMessageToDD(mailUser.getPhones,MailUtils.acquireSubjectByTag(tag),msg.toString)
    }

    //TODO: zhupeng 提供
    private def sendMailPrivate(tag: String, subJect: String, msg: V) = {
      MailUtils.sendEmail(MailUtils.acquireToUserInfoByTag(tag).mailsTo,subJect,msg.toString)
    }

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
        needRepartition,
        processorNode,
        builder)

      new KStream[K,V](kstreamJ)
    }

    private def toStatefulKStream(processor: ProcessorSupplier[K,V], name: String, needRepartition: Boolean, storeName: String) = {
      val innerKstreamImpl: KStreamImpl[K, V] = kstream.inner.asInstanceOf[KStreamImpl[K,V]]
      val builder = innerKstreamImpl.builder
      val parentGraphNode = innerKstreamImpl.streamsGraphNode
      val pName = builder.newProcessorName(name)
      val processorParameters = new ProcessorParameters[K,V](processor,pName)

      val storeBuilder = Stores.keyValueStoreBuilder( Stores.persistentKeyValueStore(storeName), Serdes.String, Serdes.Long)

      val statefulProcessorNode = new StatefulProcessorNode(pName, processorParameters, storeBuilder, needRepartition)

      builder.addGraphNode(parentGraphNode, statefulProcessorNode)

      val kstreamJ = new KStreamImpl[K,V](pName,
        innerKstreamImpl.keySerde,
        innerKstreamImpl.valueSerde(),
        innerKstreamImpl.sourceNodes,
        needRepartition,
        statefulProcessorNode,
        builder)

      new KStream[K,V](kstreamJ)

    }
  }




}
