package com.dapeng.kstream.util.mail.service

import com.dapeng.kstream.util.mail.entity.MailMsg

/**
  * @Author: zhup
  * @Date: 2019/1/10 4:25 PM
  */
trait MailService {
  /**
    * 邮件发送，默认发送者
    * @param toEmail
    * @param mailMsg
    */
  def sendMail(toEmail : String, mailMsg : MailMsg) : Unit

  /**
    * 邮件群发，默认发送者
    * @param toEmailList
    * @param mailMsg
    */
  def sendMail(toEmailList : List[String], mailMsg : MailMsg) : Unit

  /**
    * 邮件发送
    * @param fromEmail
    * @param fromPasswd
    * @param fromName
    * @param host
    * @param toEmail
    * @param mailMsg
    */
  def sendMail(fromEmail : String, fromPasswd : String, fromName : String, host : String, toEmail : String, mailMsg : MailMsg)

  /**
    * 邮件群发
    * @param fromEmail
    * @param fromPasswd
    * @param fromName
    * @param host
    * @param toEmailList
    * @param mailMsg
    */
  def sendMail(fromEmail : String, fromPasswd : String, fromName : String, host : String, toEmailList : List[String] , mailMsg : MailMsg)

}
