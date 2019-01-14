package org.apache.kafka.streams.kstream.internals

import java.time.Duration

import kafka.utils.Logging
import org.apache.kafka.streams.processor._
import org.apache.kafka.streams.state.KeyValueStore

class DapengClockProcessor[K,V](duration: Duration, keyWord: String,
                                countTimesToWarn: Int, storeName: String,
                                warningType: String,
                                userTag: String, subject: String)
  extends ProcessorSupplier[K,V] with Logging{

  var kvStore: KeyValueStore[String, Long] = null;

  override def get(): Processor[K, V] = new ClockProcessor()

  private class ClockProcessor extends AbstractProcessor[K,V] {
    override def init(processorContext: ProcessorContext): Unit = {
      super.init(processorContext)
      kvStore = processorContext.getStateStore(storeName).asInstanceOf[KeyValueStore[String,Long]]
      println(" clock processor initialized.....")
      processorContext.schedule(duration, PunctuationType.WALL_CLOCK_TIME, (i) => {
        if (kvStore.all().hasNext) {
          kvStore.all().forEachRemaining(i => {
            val counter: Long = kvStore.get(keyWord)
            if (counter != null && counter > countTimesToWarn) {
              //TODO: sendMail
              sendWarning(warningType,userTag, subject, counter )
              info(s" ClockProcessor start to send warning mail...${i.key}, ${i.value}")
            }
            kvStore.delete(keyWord)

            processorContext.forward(i.key, i.value)
          })
        } else {
          //TODO: 告警
          warn(s" clockProcessor - not any matched msg received within ${duration.toMinutes} minutes ")
        }

      })

    }
    override def process(key: K, value: V): Unit = {
      if (value.asInstanceOf[String].contains(keyWord)) {
          val result = kvStore.get(keyWord)
          if (result != null) {
            val finalResult = result + 1
            kvStore.put(keyWord, finalResult)
          } else {
            kvStore.put(keyWord, 1)
          }
      }
    }

    private def sendWarning(warningType: String, userTag: String, subject: String, counter: Long) = {
      import com.dapeng.kstream.util.DapengWarningUtil._
      val warningContent =
        s"""
          ${duration.toMinutes} 分钟内， ${keyWord} 出现的次数超过预期, 预期 < ${countTimesToWarn}, 当前: ${counter}
        """
      warningType match {
        case "mail" => sendMailPrivate(userTag, subject, warningContent)
        case "dingding" => sendDingDing(userTag, warningContent)
        case "all" =>
          sendMailPrivate(userTag, subject, warningContent)
          sendDingDing(userTag, warningContent)
        case _ => throw new Exception(s"错误的告警类型: ${warningType}")
      }
    }
  }
}
