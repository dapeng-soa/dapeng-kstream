package com.dapeng.kstream

import org.apache.kafka.streams.scala.kstream.KStream
import  com.dapeng.kstream.pojo.GcInfo

class DapengKStream[K,V](val innerS : org.apache.kafka.streams.scala.kstream.KStream[K, V]) extends KStream[K,V](innerS.inner){

  /**
    * 提供一个返回布尔值的函数:
    * k: kafka流式消息的key
    * v: kafka流式消息的value， 一般value即为: 业务接收的消息
    * @param p the provided func
    * @return DapengKStream[K,V]
    */
  def dapengFilter(p: (K,V) ⇒ Boolean):DapengKStream[K,V] = {
    val kstream = innerS.filter(p)
    new DapengKStream[K, V](kstream)
  }

  /**
    * 提供一个Key, Value转换的函数，该方法可以对消息的Key, Value 进行转换处理
    * @param mapper the function to input
    * @tparam KR new transformed Key value
    * @tparam VR new transformed Value
    * @return
    */
  def dapengMap[KR, VR](mapper: (K, V) => (KR, VR)): DapengKStream[KR, VR] = {
    val kstream = innerS.map(mapper)
    new DapengKStream[KR, VR](kstream)
  }

  /**
    * 根据消息的日志级别过滤: 如: INFO, WARN, ERROR
    * @param logLevel
    * @return
    */
  def logLevelFilter(logLevel: String) = {
    val kstream = innerS.filter((_,v) => v.asInstanceOf[String].contains(logLevel.toUpperCase()))
    new DapengKStream[K, V](kstream)
  }

  /**
    * 根据业务ServiceTag 过滤消息, 如: orderService
    * @param serviceName the serviceName for filter
    * @return
    */
  def serviceFilter(serviceName: String) = {
    val kstream = innerS.filter((_,v) => v.asInstanceOf[String].contains(serviceName))
    new DapengKStream[K, V](kstream)
  }

  def toKStream() = innerS

}
