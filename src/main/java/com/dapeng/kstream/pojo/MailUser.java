package com.dapeng.kstream.pojo;

import java.io.*;
import java.util.Set;

/**
 * @author huyj
 * @Created 2018-07-23 17:31
 */
public class MailUser implements Cloneable, Serializable {
    public String userName;
    public String mailsTo;
    public String logTag;
    public Set<String> phones;

    public MailUser(String userName, String mailsTo, String logTag, Set<String> phones) {
        this.userName = userName;
        this.mailsTo = mailsTo;
        this.logTag = logTag;
        this.phones = phones;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getMailsTo() {
        return mailsTo;
    }

    public void setMailsTo(String mailsTo) {
        this.mailsTo = mailsTo;
    }

    public String getLogTag() {
        return logTag;
    }

    public void setLogTag(String logTag) {
        this.logTag = logTag;
    }

    public Set<String> getPhones() {
        return phones;
    }

    public void setPhones(Set<String> phones) {
        this.phones = phones;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 深度复制，实参类必须实现Serializable接口
     *
     * @param o
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public MailUser deepCopy() {
        try {
            //先序列化，写入到流里
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream oo = new ObjectOutputStream(bo);
            oo.writeObject(this);
            //然后反序列化，从流里读取出来，即完成复制
            ByteArrayInputStream bi = new ByteArrayInputStream(bo.toByteArray());
            ObjectInputStream oi = new ObjectInputStream(bi);
            return (MailUser) oi.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
