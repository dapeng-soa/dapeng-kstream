package org.apache.kafka.streams.kstream.internals

import org.apache.kafka.streams.processor.{AbstractProcessor, Processor, ProcessorSupplier}

class DapengSendMailProcessor[K,V](user: String, subJect: String, sendMailFunc: (String, String, V) => Unit) extends ProcessorSupplier[K,V] {

  override def get(): Processor[K, V] = new SendMailProcessor

  private class SendMailProcessor extends AbstractProcessor[K,V] {

    override def process(key: K, value: V): Unit = {
      println(s"sending email to ${user}, msg: ${value}")
      sendMailFunc(user,subJect,value)
      //TODO: sendEmail(user)
      context().forward(key, value)
    }
  }
}
