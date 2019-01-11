package com.dapeng.kstream.util.mail.entity

import java.net.URL

case class MailAttach (
                        /**
                          * 附件名称
                          */
                        name : String,

                        /**
                          * 附件描述
                          */
                        description : String,

                        /**
                          * 附件文件路径，绝对路径
                          */
                        path : String,

                        /**
                          * 附件url
                          */
                        url : URL
                      )
