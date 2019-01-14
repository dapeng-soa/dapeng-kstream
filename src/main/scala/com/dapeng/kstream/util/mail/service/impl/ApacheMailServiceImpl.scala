package com.dapeng.kstream.util.mail.service.impl

import com.dapeng.kstream.PropertiesUtil
import com.dapeng.kstream.util.mail.entity.{MailAttach, MailMsg}
import com.dapeng.kstream.util.mail.enums.MailMsgType
import com.dapeng.kstream.util.mail.service.MailService
import javax.activation.{CommandMap, MailcapCommandMap}
import javax.mail.internet.MimeUtility
import org.apache.commons.mail.{Email, EmailAttachment, HtmlEmail, SimpleEmail}

class ApacheMailServiceImpl extends MailService{
  /**
    * 邮件发送，默认发送者
    *
    * @param toEmail
    * @param mailMsg
    */
  def sendMail(toEmail: String, mailMsg: MailMsg): Unit = {
    if(PropertiesUtil.properties.getProperty("sendMailTest").equals("true")){
      sendMail(PropertiesUtil.DEFAULT_FROM_EMAIL,PropertiesUtil.DEFAULT_FROM_PASSWD,PropertiesUtil.DEFAULT_FROM_NAME,
        PropertiesUtil.HOST,PropertiesUtil.DEFAULT_TO_NAME,mailMsg);
    }else{
      sendMail(PropertiesUtil.DEFAULT_FROM_EMAIL,PropertiesUtil.DEFAULT_FROM_PASSWD,PropertiesUtil.DEFAULT_FROM_NAME,
        PropertiesUtil.HOST,toEmail,mailMsg);
    }
  }

  /**
    * 邮件群发，默认发送者
    *
    * @param toEmailList
    * @param mailMsg
    */
  def sendMail(toEmailList: List[String], mailMsg: MailMsg): Unit = {
    sendMail(PropertiesUtil.DEFAULT_FROM_EMAIL,PropertiesUtil.DEFAULT_FROM_PASSWD,PropertiesUtil.DEFAULT_FROM_NAME,
      PropertiesUtil.HOST, toEmailList,mailMsg)
  }

  /**
    * 邮件发送
    *
    * @param fromEmail
    * @param fromPasswd
    * @param fromName
    * @param host
    * @param toEmail
    * @param mailMsg
    */
  def sendMail(fromEmail: String, fromPasswd: String, fromName: String, host: String, toEmail: String, mailMsg: MailMsg): Unit = {
    val toEmailList = toEmail.split(",").toList
    sendMail(fromEmail, fromPasswd,fromName,host,toEmailList, mailMsg)
  }

  /**
    * 邮件群发
    *
    * @param fromEmail
    * @param fromPasswd
    * @param fromName
    * @param host
    * @param toEmailList
    * @param mailMsg
    */
  def sendMail(fromEmail: String, fromPasswd: String, fromName: String, host: String, toEmailList: List[String], mailMsg: MailMsg): Unit = {
    mailMsg.msgType match {
      case MailMsgType.text => sendTextMail(fromEmail, fromPasswd, fromName, host, toEmailList, mailMsg)
      case MailMsgType.html => sendHtmlMail(fromEmail, fromPasswd, fromName, host, toEmailList, mailMsg)
      case MailMsgType.multi => sendMultiMail(fromEmail, fromPasswd, fromName, host, toEmailList, mailMsg)
    }
  }

  def sendTextMail(fromEmail: String, fromPasswd: String, fromName: String, host: String, toEmailList: List[String], mailMsg: MailMsg) : Unit = {
    val email = new SimpleEmail
    initEmail(email, fromEmail, fromPasswd,fromName, host, toEmailList, mailMsg)
    email.setMsg(mailMsg.content)
    email.send()
  }

  def sendHtmlMail(fromEmail: String, fromPasswd: String, fromName: String, host: String, toEmailList: List[String], mailMsg: MailMsg) : Unit = {
    val email = new HtmlEmail
    initEmail(email, fromEmail, fromPasswd,fromName, host, toEmailList, mailMsg)
    email.setMsg(mailMsg.content)
    email.send()
  }

  def sendMultiMail(fromEmail: String, fromPasswd: String, fromName: String, host: String, toEmailList: List[String], mailMsg: MailMsg) : Unit = {
    val email = new HtmlEmail
    initEmail(email, fromEmail, fromPasswd,fromName, host, toEmailList, mailMsg)
    email.setHtmlMsg(mailMsg.content)
    val attachList: List[MailAttach] = mailMsg.attachList
    attachList.map((mailAttach: MailAttach) => {
      val attachment: EmailAttachment = new EmailAttachment
      attachment.setDisposition(EmailAttachment.ATTACHMENT)
      attachment.setName(MimeUtility.encodeText(mailAttach.name))
      attachment.setDescription(mailAttach.description)
      attachment.setPath(mailAttach.path)
      attachment.setURL(mailAttach.url)
      email.attach(attachment)
    })

    // add handlers for main MIME types// add handlers for main MIME types
    val mc: MailcapCommandMap = CommandMap.getDefaultCommandMap.asInstanceOf[MailcapCommandMap]
    mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html")
    mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml")
    mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain")
    mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed")
    mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822")
    CommandMap.setDefaultCommandMap(mc)
    // importance => setContextClassLoader
    Thread.currentThread.setContextClassLoader(getClass.getClassLoader)

    email.send()
  }

  def initEmail(email : Email , fromEmail: String, fromPasswd: String, fromName: String, host: String, toEmailList: List[String], mailMsg: MailMsg) : Unit = {
    email.setHostName(host)
    val conf: Array[String] = host.split(":")
    if(conf.nonEmpty) email.setHostName(conf.head)
    if(conf.length >= 2) email.setSmtpPort(conf(1).toInt)

    //邮件服务器验证：用户名/密码
    email.setAuthentication(fromEmail, fromPasswd)
    //必须放在前面，否则乱码
    email.setCharset(PropertiesUtil.CHARSET)
    email.setDebug(false);//是否开启调试默认不开启
    email.setSSLOnConnect(false);//开启SSL加密
    email.setStartTLSEnabled(false);//开启TLS加密
    val emails = toEmailList.toArray[String]
    email.addTo(emails:_*)
    email.setFrom(fromEmail,fromName)
    email.setSubject(mailMsg.subject)
  }
}
