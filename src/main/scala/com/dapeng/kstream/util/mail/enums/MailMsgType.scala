package com.dapeng.kstream.util.mail.enums

class MailMsgType private(val id: Int, val name: String)

object MailMsgType {

  val text = new MailMsgType(1, "文本")

  val html = new MailMsgType(2, "html")

  val multi = new MailMsgType(3, "复合")

}
