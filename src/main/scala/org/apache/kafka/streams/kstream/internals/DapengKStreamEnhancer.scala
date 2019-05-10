package org.apache.kafka.streams.kstream.internals

import java.time.Duration
import java.util.UUID

import com.dapeng.kstream.DapengKStream
import com.dapeng.kstream.util.dingding.DispatcherDDUtils
import com.dapeng.kstream.util.mail.MailUtils
import org.apache.kafka.streams.kstream.internals.graph.{ProcessorGraphNode, ProcessorParameters, StatefulProcessorNode}
import org.apache.kafka.streams.processor.ProcessorSupplier
import org.apache.kafka.streams.scala.{ByteArrayKeyValueStore, Serdes}
import org.apache.kafka.streams.scala.kstream.{KGroupedStream, KStream, Materialized}
import org.apache.kafka.streams.state.Stores
import com.dapeng.kstream.util.DapengWarningUtil._

object DapengKStreamEnhancer {

  implicit class KGroupedStreamEnhancer[K,V](kstream: KGroupedStream[K,V]) {
    def window(duration: Duration, counts: Int) = {
      val storeName = s"CLOCK-${UUID.randomUUID().toString}"
      toStatefulKStream(new DapengWindowProcessor[K, V](duration, counts, storeName),
        "KSTREAM-DAPENG-WINDOW-",
        false,
        storeName)
    }

    private def toStatefulKStream(processor: ProcessorSupplier[K, V], name: String, needRepartition: Boolean, storeName: String) = {
      val innerKstreamImpl: KGroupedStreamImpl[K, V] = kstream.inner.asInstanceOf[KGroupedStreamImpl[K, V]]
      val builder = innerKstreamImpl.builder
      val parentGraphNode = innerKstreamImpl.streamsGraphNode
      val pName = builder.newProcessorName(name)
      val processorParameters = new ProcessorParameters[K, V](processor, pName)

      val storeBuilder = Stores.keyValueStoreBuilder(Stores.persistentKeyValueStore(storeName), Serdes.String, Serdes.Long)

      val statefulProcessorNode = new StatefulProcessorNode(pName, processorParameters, storeBuilder, needRepartition)

      builder.addGraphNode(parentGraphNode, statefulProcessorNode)

      val kstreamJ = new KStreamImpl[K, V](pName,
        innerKstreamImpl.keySerde,
        innerKstreamImpl.valueSerde(),
        innerKstreamImpl.sourceNodes,
        needRepartition,
        statefulProcessorNode,
        builder)

      new KStream[K, V](kstreamJ)

    }
  }

