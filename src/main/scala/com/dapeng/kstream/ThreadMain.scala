package com.dapeng.kstream

import java.io._
import java.util
import java.util.concurrent._

import com.dapeng.kstream.Main
import com.dapeng.kstream.util.CommonUtil
import org.apache.commons.lang.StringUtils
import org.apache.kafka.streams.KafkaStreams
import org.slf4j.LoggerFactory

import collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

object ThreadMain {

  val logger = LoggerFactory.getLogger(classOf[KafkaStreams])

  def main(args: Array[String]): Unit = {

    /*
     * 使用map来管理线程， Key: FunctionFileName, value: FunctionThread
     * 由于只有主线程会使用该Map, 所以不需要使用线程安全的ConcurrentHashMap
     * 使用Map来管理线程是因为需要使用FunctionFileName来销毁，创建线程，而线程池没办法通过Name(functionFileName)来获取对应的的线程
     */
    val threadMaps = new java.util.HashMap[String, Thread](16)

    val mainCheckingThread = Executors.newScheduledThreadPool(1, (r: Runnable) => {
      new Thread(r, "DAPENG-KSTREAM-MAIN-THREAD")
    })

    CommonUtil.loadSystemProperties(new File("kstreams.properties"))

    val workPath = System.getProperty("WORK_HOME")
    logger.info(s" current work_path: ${workPath}")
    if (StringUtils.isEmpty(workPath)) {
      throw new Exception(" WORK_HOME could not be empty. please set WORK_HOME env before your run this application.")
    }

    val workDir = new File(workPath)
    if (!workDir.exists()) {
      logger.info(s" no workDir found..start to create new Dir..${workPath}")
      workDir.mkdir()
    }

    val configPath = new File(workDir + "/config")
    val runningInfoFile = new File(configPath + "/running.info")
    if (!configPath.exists()) {
      logger.info(s" no config_dir found..start to create new config_dir:.${configPath}")
      configPath.mkdir()
      runningInfoFile.createNewFile()
    } else {
      //running.info store the running status of DapengKstream
      //config format like: action.sc lastModifyTime
      if (!runningInfoFile.exists()) {
        logger.info(s" no running.info config file found.. start to create new running.info file: ${runningInfoFile.getAbsolutePath}")
        runningInfoFile.createNewFile()
      }
    }

    //the functions path stores the biz application files
    val functionsPath = new File(workDir + "/functions")
    if (!functionsPath.exists()) {
      logger.info(s" no functions path foud. start to create new functions dir: ${functionsPath.getAbsolutePath}")
      functionsPath.mkdir()
    }

    mainCheckingThread.scheduleWithFixedDelay(() => {
      logger.info(" start to check dapeng-kstream functoins files..")

      val funcDir = functionsPath
      val files: Array[File] = funcDir.listFiles()
      if (files == null || files.length <= 0) {
        logger.info(" No function files found. start to shutdown every running dapeng-kstream thread..")
        //清除运行信息
        clearFileContent(runningInfoFile)
        //关闭运行的程序
        threadMaps.asScala.foreach(i => while (!i._2.isInterrupted || i._2.isAlive) {
          interruptThread(i._2)
          Thread.sleep(1000)
        })
        threadMaps.clear()

      } else {
        logger.info(s" functions file found...file size: ${files.length}, threadMaps.size: ${threadMaps.size()}")

        clearOldThreads(files.toList, threadMaps)

        files.toList.foreach(funcFile => {
          updateRunningInfo(runningInfoFile, funcFile)

          needRestartDapengKStream(funcFile, runningInfoFile) match {
            case true => restartDapengKStream(funcFile, threadMaps)
            case false => if (!threadMaps.containsKey(funcFile.getName)) startDapengKStream(funcFile, threadMaps)
          }

        })
      }
    },10, 10, TimeUnit.SECONDS)

    sys.ShutdownHookThread {
      threadMaps.asScala.foreach(i => {
        interruptThread(i._2)
      })
    }

  }

