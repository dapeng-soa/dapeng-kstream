package com.dapeng.kstream.util.mail.entity

import com.dapeng.kstream.util.mail.enums.MailMsgType

case class MailMsg (
                     subject : String,
                     content : String,
                     msgType : MailMsgType,
                     attachList : List[MailAttach]
                   )
