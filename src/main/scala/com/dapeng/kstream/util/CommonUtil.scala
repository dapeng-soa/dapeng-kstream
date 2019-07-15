package com.dapeng.kstream.util

import java.io.{File, FileInputStream}
import java.util.Properties
import collection.JavaConverters._

object CommonUtil {

  val LogMatcher = """(?<logtime>^\d{2}-\d{2} \d{2}:\d{2}:\d{2} \d{3}) (?<threadPool>[^ ]+|Check idle connection Thread) (?<level>[^ ]+) \[(?<sessionTid>\w*)\] - (?<message>.*)""".r

  def loadSystemProperties(file: File): Unit = {
    if (file.canRead) {
      val properties = new Properties()
      properties.load(new FileInputStream(file))

      val results = properties.keySet().asScala.map(_.toString)
      results.foreach(keyString => {
        System.setProperty(keyString, properties.getProperty(keyString))
      })
    }
  }

  def decodeUnicode[K,V](value: V): V = {
    val string = new StringBuilder
    val unicode = value.toString
    val hex = unicode.split("\\\\u")
    for (subHex <- hex) {
      try {
        if (subHex.length >= 4) {
          val chinese = subHex.substring(0, 4)
          try {
            val chr = Integer.parseInt(chinese, 16)
            if (isChinese(chr.toChar)) {
              string.append(chr.toChar)
              string.append(subHex.substring(4))
            }
            else string.append(subHex)
          } catch {
            case e: NumberFormatException =>
              string.append(subHex)
          }
        }
        else string.append(subHex)
      }
      catch {
        case e: NumberFormatException =>
          string.append(subHex)
      }
    }
    string.toString.asInstanceOf[V]
  }

  def isChinese(c: Char): Boolean = {
    val ub = Character.UnicodeBlock.of(c)
    (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) || (ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS) || (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A) || (ub == Character.UnicodeBlock.GENERAL_PUNCTUATION) || (ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION) || (ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS)
  }

  /*  def main(args: Array[String]): Unit = {
      var str = "TODAY\\u672a今\\u767b天\\u9646\\uff01"

      println(decodeUnicode(str))
    }*/
}
