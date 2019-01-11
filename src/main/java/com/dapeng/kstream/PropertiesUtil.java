package com.dapeng.kstream;

import com.dapeng.kstream.pojo.MailUser;
import com.sun.deploy.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: zhup
 * @Date: 2019/1/11 3:31 PM
 */


public class PropertiesUtil {

    public static Properties properties;

    public static Map<String,MailUser> MAIL_SEND_CONFIG;

    static {
        properties = new Properties();
        //InputStream in = MailCfg.class.getClassLoader().getResourceAsStream("mailConfig.properties");
        String path = System.getProperty("user.dir");
        String propertiesPath = path + File.separator + "mailConfig.properties";
        try {
            properties.load(new InputStreamReader(new FileInputStream(propertiesPath), "utf-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        MAIL_SEND_CONFIG = new HashMap<String, MailUser>();
        Set<String> frameworkConfig = new HashSet<>(Arrays.asList(properties.getProperty("frameworkMember").split("[,]")));
        Set<String> frameworkMail = frameworkConfig.stream().map(item -> item.substring(0, item.lastIndexOf("|"))).collect(Collectors.toSet());
        Set<String> frameworkPhone = frameworkConfig.stream().map(item -> item.substring(item.lastIndexOf("|") + 1)).collect(Collectors.toSet());


        MAIL_SEND_CONFIG.put("other", new MailUser("架构服务部成员", StringUtils.join(frameworkMail, ","), "daepng-soa framework", frameworkPhone));
        properties.stringPropertyNames().forEach(item -> {
            //加载项目负责人信息
            if (item.contains("errorMail")) {
                String logTag = item.split("_")[1];
                String[] pro_value = properties.getProperty(item).split("#");
                if (pro_value.length >= 2) {
                    String userName = pro_value[0];
                    Set<String> userConfig = new HashSet<>(Arrays.asList(pro_value[1].split("[,]")));

                    Set<String> userConfigMail = userConfig.stream().map(itemMail -> itemMail.substring(0, itemMail.lastIndexOf("|"))).collect(Collectors.toSet());
                    Set<String> userMail = new HashSet<>(frameworkMail);
                    userConfigMail.addAll(userMail);

                    Set<String> userConfigPhone = userConfig.stream().map(itemPhone -> itemPhone.substring(itemPhone.lastIndexOf("|") + 1)).collect(Collectors.toSet());
                    /*Set<String> userPbone = new HashSet<>(frameworkPhone);
                    userConfigPhone.addAll(userPbone);*/
                    MAIL_SEND_CONFIG.put(logTag, new MailUser(userName, StringUtils.join(userConfigMail, ","), logTag, userConfigPhone));
                }
            }

        });

    }

    /**
     * 邮件服务器
     */
    public final static String HOST = properties.getProperty("mail.smtp.host");

    /**
     * 邮件编码
     */
    public final static String CHARSET = properties.getProperty("mail.charset");

    /**
     * 发送者邮箱
     */
    public final static String DEFAULT_FROM_EMAIL = properties.getProperty("mail.sender.username");

    /**
     * 发送者密码
     */
    public final static String DEFAULT_FROM_PASSWD = properties.getProperty("mail.sender.password");

    /**
     * 发送者名称
     */
    public final static String DEFAULT_FROM_NAME = properties.getProperty("mail.from.name");

    public final static String DEFAULT_TO_NAME = properties.getProperty("mail.to.email");



}
