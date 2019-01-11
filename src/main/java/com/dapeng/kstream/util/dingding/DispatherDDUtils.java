package com.dapeng.kstream.util.dingding;


import com.dapeng.kstream.PropertiesUtil;
import com.dapeng.kstream.util.HttpUtils;

import java.util.*;

/**
 * 接入钉钉工具类
 *
 * @author huyj
 * @Created 2018-07-24 12:51
 */
public class DispatherDDUtils {


    public static boolean sendMessageToDD(Set<String> atPeoples, Map markDownMap) {
        String _response;
        if (PropertiesUtil.properties.getProperty("sendDingDingTest").equals("true")) {
            _response = HttpUtils.doPostJson(PropertiesUtil.properties.getProperty("DD_TOKEN_TEST"), buildMdMsgSendDDMap(atPeoples, markDownMap), "UTF-8");
        } else {
            _response = HttpUtils.doPostJson(PropertiesUtil.properties.getProperty("DD_TOKEN"), buildMdMsgSendDDMap(atPeoples, markDownMap), "UTF-8");
        }
        return true;
    }

    public static boolean sendMessageToDD(Set<String> atPeoples,String title,String text){
        Map map = new HashMap();
        map.put("title",title);
        map.put("text",text);
        return sendMessageToDD(atPeoples,map);
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

        Set set =new HashSet();
        set.add("xiaopang");
        sendMessageToDD(set,"test","test");
    }

}
