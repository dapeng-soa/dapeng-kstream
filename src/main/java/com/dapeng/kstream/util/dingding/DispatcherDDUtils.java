package com.dapeng.kstream.util.dingding;


import com.dapeng.kstream.PropertiesUtil;
import com.dapeng.kstream.pojo.MailUser;
import com.dapeng.kstream.util.HttpUtils;
import com.dapeng.kstream.util.mail.MailUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 接入钉钉工具类
 *
 * @author huyj
 * @Created 2018-07-24 12:51
 */
public class DispatcherDDUtils {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public static void sendMessageToDD(Set<String> atPeoples, Map markDownMap) {
        if (PropertiesUtil.SEND_DD_TEST) {
            HttpUtils.doPostJson(PropertiesUtil.DD_TOKEN_TEST, buildMdMsgSendDDMap(atPeoples, markDownMap), "UTF-8");
        } else {
            HttpUtils.doPostJson(PropertiesUtil.DD_TOKEN, buildMdMsgSendDDMap(atPeoples, markDownMap), "UTF-8");
        }
    }

    public static void sendLogHtmlToDD(Set<String> atPeoples, Map markDownMap) {
        if (PropertiesUtil.SEND_DD_TEST) {
            HttpUtils.doPostJson(PropertiesUtil.DD_TOKEN_TEST, buildHtmlMsgSendDDMap(atPeoples, markDownMap), "UTF-8");
        } else {
            HttpUtils.doPostJson(PropertiesUtil.DD_TOKEN, buildHtmlMsgSendDDMap(atPeoples, markDownMap), "UTF-8");
        }
    }

    public static void sendMessageToDD(MailUser users, String tag, String text) {
        Map map = new HashMap();
        StringBuffer buffer = new StringBuffer();
        map.put("title", MailUtils.acquireSubjectByTag(tag));
        buffer.append("\n").append("#### <font color=#FFA500>【重要】</font> Dear " + users.getUserName() + ":").append("\n");
        buffer.append("\n").append("---").append("\n");
        buffer.append("\n").append("&#8194;&#8194;您负责的项目 [" + tag + "] 出现异常，请安排相关人员查看错误原因并及时处理。确保系统正常运行！").append("\n");
        buffer.append("\n").append(text).append("\n");
        map.put("text", buffer.append("\n\n").toString());
        String splitStr = text.split(",")[3].split(":")[1];
        String sessionTid = splitStr.substring(1, splitStr.length() - 1);
        sendMessageToDD(users.getPhones(), map);
        if (!sessionTid.isEmpty()) {
            map.put("sessionTid",sessionTid);
            map.put("htmlTitle","Dear: " + users.getUserName());
            sendLogHtmlToDD(users.getPhones(), map);
        }
    }


    public static Map buildMdMsgSendDDMap(Set<String> atPeoples, Map markDownMap) {
        StringBuffer atBuffer = new StringBuffer();
        atBuffer.append("\n").append("---").append("\n").append("<font color=#FF3030> ");
        for (String phone : atPeoples) {
            atBuffer.append("@" + phone + " ");
        }
        atBuffer.append(" </font>");
        markDownMap.put("text", markDownMap.get("text") + atBuffer.toString());

        Map root = putMap(null, "msgtype", "markdown");
        putMap(root, "markdown", markDownMap);
        putMap(root, "at", putMap(putMap(null, "atMobiles", atPeoples), "isAtAll", false));
        return root;
    }

    public static Map buildHtmlMsgSendDDMap(Set<String> atPeoples, Map markDownMap) {
        String sessionTid = markDownMap.get("sessionTid").toString();
        String logIndexDate = formatter.format(LocalDateTime.now());
        Map root = putMap(null, "msgtype", "link");
        Map linkMap = putMap(null, "text", markDownMap.get("title"));
        putMap(linkMap, "title", markDownMap.get("htmlTitle"));
        putMap(linkMap, "picUrl", PropertiesUtil.MESSAGE_IMAGE_URL);
        putMap(linkMap, "messageUrl", PropertiesUtil.ES_QUERY_HOST + "?sessionTid=" + sessionTid + "&index=" + logIndexDate);
        putMap(root, "link", linkMap);
        return root;
    }

    public static Map putMap(Map map, String key, Object value) {
        if (map == null) {
            map = new HashMap();
        }
        map.put(key, value);
        return map;
    }


    public static void main(String[] arg0) {
        //sendMessageToDDTest("test");
        //sendMdMessage();

/*        Set set =new HashSet();
        set.add("xiaopang");
        sendMessageToDD(set,"test","test");*/
    }

}
