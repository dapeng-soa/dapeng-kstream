package org.apache.kafka.streams.kstream.internals

import org.apache.kafka.streams.processor.{AbstractProcessor, Processor, ProcessorSupplier}

class DapengSendDingDingProcessor[K,V](user: String, mapper: (K,V) => (K, String), sendDingDingFunc: (String, String) => Unit) extends ProcessorSupplier[K,V] {

  override def get(): Processor[K, V] = new SendDingDingProcessor

  private class SendDingDingProcessor extends AbstractProcessor[K,V] {

    override def process(key: K, value: V): Unit = {
      //TODO: sendDingDing(user, msg)
      val (_, finalValue) = mapper(key, value)
      sendDingDingFunc(user, finalValue.toString)
      context().forward(key, value)
    }
  }
}
