# dapeng-kstream

---

dapeng-kstream开发的初衷，由于kafka-kstream有一定的学习成本，为了让业务同事能够快速的定制自己的业务告警逻辑，通过dapeng-kstream的封装，业务同事只需要关注自己的消息处理逻辑，同时封装后的dapeng-kstream针对我们的dapeng部署中心会更加容易扩展，目前0.1-SNAPSHOT版本，还需要业务同事自己写一个业务相关的代码。
> 后续业务同事可以在部署中心页面定制自己的业务告警逻辑，而不需要再自己写代码，敬请期待.


### 1. 基本部署流程
> 前期在部署中心页面功能未完善之前，暂时需要运维帮忙手动操作一下，后续页面操作功能完善后，业务部门就只需要关心自己的告警逻辑。

1. 用户自定义告警逻辑保存为文件并上传到服务器指定目录(如 `/opt/kstream/orderWarning.txt`) (**后面会说到具体如何定义自己的告警逻辑**)
2. 调用DapengKStream引擎 => `java -jar dapengKstream.jar -Dname=orderWarning /opt/kstream/orderWarning.txt `

### 2. Dapeng-kstream 提供的拓展接口:
```java
/**
* 提供一个返回布尔值的函数: true: 下一个节点继续处理该消息，false: 丢弃
* k: kafka流式消息的key
* v: kafka流式消息的value， 一般value即为: 业务接收的消息
* @param p the provided func
* @return DapengKStream[K,V]
*/
def dapengFilter(p: (K,V) ⇒ Boolean):DapengKStream[K,V]

/**
* 根据业务ServiceTag 过滤消息, 如: orderService
* @param serviceName the serviceName for filter
* @return DapengKStream[K,V]
*/
def serviceFilter(serviceName: String):DapengKStream[K,V]

// 根据日志级别过滤消息
/**
* 根据消息的日志级别过滤: 如: INFO, WARN, ERROR
* @param logLevel the logLevel to input
* @return DapengKStream[K,V]
*/
def logLevelFilter(logLevel: String)

/**
* 提供一个Key, Value转换的函数，该方法可以对消息的Key, Value 进行转换处理
* @param mapper the function to input
* @tparam KR new transformed Key value
* @tparam VR new transformed Value
* @return
*/
def dapengMap[KR, VR](mapper: (K, V) => (KR, VR)): DapengKStream[KR, VR]

/**
  *
  * @param duration 定时任务触发间隔
  * @param keyWord 统计的关键字
  * @param countTimesToWarn 告警阈值
  * @param warningType 发送告警类型: "mail": 发邮件, "dingding": 发钉钉, "all", 同时发邮件跟钉钉
  * @param userTag  根据ServiceTag 获取发送的用户
  * @param subject  邮件 或 钉钉的主题
  * @return KStream[K,V]
  */
def clockCountToWarn(duration: Duration, 
                    keyWord: String, 
                    countTimesToWarn: Int, 
                    warningType:String, 
                    userTag: String, 
                    subject: String)

/**
  * 该方式适用于有定时启动范围的需求： 如2点到6点内，统计每分钟的指定消息
  * @param timeFrom 开始范围
  * @param timeTo   结束范围
  * @param duration 定时间隔
  * @param keyWord  统计的关键消息
  * @param countTimesToWarn 告警统计阈值
  * @param warningType 发送告警类型: "mail": 发邮件, "dingding": 发钉钉, "all", 同时发邮件跟钉钉
  * @param userTag  根据ServiceTag 获取发送的用户
  * @param subject  邮件 或 钉钉的主题
  * @return KStream[K,V]
  */
def clockToClockCountToWarn(timeFrom: Int, 
                            timeTo: Int,
                            duration: Duration, 
                            keyWord: String,         
                            countTimesToWarn: Int,
                            warningType: String, 
                            userTag: String, 
                            subject: String): KStream[K,V]

/**
  * 根据ServiceTag获取用户组，并发送钉钉消息
  * @param user 业务用户组, 如: orderService
  * @return
  */
def sendDingding(userTag: String)

/**
  * 根据ServiceTag获取用户组，并根据设置的标题发送邮件
  * @param user 业务用户组, 如：orderService
  * @param subject 邮件标题
  * @return KStream[K,V]
  */
def sendMail(user: String, subject: String)

```
> 注: Dapeng-KStream原生支持Kstream， 如果熟悉KStream Api的也可以使用原生的Kafka-KStreamApi无缝接入

### 3. 业务如何自定义告警逻辑

我们以以下几个需求来看如果使用这套Api: 
> 业务需要把如下代码复制到某一文件中，具体的升级流程，详见 `### 1. 基本部署流程`

