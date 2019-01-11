package com.dapeng.kstream.util

import java.io.{BufferedReader, IOException, InputStreamReader, UnsupportedEncodingException}
import java.net.URLEncoder
import java.util
import java.util.{Map, Set}
import com.google.gson.Gson


import org.apache.commons.httpclient.{HttpClient, HttpException, HttpStatus, MultiThreadedHttpConnectionManager}
import org.apache.commons.httpclient.methods.{GetMethod, PostMethod, RequestEntity, StringRequestEntity}

/**
  * @Author: zhup
  * @Date: 2019/1/10 4:45 PM
  */

object HttpUtils {
  /**
    * 定义编码格式 UTF-8
    */
  val URL_PARAM_DECODECHARSET_UTF8 = "UTF-8"
  /**
    * 定义编码格式 GBK
    */
  val URL_PARAM_DECODECHARSET_GBK = "GBK"
  private var connectionManager = new MultiThreadedHttpConnectionManager()
  private val connectionTimeOut = 25000
  private val socketTimeOut = 25000
  private val maxConnectionPerHost = 20
  private val maxTotalConnections = 20

  connectionManager.getParams().setConnectionTimeout(connectionTimeOut)
  connectionManager.getParams().setSoTimeout(socketTimeOut)
  connectionManager.getParams().setDefaultMaxConnectionsPerHost(maxConnectionPerHost)
  connectionManager.getParams().setMaxTotalConnections(maxTotalConnections)

  private var client = new HttpClient(connectionManager)


  /**
    * 无参数的POST方式提交数据<br>
    * 编码默认为UTF-8
    *
    * @param url 待请求的URL
    * @return 响应结果
    */
  def doPost(url: String): String = doPost(url, null, URL_PARAM_DECODECHARSET_UTF8)

  /**
    * POST方式提交数据<br>
    * 编码默认为UTF-8
    *
    * @param url 待请求的URL
    * @return 响应结果
    */
  def doPost(url: String, params: util.Map[String, String]): String = {
    System.out.println(params)
    doPost(url, params, URL_PARAM_DECODECHARSET_UTF8)
  }

  /**
    * POST方式提交数据
    *
    * @param url    待请求的URL
    * @param params 要提交的数据
    * @param enc    编码
    * @return 响应结果
    * @throws IOException IO异常
    */
  def doPost(url: String, params: util.Map[String, String], enc: String): String = {
    var response:String = null
    var postMethod:PostMethod = null
    try {
      postMethod = new PostMethod(url)
      postMethod.setRequestHeader("Content-Type", "application/json;charset=" + enc)
      //postMethod.setRequestHeader("Content-Type", "application/x-www-form-urlencoded;charset=" + enc);
      if (params != null) { //将表单的值放入postMethod中
        val keySet = params.keySet
        import scala.collection.JavaConversions._
        for (key <- keySet) {
          val value = params.get(key)
          postMethod.addParameter(key, if (value == null) ""
          else value)
        }
      }
      //执行postMethod
      val statusCode = client.executeMethod(postMethod)
      if (statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_CREATED || statusCode == HttpStatus.SC_BAD_REQUEST)
        response = postMethod.getResponseBodyAsString
      else
        System.out.println("响应状态码 = " + postMethod.getStatusCode)
    } catch {
      case e: HttpException =>
        System.out.println("发生致命的异常，可能是协议不对或者返回的内容有问题:" + e)
        e.printStackTrace()
      case e: IOException =>
        System.out.println("发生网络异常:" + e)
        e.printStackTrace()
    } finally if (postMethod != null) {
      postMethod.releaseConnection()
      postMethod = null
    }
    response
  }


  /**
    * 据Map生成URL字符串
    *
    * @param map      Map
    * @param valueEnc URL编码
    * @return URL
    */
  private def getUrl(map: util.Map[String, String], valueEnc: String) = {
    val url = new StringBuffer
    val set = map.entrySet
    import scala.collection.JavaConversions._
    for (entry <- set) {
      val key = entry.getKey
      val value = if (entry.getValue == null) ""
      else entry.getValue
      try
        url.append(key).append("=").append(URLEncoder.encode(value, valueEnc)).append("&")
      catch {
        case e: UnsupportedEncodingException =>
          e.printStackTrace()
      }
    }
    if (url.lastIndexOf("&") > -1) url.substring(0, url.lastIndexOf("&"))
    else url.toString
  }


  /**
    * POST方式提交数据
    *
    * @param url    待请求的URL
    * @param params 要提交的数据
    * @param enc    编码
    * @return 响应结果
    * @throws IOException IO异常
    */
  def doPostJson(url: String, paramsJson: String, enc:String): String = {
    var response = ""
    var postMethod:PostMethod = null
    try {
      postMethod = new PostMethod(url)
      //postMethod.setRequestHeader("Content-Type", "application/x-www-form-urlencoded;charset=" + enc);
      postMethod.setRequestHeader("Content-Type", "application/json;charset=" + enc)
      val entity = new StringRequestEntity(paramsJson, "application/json", "UTF-8")
      postMethod.setRequestEntity(entity)
      //执行postMethod
      val statusCode = client.executeMethod(postMethod)
      if (statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_CREATED || statusCode == HttpStatus.SC_BAD_REQUEST) { //response = postMethod.getResponseBodyAsString();
        //response = postMethod.getResponseBodyAsStream().toString();
        val reader = new BufferedReader(new InputStreamReader(postMethod.getResponseBodyAsStream))
        val stringBuffer = new StringBuffer
        var str = ""
        while ( {
          (str = reader.readLine) != null
        }) stringBuffer.append(str)
        response = stringBuffer.toString
        //System.out.println(response);
      }
      else System.out.println("响应状态码 = " + postMethod.getStatusCode)
    } catch {
      case e: HttpException =>
        System.out.println("发生致命的异常，可能是协议不对或者返回的内容有问题")
        e.printStackTrace()
      case e: IOException =>
        System.out.println("发生网络异常")
        e.printStackTrace()
    } finally if (postMethod != null) {
      postMethod.releaseConnection()
      postMethod = null
    }
    response
  }


  /**
    * POST方式提交数据
    *
    * @param url    待请求的URL
    * @param params 要提交的数据
    * @param enc    编码
    * @return 响应结果
    * @throws IOException IO异常
    */
  def doPostJson(url: String, params: util.Map[_, _], enc: String): String = doPostJson(url, new Gson().toJson(params), enc)
}
