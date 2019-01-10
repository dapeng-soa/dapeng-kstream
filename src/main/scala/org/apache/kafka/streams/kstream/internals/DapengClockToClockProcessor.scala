package org.apache.kafka.streams.kstream.internals

import java.time.Duration

import org.apache.kafka.streams.processor._
import org.apache.kafka.streams.state.KeyValueStore

class DapengClockToClockProcessor[K,V](Timefrom: Int, TimeTo: Int, duration: Duration, keyWord: String, countTimesToWarn: Int) extends ProcessorSupplier[K,V] {

  var kvStore: KeyValueStore[String, Long] = null;

  override def get(): Processor[K, V] = new ClockProcessor()

  private class ClockProcessor extends AbstractProcessor[K,V] {
    override def init(processorContext: ProcessorContext): Unit = {
      super.init(processorContext)
      kvStore = processorContext.getStateStore("order-counts").asInstanceOf[KeyValueStore[String,Long]]
      processorContext.schedule(duration, PunctuationType.WALL_CLOCK_TIME, (i) => {
        if (kvStore.all().hasNext) {
          kvStore.all().forEachRemaining(i => {
            val counter: Long = kvStore.get(keyWord)
            if (counter != null && counter > countTimesToWarn) {
              //TODO: sendMail
            }
            kvStore.delete(keyWord)

            processorContext.forward(i.key, i.value)
          })
        } else {
          //TODO: 告警
        }

      })

    }
    override def process(key: K, value: V): Unit = {
      if (value.asInstanceOf[String].contains(keyWord)) {
          val result = kvStore.get(keyWord)
          if (result != null) kvStore.put(keyWord, result + 1) else kvStore.put(keyWord, 1)
      }
    }
  }
}