* 1.线上的堆栈异常要及时通过钉钉通知开发人员,同时发送错误邮件。
    ```java
    topic("order_topic")
    .dapengFilter((_,v) => v.contains("ERROR") || v.contains("Exception"))
    .sendMail("orderService", "订单错误异常告警")
    .sendDingding(("orderService")

    ```
    > 上面的意思是： 订阅对应的消息主题，过滤消息包含错误或异常的消息，然后发邮件，钉钉
    
* 2.某jvm进程一分钟内FullGc次数超过2次告警。

    ```java
    topic("efk")
    .serviceFilter("orderService")
    .dapengFilter((_,v) => v.contains("FullGc"))
    .clockCountToWarn(Duration.ofMinutes(1), 
            "ERROR",
             2,
            "all", //（all： 包含发邮件，钉钉告警, 详见2的APi接口参数）
            "orderService", 
            "[订单错误统计告警]")
    ```
    >上面的意思是：订阅对应的消息主题，根据serviceTag：`orderService` 过滤订单的消息，根据消息内容:`FullGc`过滤后，每隔1分钟统计该消息，如果次数等于或超过2次后，发送 邮件,钉钉告警，同时主题是: "[订单错误统计告警]", 内容为原消息， 如果需要针对消息做进一步加工转换的话，可以在dapengFilter后 接一个dapengMap的函数, 如:
    ```java
    topic("efk")
    .serviceFilter("orderService")
    .dapengFilter((_,v) => v.contains("FullGc"))
    .dapengMap((k,v) => s"FullGc高过预期: $v")
    .clockCountToWarn(Duration.ofMinutes(1), 
            "ERROR",
             2,
            "all", //（all： 包含发邮件，钉钉告警, 详见2的APi接口参数）
            "orderService", 
            "[订单错误统计告警]")
    ```

    

    
* 3.单个订单总金额超过10w或者子单个数超过1k的告警, 需业务埋点去提取关键信息
    ```java
    //如： 业务埋点： logger.info(s"@@@:  createOrder orderNo: ${order.orderNo} totalAmount: ${order.orderActualAmount}, orderDetailSize: ${request.orderDetails.size}")
    
    val matcher = "@@@: (createOrder orderNo: )(\\w+)(, totalAmount: )(-?\\d+.?\\d*)(, orderDetailSize: )(\\d+)".r
    
    topic("test")
    .serviceFilter("orderService")
    .dapengFilter((_,v) => v.contains("@@@:"))
    .dapengMap((k,v) => { //业务自定义格式切割
        val r = matcher findAllMatchIn v map( i => { i.group(4).toDouble})
        val amount = if (r.hasNext) r.next() else 0D
        (k,String.valueOf(amount))
    })
    .filter((k,v) => v.toDouble > 1000)
    .sendMail("bbliang@today36524.com.cn", "订单异常")
    ```
    
* 4.在凌晨6点到第二天凌晨2点， 一分钟内没有订单产生的话告警
    ```java
    // 4.  在凌晨6点到第二天凌晨2点， 一分钟内没有订单产生的话告警
    topic("test")
    .serviceFilter("orderService")
    .dapengFilter((_,v) => v.contains("createOrder"))
    .clockToClockCountToWarn(2,
            6,
            Duration.ofMinutes(1), 
            "createOrder",
            0,
            "all",
            "orderService", 
            "[一分钟内没有订单创建，请查看]")
    ```

### 4. 基本升级流程
1.  业务只需要修改自己的告警文件内容. 无需额外的操作。

> 我们有独立的进程去检测告警文件是否修改，如果有修改，会杀掉原有进程，并基于新的文件内容启动新的告警进程

### 附录：DapengKstream实现主要逻辑（伪代码）
1. 将用户输入的代码, 结合DapengStream引擎，构造成一个完整的KafkaKStream流处理主程序（当前为字符串形态）
2. 将上述构造好的代码字符串封装在一个类型为: `() => Unit` 的函数中 （当前也是 字符串心态）
3. 将封装好的函数包装在Ammonite(感兴趣的关注`ammonite.io`)的主函数中，将字符串翻译成可执行的函数代码，并执行,如下述伪代码
```
val input = s"$userInput"
val header = s"$initializeCode"
val runner = s"startFuncCode"
val func = header + input + runner
val execFunction = wrapFunction(func)

val file = new File("doAction.sc")
val outputStream = new FileOutputStream(file)
outputStream.write(execFunction.getBytes())
outputStream.close()

val result: (Res[Any], Seq[(Path, Long)]) = ammonite.Main().runScript(Path(file.getAbsolutePath), Seq(("args", Option.empty)))

result._1 match {
  case Success(x: (() => Unit)) =>
    println("matched function. start to execute...")
    x()
  case Success(x: (Any => Any)) => x()
  case Success(x: (Any => Unit)) => x()
  case _ => throw new Exception(s"非法函数...${result._1}")
}
```