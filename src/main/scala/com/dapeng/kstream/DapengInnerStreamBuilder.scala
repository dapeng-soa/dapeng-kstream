package com.dapeng.kstream

import java.time.Duration
import java.util.{Properties, UUID}

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.scala.{Serdes, StreamsBuilder}

class DapengInnerStreamBuilder[K,V](consumed: Consumed[K,V]) {

  private val innerStreamBuilder = new StreamsBuilder()
  private var innerTopic: String = _

  def topic(topic:String):DapengKStream[K,V] = {
    innerTopic = topic
    val kstream = innerStreamBuilder.stream[K, V](topic)(consumed)
    new DapengKStream[K, V](kstream)
  }

  /**
    *
    * @param server kakfa服务IpPort: localhost:9092
    * @param offset What to do when there is no initial offset in Kafka or if the current offset does not exist any more on the server (e.g. because that data has been deleted): <ul><li>earliest: automatically reset the offset to the earliest offset<li>latest: automatically reset the offset to the latest offset</li><li>none: throw exception to the consumer if no previous offset is found for the consumer's group</li><li>anything else: throw exception to the consumer.</li></ul>
    * @return
    */
  def start(server: String, offset: String) = {
    val props = {
      val p = new Properties()
      p.put(StreamsConfig.APPLICATION_ID_CONFIG, s"$innerTopic-${UUID.randomUUID()}")
      p.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, server)
      p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offset)
      p.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String.getClass.getName)
      p.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String.getClass.getName)
      p
    }

    val kstream = new KafkaStreams(innerStreamBuilder.build(), props)
    kstream.start()

    sys.ShutdownHookThread {
      kstream.close(Duration.ofSeconds(10))
    }
  }
}