  private def needRestartDapengKStream(functionFile: File, runningInfo: File) = {
    val configContent = readFile(runningInfo)
    configContent.find(_.contains(functionFile.getName)) match {
      case Some(info) =>
        val infos = info.split(" ")
        logger.info(s" compare lastModifyTime..infoLastModifyTime: ${infos(1).toLong}, functionFileModifyTime: ${functionFile.lastModified()}")
        infos(1).toLong < functionFile.lastModified()
      case None => false
    }
  }

  private def interruptThread(t: Thread) = {
    try {
      logger.info(s" start to interrupt running thread, threadName: ${t.getName}")
      t.interrupt()
    } catch {
      case e: InterruptedException => logger.warn(s"failed to interrupt current thread, ${t.getName}, error: ${e.getMessage}")
      case e: Exception => logger.error(s" failed to interrupt current thread.. ${t.getName}, error: ${e.getMessage}")
    } finally {
      logger.info(s" current interrupting thread ${t.getName} status: ${t.getState}, isInterrupted: ${t.isInterrupted}")
    }
  }

  private def readFile(file: File) = {
    Source.fromFile(file).getLines()
  }

  private def writeFile(file: File, content: String) = {
    val outputStream = new BufferedWriter(new FileWriter(file))
    try {
      outputStream.write(content)
      outputStream.flush()
      outputStream.close()
    } catch {
      case e: Exception => logger.error(s" failed to write file , msg: ${e.getMessage}", e)
    } finally {
      outputStream.close()
    }
  }

  private def clearOldThreads(functionFiles: List[File], threadMaps: java.util.HashMap[String, Thread]): Unit = {
    if (threadMaps.size() > functionFiles.size) {
      threadMaps.asScala.filterNot(i => functionFiles.map(_.getName).contains(i._1)).foreach(t => {
        logger.info(s" start to clean unused thread. threadName: ${t._2.getName}")
        while (!t._2.isInterrupted || t._2.isAlive) {
          interruptThread(t._2)
          Thread.sleep(1000)
        }
        threadMaps.remove(t._1)
      })
    }
  }

  private def clearFileContent(file: File) = {
    writeFile(file, "")
  }

  private def updateRunningInfo(runningInfoFile: File, currentFuncFile: File) = {
    logger.info(s" current function need to restart. file: ${currentFuncFile.getName}")
    val configContent = readFile(runningInfoFile)
    logger.info(s"runningInfo oriContent: ${configContent}")
    val newConfigContent = configContent.filterNot(_.contains(currentFuncFile.getName)) ++ List(s"${currentFuncFile.getName} ${currentFuncFile.lastModified()}")
    logger.info(s" runningInfo updatedContent: ${newConfigContent}")
    val strBuilder = new StringBuilder(128)
    newConfigContent.zipWithIndex.foreach(i => {
      if (i._2 != 0) {
        strBuilder.append("\r\n").append(i._1)
      } else {
        strBuilder.append(i._1)
      }
    })
    writeFile(runningInfoFile, strBuilder.toString())
  }

  private def startDapengKStream(currentFuncFile: File, threadMaps: java.util.HashMap[String, Thread]) = {
    logger.info(s"start to reRun dapeng-kstream.....${currentFuncFile.getName},")
    val dapengStreamThread = new DapengStreamThread(currentFuncFile.getName, currentFuncFile.getAbsoluteFile)
    threadMaps.put(currentFuncFile.getName, dapengStreamThread)
    dapengStreamThread.start()
    logger.info(s" dapeng-kstream restarted: ${currentFuncFile.getName}, current thread MapSize: ${threadMaps.asScala.keys.toList}")
  }

  private def restartDapengKStream(currentFuncFile: File, threadMaps: java.util.HashMap[String, Thread]) = {
    val runningFunc = threadMaps.get(currentFuncFile.getName)
    if (runningFunc != null) {
      interruptThread(runningFunc)
    } else {
      logger.warn(s" found need to restart functionFile, but no thread instance found in threadMap: ${currentFuncFile.getName}")
    }
    startDapengKStream(currentFuncFile: File, threadMaps: java.util.HashMap[String, Thread])
  }

}

class DapengStreamThread(threadName: String, functionFile: File) extends Thread {

  override def run() {
    setName(threadName)
    super.run()
    Main.main(Array(functionFile.getAbsolutePath))
  }
}
