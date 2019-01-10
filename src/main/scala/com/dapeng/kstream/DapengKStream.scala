package com.dapeng.kstream

import org.apache.kafka.streams.scala.kstream.KStream

class DapengKStream[K,V](val innerS : org.apache.kafka.streams.scala.kstream.KStream[K, V]) extends KStream[K,V](innerS.inner){

  def dapengFilter(p: (K,V) ⇒ Boolean):DapengKStream[K,V] = {
    val kstream = innerS.filter(p)
    new DapengKStream[K, V](kstream)
  }

  def dapengMap[KR, VR](mapper: (K, V) => (KR, VR)): DapengKStream[KR, VR] = {
    val kstream = innerS.map(mapper)
    new DapengKStream[KR, VR](kstream)
  }

  def LogLevelFilter(logLevel: String) = {
    val kstream = innerS.filter((_,v) => v.asInstanceOf[String].contains(logLevel.toUpperCase()))
    new DapengKStream[K, V](kstream)
  }

  def serviceFilter(serviceName: String) = {
    val kstream = innerS.filter((_,v) => v.asInstanceOf[String].contains(serviceName))
    new DapengKStream[K, V](kstream)
  }

  def toKStream() = innerS

}
