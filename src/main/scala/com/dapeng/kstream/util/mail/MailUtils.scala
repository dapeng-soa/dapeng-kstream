package com.dapeng.kstream.util.mail

import com.dapeng.kstream.PropertiesUtil
import com.dapeng.kstream.pojo.MailUser
import com.dapeng.kstream.util.mail.entity.MailMsg
import com.dapeng.kstream.util.mail.enums.MailMsgType
import com.dapeng.kstream.util.mail.service.impl.ApacheMailServiceImpl

/**
  * @Author: zhup
  * @Date: 2019/1/11 1:27 PM
  */

object MailUtils {


  def sendEmail(toUser: String, msg: MailMsg): Unit = {
    val service = new ApacheMailServiceImpl

    service.sendMail(toUser, msg)
  }


  /**
    * 通过streamStr构建发送消息体
    *
    * @param streamStr
    */
  def sendEmail(mailsTo: String, subject: String, streamStr: String): Unit = {
    val mailMsg = new MailMsg(subject,streamStr,MailMsgType.text,null);
    sendEmail(mailsTo, mailMsg)
  }

  /**
    *
    * @param tag
    * @return 通过tag获取收件人信息
    */
  def acquireToEmailByTag(tag: String): String = acquireToUserInfoByTag(tag).getMailsTo


  def acquireToUserInfoByTag(tag:String): MailUser= PropertiesUtil.MAIL_SEND_CONFIG.get(tag)

  /**
    *
    * @param tag
    * @return 通过tag获取主题信息
    */
  def acquireSubjectByTag(tag: String): String = {
    val info = "您负责的项目 [\" + tag + \"] 产生了自定义埋点信息，请安排相关人员查看并及时处理。"
    info
  }


  def main(args: Array[String]): Unit = {
    //sendEmail("yjhu@today36524.com.cn","xiaopang","xiaopang")
    print(acquireToEmailByTag("other"))
  }
}
