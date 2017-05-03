package com.app.webview;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class Main2Activity extends ActionBarActivity implements View.OnClickListener {
    private static final int PICK_CODE =1;
    private ImageView myPhoto;
    private Button getImage;
    private Button detect;
    private TextView tip;
    private View mWaitting;
    private String ImagePath=null;
    private Paint mypaint;
    private Bitmap myBitmapImage;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        mypaint=new Paint();
        initViews();
        initEvent();
    }
    private void initViews()
    {
        myPhoto=(ImageView)findViewById(R.id.id_photo);
        getImage=(Button)findViewById(R.id.get_image);
        detect=(Button)findViewById(R.id.detect);
        tip=(TextView)findViewById(R.id.id_Tip);
        mWaitting=findViewById(R.id.id_waitting);
        tip.setMovementMethod(ScrollingMovementMethod.getInstance());

    }
    private void initEvent()
    {
        getImage.setOnClickListener(this);
        detect.setOnClickListener(this);
    }
    @Override
    public void onClick(View v) {

        switch (v.getId())
        {
            case R.id.get_image:
                //获取系统选择图片intent
                Intent intent=new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                //开启选择图片功能响应码为PICK_CODE
                startActivityForResult(intent,PICK_CODE);
                break;
            case R.id.detect:
                //显示进度条圆形
                mWaitting.setVisibility(View.VISIBLE);
                //这里需要注意判断用户是否没有选择图片直接点击了detect按钮
                //否则会报一个空指针异常而造成程序崩溃
                if(ImagePath!=null&&!ImagePath.trim().equals(""))
                {
                    //如果不是直接点击的图片则压缩当前选中的图片
                    resizePhoto();
                }else
                {
                    //否则将默认的背景图作为bitmap传入
                    myBitmapImage=BitmapFactory.decodeResource(getResources(),R.drawable.test1);
                }
                //设置回调
                InternetDetect.dectect(myBitmapImage, new InternetDetect.CallBack() {
                    @Override
                    public void success(JSONObject jsonObject) {
                        Message message=Message.obtain();
                        message.what=MSG_SUCESS;
                        message.obj=jsonObject;
                        myhandler.sendMessage(message);
                    }
                    @Override
                    public void error(FaceppParseException exception) {
                        Message message=Message.obtain();
                        message.what=MSG_ERROR;
                        message.obj=exception;
                        myhandler.sendMessage(message);
                    }
                });
                break;
        }

    }
    //设置响应intent请求
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if(requestCode==PICK_CODE)
        {
            if(intent!=null)
            {
                //获取图片路径
                //获取所有图片资源
                Uri uri=intent.getData();
                //设置指针获得一个ContentResolver的实例
                Cursor cursor=getContentResolver().query(uri,null,null,null,null);
                cursor.moveToFirst();
                //返回索引项位置
                int index=cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                //返回索引项路径
                ImagePath=cursor.getString(index);
                cursor.close();
                //这个jar包要求请求的图片大小不得超过3m所以要进行一个压缩图片操作
                resizePhoto();
                myPhoto.setImageBitmap(myBitmapImage);
                tip.setText("Click Detect==>");
            }
        }
    }

    private void resizePhoto() {
        //得到BitmapFactory的操作权
        BitmapFactory.Options options = new BitmapFactory.Options();
        // 如果设置为 true ，不获取图片，不分配内存，但会返回图片的高宽度信息。
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(ImagePath,options);
        //计算宽高要尽可能小于1024
        double ratio=Math.max(options.outWidth*1.0d/1024f,options.outHeight*1.0d/1024f);
        //设置图片缩放的倍数。假如设为 4 ，则宽和高都为原来的 1/4 ，则图是原来的 1/16 。
        options.inSampleSize=(int)Math.ceil(ratio);
        //我们这里并想让他显示图片所以这里要置为false
        options.inJustDecodeBounds=false;
        //利用Options的这些值就可以高效的得到一幅缩略图。
        myBitmapImage=BitmapFactory.decodeFile(ImagePath,options);
    }

    private static final int MSG_SUCESS=11;
    private static final int MSG_ERROR=22;
    private Handler myhandler=new Handler()
    {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what)
            {
                case MSG_SUCESS:
                    //关闭缓冲条
                    mWaitting.setVisibility(View.GONE);
                    //拿到新线程中返回的JSONObject数据
                    JSONObject rsobj= (JSONObject) msg.obj;
                    //准备Bitmap，这里会解析JSONObject传回的数据
                    prepareBitmap(rsobj);
                    //让主线程的相框刷新
                    myPhoto.setImageBitmap(myBitmapImage);
                    break;
                case MSG_ERROR:
                    mWaitting.setVisibility(View.GONE);
                    String errormsg= (String) msg.obj;
                    break;
            }
        }


    };

    private void prepareBitmap(JSONObject JS) {
        //新建一个Bitmap使用它作为Canvas操作的对象
        Bitmap bitmap=Bitmap.createBitmap(myBitmapImage.getWidth(),myBitmapImage.getHeight(),myBitmapImage.getConfig());
        //实例化一块画布
        Canvas canvas=new Canvas(bitmap);
        //把原图先画到画布上面
        canvas.drawBitmap(myBitmapImage, 0, 0, null);
        //解析传回的JSONObject数据
        try {
            //JSONObject中包含着众多JSONArray，但是我们这里需要关键字为face的数组中的信息
            JSONArray faces=JS.getJSONArray("face");
            //获取得到几个人脸
            int faceCount=faces.length();
            //让提示文本显示人脸数
            tip.setText("find"+faceCount);
            //下面对每一张人脸都进行单独的信息绘制
            for(int i=0;i<faceCount;i++)
            {
                //拿到每张人脸的信息
                JSONObject face=faces.getJSONObject(i);
                //拿到人脸的详细位置信息
                JSONObject position=face.getJSONObject("position");
                float x=(float)position.getJSONObject("center").getDouble("x");
                float y=(float)position.getJSONObject("center").getDouble("y");
                float w=(float)position.getDouble("width");
                float h=(float)position.getDouble("height");
                //注意这里拿到的各个参数并不是实际的像素值，而是一个比例，都是相对于整个屏幕而言的比例信息
                //因此我们使用的时候要进行一下数据处理
                x=x/100*bitmap.getWidth();
                y=y/100*bitmap.getHeight();
                w=w/100*bitmap.getWidth();
                h=h/100*bitmap.getHeight();
                //设置画笔颜色
                mypaint.setColor(0xffffffff);
                //设置画笔宽度
                mypaint.setStrokeWidth(3);
                //绘制一个矩形框
                canvas.drawLine(x-w/2,y-h/2,x-w/2,y+h/2,mypaint);
                canvas.drawLine(x-w/2,y-h/2,x+w/2,y-h/2,mypaint);
                canvas.drawLine(x+w/2,y-h/2,x+w/2,y+h/2,mypaint);
                canvas.drawLine(x-w/2,y+h/2,x+w/2,y+h/2,mypaint);
                //得到年龄信息
                int age=face.getJSONObject("attribute").getJSONObject("age").getInt("value");
                //得到性别信息
                String gender=face.getJSONObject("attribute").getJSONObject("gender").getString("value");
                System.out.println("age:"+age);
                System.out.println("gender:"+gender);
                //现在要把得到的文字信息转化为一个图像信息，我们写一个专门的函数来处理
                Bitmap ageBitmap=buildAgeBitmap(age,("Male").equals(gender));
                //进行图片提示气泡的缩放，这个很有必要，当人脸很小的时候我们需要把提示气泡也变小
                int agewidth=ageBitmap.getWidth();
                int agehight=ageBitmap.getHeight();
                if(bitmap.getWidth()<myPhoto.getWidth()&&bitmap.getHeight()<myPhoto.getHeight())
                {
                    //设置缩放比
                    float ratio=Math.max(bitmap.getWidth()*1.0f/
                            myPhoto.getWidth(),bitmap.getHeight()*1.0f/myPhoto.getHeight());

                    //完成缩放
                    ageBitmap=Bitmap.createScaledBitmap(ageBitmap,(int)(agewidth*ratio*0.8),(int)(agehight*ratio*0.5),false);
                }
                //在画布上画出提示气泡
                canvas.drawBitmap(ageBitmap,x-ageBitmap.getWidth()/2,y-h/2-ageBitmap.getHeight(),null);
                //得到新的bitmap
                myBitmapImage=bitmap;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private Bitmap buildAgeBitmap(int age, boolean isMale) {
        //这里要将文字信息转化为图像信息，如果拿Canvas直接画的话操作量太大
        //因此这里有一些技巧，将提示气泡设置成一个TextView，他的背景就是气泡的背景
        //他的内容左侧是显示性别的图片右侧是年龄
        TextView tv= (TextView) mWaitting.findViewById(R.id.id_age_and_gender);
        //这里要记得显示数字的时候后面最好跟一个""不然有时候会显示不出来
        tv.setText(age + "");
        if(isMale)
        {
            //判断性别
            tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.male),null,null,null);

        }else
        {
            tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.female),null,null,null);
        }

        //使用setDrawingCacheEnabled(boolean flag)提高绘图速度
        //View组件显示的内容可以通过cache机制保存为bitmap
        //这里要获取它的cache先要通过setDrawingCacheEnable方法把cache开启，
        // 然后再调用getDrawingCache方法就可 以获得view的cache图片了。
        tv.setDrawingCacheEnabled(true);
        Bitmap bitmap=Bitmap.createBitmap(tv.getDrawingCache());
        //关闭许可
        tv.destroyDrawingCache();
        return bitmap;
    }
}
