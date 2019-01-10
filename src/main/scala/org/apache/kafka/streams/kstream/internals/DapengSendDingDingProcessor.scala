package org.apache.kafka.streams.kstream.internals

import org.apache.kafka.streams.processor.{AbstractProcessor, Processor, ProcessorSupplier}

class DapengSendDingDingProcessor[K,V](user: String, sendDingDingFunc: (String, V) => Unit) extends ProcessorSupplier[K,V] {

  override def get(): Processor[K, V] = new SendDingDingProcessor

  private class SendDingDingProcessor extends AbstractProcessor[K,V] {

    override def process(key: K, value: V): Unit = {
      println(s"sending dingding to ${user}, msg: ${value}")
      //TODO: sendDingDing(user, msg)
      sendDingDingFunc(user, value)
      context().forward(key, value)
    }
  }
}
