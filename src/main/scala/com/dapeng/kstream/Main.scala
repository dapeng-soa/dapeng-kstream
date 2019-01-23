package com.dapeng.kstream

import java.io.{BufferedOutputStream, File, FileNotFoundException, FileOutputStream}

import ammonite.ops.Path
import ammonite.util.Res
import ammonite.util.Res.Success
import com.dapeng.kstream.util.DapengKstreamUtil._
import org.slf4j.LoggerFactory
import com.dapeng.kstream.DapengInnerStreamBuilder
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.scala.Serdes
import org.apache.kafka.streams.scala.kstream.{Consumed, Grouped, Materialized}
import org.apache.kafka.streams.kstream.internals.DapengKStreamEnhancer._
import com.dapeng.kstream.pojo.GcInfo
import java.time.Duration

object Main {

  val logger = LoggerFactory.getLogger(classOf[KafkaStreams])

  var kafkaStreams: KafkaStreams = null

  def main(args: Array[String]) = {

    if (args == null || args.isEmpty) {
      throw new Exception(" Please input dapeng-kstream file content absolute path..")
    }

    val fileName = args(0)
    val file = new File(fileName)
    if (!file.exists()) {
      throw new FileNotFoundException(s" File not found: ${fileName}")
    }

    logger.info(s" received function file: ${file.getAbsolutePath}. start to boot kstream..")
    val fileSource = scala.io.Source.fromFile(fileName)
    val inputFile = fileSource.mkString

    if (inputFile.isEmpty) {
      throw new Exception(s" 配置告警的文件内容不能为空...${fileName}")
    }

    val func = getHeaderContent + inputFile + getStarter
    val execFuc = wrapFunction(func)


    val execFile = if (file.getName.indexOf(".") == -1) {
      new File(s"${file.getName}_executed.sc")
    } else {
      new File(s"${file.getName.substring(0, file.getName.lastIndexOf("."))}_executed.sc")
    }

    logger.info(s" the final executed fileName: ${execFile}")
    val outputStream = new BufferedOutputStream(new FileOutputStream(execFile))
    try {
      outputStream.write(execFuc.getBytes())
      outputStream.close()

      val result: (Res[Any], Seq[(Path, Long)]) = ammonite.Main().runScript(Path(execFile.getAbsolutePath), Seq(("args", Option.empty)))

      kafkaStreams = result._1 match {
        case Success(f: (() => KafkaStreams)) =>
          logger.info(s"matched function. start to execute..${fileName}.")
          f()
//        case Success(x: (Any => Any)) => x()
//        case Success(x: (Any => Unit)) => x()
        case _ => throw new Exception(s"非法函数...${result._1}")
      }

    } catch {
      case e: Exception => logger.error(s" Failed to execute function.....${e.getMessage}", e)
    } finally {
      outputStream.close()
    }

    kafkaStreams
  }
}
