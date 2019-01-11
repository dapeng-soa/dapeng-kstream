# dapeng-kstream

---
### 1. 基本部署流程
> 前期在部署中心页面功能未完善之前，暂时需要运维帮忙手动操作一下，后续页面操作功能完善后，业务部门就只需要关心自己的告警逻辑。

1. 用户自定义告警逻辑保存为文件并上传到服务器指定目录(如 `/opt/kstream/orderWarning.txt`) (**后面会说到具体如何定义自己的告警逻辑**)
2. 调用DapengKStream引擎 => `java -jar dapengKstream.jar -Dname=orderWarning /opt/kstream/orderWarning.txt `

### 2. 基本升级流程
1.  业务只需要修改自己的告警文件内容. 无需额外的操作。

> 我们有独立的进程去检测告警文件是否修改，如果有修改，会杀掉原有进程，并基于新的文件内容启动新的告警进程


### 3. 业务如何自定义告警逻辑

我们以以下几个需求来看如果使用这套Api: 

* 1.线上的堆栈异常要及时通过钉钉通知开发人员,同时发送错误邮件。
    ```
    topic("test")
    .dapengFilter((_,v) => v.contains("ERROR") || v.contains("Exception"))
    .sendMail("bbliang@today36524.com.cn", "订单异常")
    .sendDingding(("18588733858")

    ```
    > 上面的意思是： 订阅对应的消息主题，过滤消息包含错误或异常的消息，然后发邮件，钉钉
    
* 2.某jvm进程一分钟内FullGc次数超过2次告警。

    ```
    topic("test")
    .serviceFilter("orderService")
    .dapengFilter((_,v) => v.contains("FullGc"))
    .clockCountToWarn(Duration.ofMinutes(1), "FullGc", 2)
    ```
    >上面的意思是：订阅对应的消息主题，过滤订单的消息，过滤FullGc的消息。
    ClockCountToWarn的
    第一个参数(timeToCount)是 统计时间, 指以多久的时间段为统计维度
    第二参数(content) 是: 计数器的key, 根据该Key统计出现次数
    第三个参数(count) 是: 告警阈值, 达到该次数则告警(默认发邮件, 也可以像1一样定制发钉钉，邮件)
    
* 3.单个订单总金额超过10w或者子单个数超过1k的告警, 需业务埋点去提取关键信息
    ```
    如： 业务埋点： logger.info(s"@@@:  createOrder orderNo: ${order.orderNo} totalAmount: ${order.orderActualAmount}, orderDetailSize: ${request.orderDetails.size}")
    
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
    ```
    # 4.  在凌晨6点到第二天凌晨2点， 一分钟内没有订单产生的话告警
    topic("test")
    .serviceFilter("orderService")
    .dapengFilter((_,v) => v.contains("createOrder"))
    .clockToClockCountToWarn(2,6,Duration.ofMinutes(1), "createOrder", 0)
    
    ```
    > clockToClockCountToWarn的
    第一个参数是开始统计时间
    第二个参数是结束统计时间
    第三个参数(timeToCount)是 统计时间, 指以多久的时间段为统计维度
    第四参数(content) 是: 计数器的key, 根据该Key统计出现次数
    第五个参数(count) 是: 告警阈值, 达到该次数则告警(默认发邮件, 也可以像1一样定制发钉钉，邮件)

### 4. 提供的接口:
```
# 自定义消息过滤条件，true: 继续往下执行，false: 丢弃
def dapengFilter(p: (K,V) ⇒ Boolean):DapengKStream[K,V]

# 根据serviceTag 过滤消息
serviceFilter(serviceName: String)

# 根据日志级别过滤消息
logLevelFilter(logLevel: String)

# 自定义消息转换，用于日志消息转换处理
dapengMap[KR, VR](mapper: (K, V) => (KR, VR))

# 用于时间段统计的需求, 如前面的需求2
def clockCountToWarn(duration: Duration, keyWord: String, countTimesToWarn: Int)

# 用于有特殊时间段统计的需求,  如需求4
def clockToClockCountToWarn(timeFrom: Int, timeTo: Int, duration: Duration, keyWord: String, countTimesToWarn: Int)

# 发送钉钉消息
def sendDingding(user: String)

# 发送邮件
def sendMail(user: String, subJect: String)

# Kstream原生Api也是可以无缝支持的，如果有Kafka Kstream基础的同学可以使用原生Api
```

### 5. DapengKstream实现主要逻辑
1. 将用户输入的代码, 结合DapengStream引擎，构造成一个完整的KafkaKStream流处理主程序（当前为字符串形态）
2. 将上述构造好的代码字符串封装在一个类型为: `() => Unit` 的函数中 （当前也是 字符串心态）
3. 将封装好的函数包装在Ammonite(感兴趣的关注ammonite.io)的主函数中，将字符串翻译成可执行的函数代码，并执行,如下述伪代码
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