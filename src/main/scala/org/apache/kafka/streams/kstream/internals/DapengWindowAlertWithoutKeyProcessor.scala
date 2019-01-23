package org.apache.kafka.streams.kstream.internals

import java.time.Duration

import com.dapeng.kstream.util.DapengWarningUtil.sendWarning
import kafka.utils.Logging
import org.apache.kafka.streams.processor._
import org.apache.kafka.streams.state.KeyValueStore

class DapengWindowAlertWithoutKeyProcessor[K, V](duration: Duration,
                                                 countTimesToWarn: Int, storeName: String,
                                                 warningType: String,
                                                 userTag: String, subject: String)
  extends ProcessorSupplier[K, V] with Logging {

  var kvStore: KeyValueStore[String, Long] = null;

  override def get(): Processor[K, V] = new ClockProcessor()

  private class ClockProcessor extends AbstractProcessor[K, V] {
    override def init(processorContext: ProcessorContext): Unit = {
      super.init(processorContext)
      kvStore = processorContext.getStateStore(storeName).asInstanceOf[KeyValueStore[String, Long]]
      info(" clock processor initialized.....")
      processorContext.schedule(duration, PunctuationType.WALL_CLOCK_TIME, (i) => {
        if (kvStore.all().hasNext) {
          kvStore.all().forEachRemaining(i => {
            info(s" current processing kvStore key: ${i.key}, value: ${i.value}")
            val counter: Long = kvStore.get(i.key)
            if (counter != null && counter > countTimesToWarn) {
              //TODO: sendMail
              val warningContent =
                s"""
                  ${duration.toMinutes} 分钟内， ${i.key} 出现的次数超过预期, 预期 < ${countTimesToWarn}, 当前: ${counter}
                """
              sendWarning(warningType, userTag, subject, warningContent)
              println(s" ClockProcessor start to send warning mail...${i.key}, ${i.value}")
            } else {
              warn(" counter less than ")
            }
            kvStore.delete(i.key)

            processorContext.forward(i.key, i.value)
          })
        } else {
          //TODO: 告警
          warn(s" clockProcessor - not any matched msg received within ${duration.toMinutes} minutes ")
        }

      })

    }

    override def process(key: K, value: V): Unit = {
      info(s" received msg: key: ${key}, value: ${value}")
      val result = kvStore.get(key.toString)
      if (result != null) {
        val finalResult = result + 1
        kvStore.put(key.toString, finalResult)
      } else {
        kvStore.put(key.toString, 1)
      }
    }

  }

}
