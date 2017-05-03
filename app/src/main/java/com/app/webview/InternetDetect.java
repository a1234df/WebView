package com.app.webview;

import android.graphics.Bitmap;

import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;


public class InternetDetect {
    public interface CallBack
    {
        void success(JSONObject jsonObject);

        void error(FaceppParseException exception);

    }
    public static void dectect(final Bitmap bitmap, final CallBack callBack)
    {
        //因为这里要向网络发送数据是耗时操作所以要在新线程中执行
        new Thread(new Runnable() {
            @Override
            public void run() {
                /*1.设置请求
                * 2.创建一个Bitmap
                * 3.创建字符数组流
                * 4.将bitmap转换为字符并传入流中
                * 5.新建字符数组接受流
                * 6.创建发送数据包
                * 7.创建接受数据包
                *
                * */
                try {

                    HttpRequests httpRequests=new HttpRequests(Constant.Key,Constant.Secret,true,true);
                    //从0，0点挖取整个视图，后两个参数是目标大小
                    Bitmap bitmapsmall = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight());
                    //这里api要求传入一个字节数组数据，因此要用字节数组输出流
                    ByteArrayOutputStream stream=new ByteArrayOutputStream();
                    /*Bitmap.compress()方法可以用于将Bitmap-->byte[]
                      既将位图的压缩到指定的OutputStream。如果返回true，
                      位图可以通过传递一个相应的InputStream BitmapFactory.decodeStream（重建）
                      第一个参数可设置JPEG或PNG格式,第二个参数是图片质量，第三个参数是一个流信息*/
                    bitmapsmall.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    byte[] arrays=stream.toByteArray();
                    //实现发送参数功能
                    PostParameters parameters=new PostParameters();
                    //发送数据
                    parameters.setImg(arrays);
                    //服务器返回一个JSONObject的数据
                    JSONObject jsonObject=httpRequests.detectionDetect(parameters);
                    System.out.println("jsonObject:"+jsonObject.toString());
                    if(callBack!=null)
                    {
                        //设置回调
                        callBack.success(jsonObject);
                    }
                } catch (FaceppParseException e) {

                    System.out.println("error");
                    e.printStackTrace();
                    if(callBack!=null)
                    {
                        callBack.error(e);
                    }
                }


            }
        }).start();
    }
}
