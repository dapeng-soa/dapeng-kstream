package org.apache.kafka.streams.kstream.internals

import org.apache.kafka.streams.processor.{AbstractProcessor, Processor, ProcessorContext, ProcessorSupplier}

class DapengDoActionProcessor[K,V](f: String => String) extends ProcessorSupplier[K,V] {

  override def get(): Processor[K, V] = new DoActionProcessor()

  private class DoActionProcessor extends AbstractProcessor[K,V] {
    override def init(context: ProcessorContext): Unit = {
      //context.schedule()
      super.init(context)
    }
    override def process(key: K, value: V): Unit = {
      println(s" received msg: ${key},  value: ${value}")
      context().forward(key, f(value.asInstanceOf[String]))
    }
  }

}
