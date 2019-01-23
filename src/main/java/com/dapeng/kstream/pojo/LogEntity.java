package com.dapeng.kstream.pojo;


/**
 * @Author: zhup
 * @Date: 2018/11/22 10:26 AM
 */


public class LogEntity {

    private String logtime;
    private String level;
    private String sessionTid;
    private String tag;
    private String hostname;
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTagGroup() {
        return tagGroup;
    }

    public void setTagGroup(String tagGroup) {
        this.tagGroup = tagGroup;
    }

    private String threadPool;
    private String exceptionType;
    private String tagGroup;


    public String getExceptionType() {
        if (this.getMessage().contains("\n")) {
            String [] splitStr=this.getMessage().split("\n",3);
            this.exceptionType = this.getTag() + ":" + splitStr[1]==null?splitStr[0]:splitStr[1];
        } else {
            this.exceptionType = this.getTag() + ":" + this.getMessage();
        }
        return this.exceptionType;
    }

    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public String getThreadPool() {
        return threadPool;
    }

    public void setThreadPool(String threadPool) {
        this.threadPool = threadPool;
    }

    public String getLogtime() {
        return logtime;
    }

    public void setLogtime(String logtime) {
        this.logtime = logtime;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getSessionTid() {
        return sessionTid;
    }

    public void setSessionTid(String sessionTid) {
        this.sessionTid = sessionTid;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }


    public String getMailContext(){
        StringBuilder sb = new StringBuilder();
        sb.append("---------------------------------------------------------------------------------------------").append("\n");
        sb.append("***  " + String.format("%-40s", "logtime:[" + getLogtime()+"]") + " **  " + String.format("%-35s", "hostname:[" + getHostname()+"]") + "  ***\n");
        sb.append("***  " + String.format("%-40s", "sessionTid:[" + getSessionTid()+"]") + "  **  " + String.format("%-35s", "tag:[" + getTag()+"]") + " ***\n");
        sb.append("---------------------------------------------------------------------------------------------").append("\n");
        sb.append(getMessage()).append("\n");
        sb.append("\n\n");
        return sb.toString();
    }
}