  implicit class KStreamImplEnhancer[K, V](kstream: KStream[K, V]) {

    def windowAlertWithoutKey(duration: Duration, countTimesToWarn: Int, warningType: String, userTag: String, subject: String) = {
      val storeName = s"CLOCK-${UUID.randomUUID().toString}"
      toStatefulKStream(new DapengWindowAlertWithoutKeyProcessor[K, V](duration, countTimesToWarn, storeName, warningType, userTag, subject),
        "KSTREAM-CLOCK-COUNT-TO-WARN-",
        false,
        storeName)
    }

    /**
      *
      * @param duration         定时任务触发间隔
      * @param keyWord          统计的关键字
      * @param countTimesToWarn 告警阈值
      * @param warningType      发送告警类型: "mail": 发邮件, "dingding": 发钉钉, "all", 同时发邮件跟钉钉
      * @param userTag          根据ServiceTag 获取发送的用户
      * @param subject          邮件 或 钉钉的主题
      * @return
      */
    def windowAlert(duration: Duration, keyWord: String, countTimesToWarn: Int, warningType: String, userTag: String, subject: String) = {
      toStatefulKStream(new DapengWindowAlertProcessor[K, V](duration, keyWord, countTimesToWarn, s"CLOCK-${keyWord}", warningType, userTag, subject),
        "KSTREAM-CLOCK-COUNT-TO-WARN-",
        false,
        s"CLOCK-${keyWord}")
    }

    /**
      * 该方式适用于有定时启动范围的需求： 如2点到6点内，统计每分钟的指定消息
      * @param timeFrom 开始范围
      * @param timeTo   结束范围
      * @param duration 定时间隔
      * @param keyWord  统计的关键消息
      * @param countTimesToWarn 告警统计阈值
      * @param warningType 发送告警类型: "mail": 发邮件, "dingding": 发钉钉, "all", 同时发邮件跟钉钉
      * @param userTag  根据ServiceTag 获取发送的用户
      * @param subject  邮件 或 钉钉的主题
      * @return KStream[K,V]
      */
    def timeRangeAlert(timeFrom: Int, timeTo: Int,
                        duration: Duration, keyWord: String, countTimesToWarn: Int,
                        warningType: String, userTag: String, subject: String) = {
      toStatefulKStream(
        new DapengTimeRangeAlertProcessor[K, V](timeFrom, timeTo, duration,
          keyWord,
          countTimesToWarn,
          s"CLOCK-TO-CLOCK-${keyWord}",
          warningType,
          userTag,
          subject),
        "KSTREAM-CLOCK-TO-CLOCK-COUNT-TO-WARN-",
        false,
        s"CLOCK-TO-CLOCK-${keyWord}")
    }

    def toDapengKStream() = {
      new DapengKStream[K, V](kstream)
    }

    def sendDingding(user: String, mapper: (K,V) => (K, String)) = {
      val sendDingDingFunc = (user: String, msg: String) => sendDingDing(user, msg)
      toKstream(new DapengSendDingDingProcessor[K, V](user,mapper, sendDingDingFunc), "KSTREAM-SEND-DINGDING-", false)
    }

    /**
      * 根据ServiceTag获取用户组，并根据设置的标题发送邮件
      * @param user 业务用户组, 如：orderService
      * @param subject 邮件标题
      * @return KStream[K,V]
      */
    def sendMail(user: String, subject: String) = {
      val sendMailFunc = (user: String, subJect: String, msg: V) => sendMailPrivate(user, subJect, msg.asInstanceOf[String])
      toKstream(new DapengSendMailProcessor[K, V](user, subject, sendMailFunc), "KSTREAM-SEND-MAIL-", false)
    }



    private def toKstream(processor: ProcessorSupplier[K, V], name: String, needRepartition: Boolean) = {
      val innerKstreamImpl: KStreamImpl[K, V] = kstream.inner.asInstanceOf[KStreamImpl[K, V]]
      val parentGraphNode = innerKstreamImpl.streamsGraphNode
      val builder = innerKstreamImpl.builder
      val pName = builder.newProcessorName(name)
      val processorParameters = new ProcessorParameters[K, V](processor, pName)

      val processorNode = new ProcessorGraphNode(pName, processorParameters, needRepartition)

      builder.addGraphNode(parentGraphNode, processorNode)

      val kstreamJ = new KStreamImpl[K, V](pName,
        innerKstreamImpl.keySerde,
        innerKstreamImpl.valueSerde(),
        innerKstreamImpl.sourceNodes,
        needRepartition,
        processorNode,
        builder)

      new KStream[K, V](kstreamJ)
    }

    private def toStatefulKStream(processor: ProcessorSupplier[K, V], name: String, needRepartition: Boolean, storeName: String) = {
      val innerKstreamImpl: KStreamImpl[K, V] = kstream.inner.asInstanceOf[KStreamImpl[K, V]]
      val builder = innerKstreamImpl.builder
      val parentGraphNode = innerKstreamImpl.streamsGraphNode
      val pName = builder.newProcessorName(name)
      val processorParameters = new ProcessorParameters[K, V](processor, pName)

      val storeBuilder = Stores.keyValueStoreBuilder(Stores.persistentKeyValueStore(storeName), Serdes.String, Serdes.Long)

      val statefulProcessorNode = new StatefulProcessorNode(pName, processorParameters, storeBuilder, needRepartition)

      builder.addGraphNode(parentGraphNode, statefulProcessorNode)

      val kstreamJ = new KStreamImpl[K, V](pName,
        innerKstreamImpl.keySerde,
        innerKstreamImpl.valueSerde,
        innerKstreamImpl.sourceNodes,
        needRepartition,
        statefulProcessorNode,
        builder)

      new KStream[K, V](kstreamJ)

    }
  }


}
