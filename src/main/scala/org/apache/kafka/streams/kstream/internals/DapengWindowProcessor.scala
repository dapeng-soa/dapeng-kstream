package org.apache.kafka.streams.kstream.internals

import java.time.Duration

import kafka.utils.Logging
import org.apache.kafka.streams.processor._
import org.apache.kafka.streams.state.KeyValueStore

class DapengWindowProcessor[K,V] (duration: Duration, counts: Int, storeName: String) extends ProcessorSupplier[K, V] with Logging  {
  override def get(): Processor[K, V] = new WindowProcessor()

  var kvStore: KeyValueStore[String, Long] = null;

  private class WindowProcessor extends AbstractProcessor[K, V] {
    override def init(context: ProcessorContext): Unit = {
      super.init(context)
      kvStore = context.getStateStore(storeName).asInstanceOf[KeyValueStore[String, Long]]
      println(" clock processor initialized.....")
      context.schedule(duration, PunctuationType.WALL_CLOCK_TIME, i => {
        info(s" start to count kvStore, hasValue: ${kvStore.all().hasNext}")
        if (kvStore.all().hasNext) {
          kvStore.all().forEachRemaining(i => {
            val counter: Long = kvStore.get(i.key)
            if (counter != null && counter > counts) {
              info(s"超过阈值， expect less than: ${counts}, actual: ${counter}")
              context.forward(i.key, i.value.toString)
            }
            kvStore.delete(i.key)
          })
        }
      })
    }

    override def process(key: K, value: V): Unit = {
      info(s"receive msg, key: ${key}, value: ${value}")
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
