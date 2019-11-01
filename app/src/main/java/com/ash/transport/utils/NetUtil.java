package com.ash.transport.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

/*----------------------------------------------*
 * @package:   com.ash.transport.utils
 * @fileName:  NetUtil.java
 * @describe:  网络访问工具类
 *----------------------------------------------*
 * @author:    ash
 * @email:     Glaxyinfinite@outlook.com
 * @date:      on 2019-05-22 14:39
 *----------------------------------------------*/
public class NetUtil {
    private static final int READ_TIME = 1000 * 3;      // 读取超时限制 3秒
    private static final int CONNECT_TIME = 1000 * 3;   // 连接超时限制 3秒

    // [非主线程](子线程)中进行UI相关操作
    private Handler handler = new Handler(Looper.getMainLooper());

    // 定义 [响应事件监听器] 公开接口
    // 整个网络请求都通过该监听器驱动
    public interface ResponseListener {

        void success(String result);    // 定义 成功事件 空方法

        void error(String message);     // 定义 错误事件 空方法
    }

    // 定义 判断网络连接是否可用 公开静态方法
    public static boolean isNetworkOK(Context context) {
        boolean isNetworkOK = false;
        try {
            // 需申请检查网络状态权限 android.permission.ACCESS_NETWORK_STATE
            ConnectivityManager conn = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);

            isNetworkOK = !(conn == null || conn.getActiveNetworkInfo() == null);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return isNetworkOK;
    }

    // 定义 初始化请求URL 方法
    private HttpURLConnection initURL(String urlString) throws IOException {
        URL url = new URL(urlString);
        return (HttpURLConnection) url.openConnection();
    }

    // 定义 设置URL请求头参数 方法
    private void setURLParams(HttpURLConnection conn) throws ProtocolException {
        // 涉及http协议知识点
        conn.setRequestProperty("accept", "*/*");               // 同意所有文件类型
        conn.setRequestProperty("connection", "Keep-Alive");    // 设置连接方式
        conn.setRequestMethod("POST");                          // 设置以post形式发送
        conn.setRequestProperty("Content-Type", "text/html; charset=UTF-8");    // 设置发送格式
        conn.setConnectTimeout(CONNECT_TIME);                   // 设置连接超时时间
        conn.setReadTimeout(READ_TIME);                         // 设置读取超时时间
        conn.setDoOutput(true);                                 // 打开输出流
        conn.setDoInput(true);                                  // 打开输入流
    }

    // 定义 发送数据 方法
    private void sendData(HttpURLConnection conn, final String params) throws IOException {
        OutputStream os = null;         // 定义输出流
        OutputStreamWriter osw = null;  // 定义输出流写入器

        // 利用输出流，向服务器输出流体数据
        try {
            // 需申请网络连接权限 android.permission.INTERNET
            os = conn.getOutputStream(); // 通过conn获取该连接的输出流
            osw = new OutputStreamWriter(os, "UTF-8"); // 通过输出流生成写入器
            osw.write(params);           // 向写入器中写入欲请求的参数数据
            osw.flush();                 // 把写入器内的流体数据冲往服务器
        } finally {
            // finally 关键字内的代码将在函数返回之前[一定执行]
            // 使用完毕要及时关闭输出流
            if (os != null) os.close();
            if (osw != null) osw.close();
        }
    }

    // 定义 接收数据 方法
    private String receiveData(HttpURLConnection conn) throws IOException {
        InputStream is = null;           // 定义输入流
        InputStreamReader isr = null;    // 定义输入流读取器
        BufferedReader bufReader = null; // 定义缓冲区读取器
        String result = "";              // 定义返回结果

        // 利用输入流，把服务器返回的流体数据输入给应用程序
        try {
            is = conn.getInputStream();             // 通过conn获取该连接的输入流
            isr = new InputStreamReader(is);        // 通过输入流生成读取器
            bufReader = new BufferedReader(isr);    // 定义与读取器一样大的缓冲区

            // 按行循环读取 储存至result
            String line;
            while ((line = bufReader.readLine()) != null) {
                if (result.equals("")) {
                    result += line;
                } else {
                    result += "\n" + line;
                }
            }

        } finally {
            // finally 关键字内的代码将在函数返回之前[一定执行]
            // 使用完毕要及时关闭输入流
            if (is != null) is.close();
            if (isr != null) isr.close();
            if (bufReader != null) bufReader.close();
        }

        return result;
    }

    // 定义 post请求服务器 公开方法
    public String postData(String urlString, String params) throws IOException, JSONException {
        String result;                      // 定义结果字符串
        HttpURLConnection conn = initURL(urlString); // 调用本类方法 初始化URL
        setURLParams(conn);                 // 调用本类方法 设置URL请求头参数
        sendData(conn, params);             // 调用本类方法 发送数据
        String temp = receiveData(conn);    // 调用本类方法 接收服务器返回数据

        /*  此时接收到的 temp 字符串的内容格式为json字符串格式,大体如下：
         *--------------------------------------------------------
         *  真实样貌：
         *
         *  {serverinfo:{'money':30}}
         *--------------------------------------------------------
         *  展开一览：
         *
         *  {
         *      serverinfo:
         *      {
         *          'money':30
         *      }
         *  }
         *---------------------------------------------------------
         *  服务器返回的每条数据都包裹在 serverinfo 键内
         *  应提前剔除 serverinfo 键，取出其中的数据方便后续使用
         *---------------------------------------------------------
         */

        // 取出 serverinfo 键内数据作为结果字符串
        result = new JSONObject(temp).getString("serverinfo");

        // result 此时内容为 {'money':30}
        return result;
    }


    // 定义 监听线程 方法
    private void listeningThread(final String urlString,
                                 final String params,
                                 final ResponseListener listener) {
        // 在 Android 中，与网络请求相关的功能必须在[非UI线程](子线程)中执行
        // 创建子线程 请求服务器并使用本类[响应事件监听器接口]接收返回数据
        new Thread() {
            @Override
            public void run() {
                // 监听器不为空的前提下
                if (listener != null) {
                    String result = "";

                    try {

                        // 调用本类方法 请求服务器并取回结果
                        result = postData(urlString, params);

                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // 向 响应事件监听器接口 成功事件 传递服务器结果
                    listener.success(result);
                }
            }
        }.start();
    }

    // 定义 异步请求 公开方法
    public void asyncRequest(final String urlString,
                             final String params,
                             final ResponseListener listener) {
        // 调用本类方法 正式启动线程开始请求并进行监听
        // 使用匿名内部类实现[响应事件监听器接口]事件
        listeningThread(urlString, params, new ResponseListener() {

            // 重写实现请求成功事件
            @Override
            public void success(final String result) {
                // 启动 handler
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        // 使用定义好的[响应事件监听器]回调成功事件
                        listener.success(result);
                    }
                });
            }

            // 重写实现请求错误事件
            @Override
            public void error(final String message) {
                // 启动 handler
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        // 使用定义好的[响应事件监听器]回调失败事件
                        listener.error(message);
                    }
                });
            }
        });
    }
}
