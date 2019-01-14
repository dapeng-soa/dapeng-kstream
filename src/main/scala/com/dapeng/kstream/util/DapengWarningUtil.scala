package com.dapeng.kstream.util

import com.dapeng.kstream.util.dingding.DispatcherDDUtils
import com.dapeng.kstream.util.mail.MailUtils

object DapengWarningUtil {

  def sendDingDing(tag: String, msg: String): Unit = {
    val mailUser = MailUtils.acquireToUserInfoByTag(tag)
    DispatcherDDUtils.sendMessageToDD(mailUser.getPhones,MailUtils.acquireSubjectByTag(tag),msg)
  }

  //TODO: zhupeng 提供
  def sendMailPrivate(tag: String, subJect: String, msg: String) = {
    MailUtils.sendEmail(MailUtils.acquireToUserInfoByTag(tag).mailsTo,subJect,msg)
  }
}
