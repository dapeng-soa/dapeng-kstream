package com.dapeng.kstream

import java.io.{File, FileNotFoundException, FileOutputStream}

import ammonite.ops.Path
import ammonite.util.Res
import ammonite.util.Res.Success
import com.dapeng.kstream.util.DapengKstreamUtil._
import org.slf4j.LoggerFactory
import com.dapeng.kstream.DapengInnerStreamBuilder
import org.apache.kafka.streams.scala.Serdes
import org.apache.kafka.streams.scala.kstream.{Consumed, Grouped, Materialized}
import org.apache.kafka.streams.kstream.internals.DapengKStreamEnhancer._

object Main {

  val logger = LoggerFactory.getLogger("Main")

  def main(args: Array[String]): Unit = {

    if (args == null || args.isEmpty) {
      throw new Exception(" Please input dapeng-kstream file content absolute path..")
    }

    val fileName = args(0)
    val file = new File(fileName)
    if (!file.exists()) {
      throw new FileNotFoundException(s" File not found: ${fileName}")
    }

    val fileSource = scala.io.Source.fromFile(fileName)
    val inputFile = fileSource.mkString

    val func = getHeaderContent + inputFile + getStarter
    val execFuc = wrapFunction(func)

    val execFile = new File(s"${file.getName}_executed.sc")
    logger.info(s" the final executed fileName: ${execFile}")
    val outputStream = new FileOutputStream(execFile)
    try {
      outputStream.write(execFuc.getBytes())

      val result: (Res[Any], Seq[(Path, Long)]) = ammonite.Main().runScript(Path(execFile.getAbsolutePath), Seq(("args", Option.empty)))

      result._1 match {
        case Success(f: (() => Unit)) =>
          println("matched function. start to execute...")
          f()
        case Success(x: (Any => Any)) => x()
        case Success(x: (Any => Unit)) => x()
        case _ => throw new Exception(s"非法函数...${result._1}")
      }

    } catch {
      case e: Exception => logger.error(s" Failed to execute function.....${e.getMessage}", e)
    } finally {
      outputStream.close()
    }

  }
}
