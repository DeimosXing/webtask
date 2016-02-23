package com.almighty.webtask;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginActivity extends AppCompatActivity {
    private ImageView imageView;
    private Bitmap bitmap;
    private ProgressBar roundProBar;
    private Button loginBtn;
    private Button refreshBtn;
    private EditText idEdit;
    private EditText pwdEdit;
    private EditText codeEdit;
    private final int GET_CODE_SUCCESS = 1;
    private final int GET_CODE_ERROR = 2;
    private final int LOGIN_SUCCESS = 3;
    private final int LOGIN_ERROR = 4;
    private final String LOGIN_URL = "http://210.42.121.241/servlet/Login";
    String responseCookie;
    String responseCookie2;
    private String schedule;
    static String userid;
    static String userpassword;

    //信息处理
    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            int tag =  msg.what;

            switch(tag){
                case GET_CODE_SUCCESS:
                    imageView.setImageBitmap( bitmap);
                    break;
                case GET_CODE_ERROR:
                    Toast.makeText(LoginActivity.this, "get code error", Toast.LENGTH_SHORT).show();
                    break;
                case LOGIN_SUCCESS:
                    Toast.makeText(LoginActivity.this, "login success", Toast.LENGTH_SHORT).show();
                    toShowSchedule();
                    break;
                case LOGIN_ERROR:
                    Toast.makeText(LoginActivity.this, "login error", Toast.LENGTH_SHORT).show();
                    break;
            }
            roundProBar.setVisibility(View.GONE);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        CookieManager cookieManager = new CookieManager();//设置cookiemanager
        CookieHandler.setDefault(cookieManager);

        init();//初始化UI
        new GetCodeThread().start();//获取验证码
    }
    //初始化UI
    private void init(){
        imageView = (ImageView) findViewById(R.id.login_image);
        loginBtn = (Button) findViewById(R.id.user_login_btn);
        refreshBtn=(Button) findViewById(R.id.refresh_button);
        idEdit = (EditText) findViewById(R.id.user_id);
        pwdEdit = (EditText) findViewById(R.id.user_pwd);
        codeEdit = (EditText) findViewById(R.id.user_code);
        roundProBar = (ProgressBar) findViewById(R.id.login_progressbar);
        roundProBar.setVisibility(View.GONE);
        getTxtFileInfo(this);//获取保存的密码
        idEdit.setText(userid);
        pwdEdit.setText(userpassword);

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //点击登录时，将获得的用户名、密码、验证码送至login方法
                String id = idEdit.getText().toString().trim();
                String pwd = pwdEdit.getText().toString().trim();
                String code = codeEdit.getText().toString().trim();

                roundProBar.setVisibility(View.VISIBLE);//设置为显示
                login(LOGIN_URL, id, pwd, code);
                saveUserInfo(id, pwd);//保存密码
            }
        });
        //重新获取验证码
        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new GetCodeThread().start();
            }
        });
    }
    //登录
    private void login(final String url,final String id ,final String pwd, final String code){

        new Thread(new Runnable() {
            @Override
            public void run() {
                Message msg = new Message();
                try{
                    URL ul = new URL(url);
                    HttpURLConnection connection = (HttpURLConnection) ul.openConnection();
                    connection.setInstanceFollowRedirects(true);
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.setConnectTimeout(30000);//连接超时限定
                    connection.setReadTimeout(30000);
                    connection.setRequestMethod("POST");
                    //connection.setRequestProperty("Cookie", responseCookie);
                    String content = "id="+id+"&pwd="+pwd+"&xdvfb="+code;//POST的数据
                    OutputStream os = connection.getOutputStream();
                    os.write(content.getBytes());
                    os.flush();//刷新流
                    os.close();
                    BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(),"GBK"));
                    //responseCookie2 = connection.getHeaderField("Set-Cookie");
                    /*打印cookie
                    System.out.println(responseCookie);
                    System.out.println(responseCookie2);
                    */
                    /*打印响应体
                    String line = br.readLine();
                    while (line != null) {
                        System.out.println(line);
                        line = br.readLine();
                    }
                    */
                    URL ul2=new URL("http://210.42.121.241/servlet/Svlt_QueryStuLsn?action=queryStuLsn");//连接课表的URL
                    HttpURLConnection connection2 = (HttpURLConnection) ul2.openConnection();
                    connection2.setRequestMethod("GET");
                    connection2.setDoInput(true);
                    connection2.setDoOutput(true);
                    connection2.setConnectTimeout(30000);
                    connection2.setReadTimeout(30000);
                    //connection2.setRequestProperty("Cookie", responseCookie2);
                    BufferedReader br2 = new BufferedReader(new InputStreamReader(connection2.getInputStream(),"GBK"));
                    String inputLine = "";
                    while((inputLine = br2.readLine()) != null){
                        schedule += inputLine + "\n";
                    }
                    System.out.println(schedule);
                    //验证是否已经登录
                    if (schedule.indexOf("获取当前月份的日期")!=-1) {
                        msg.what = LOGIN_SUCCESS;
                    }else{
                        msg.what = LOGIN_ERROR;
                    }
                }catch(Exception e){
                    msg.what = LOGIN_ERROR;
                    e.printStackTrace();
                }
                handler.sendMessage(msg);
            }
        }).start();
    }
    //获取验证码
    private class GetCodeThread extends Thread {
        @Override
        public void run() {
            Message msg = new Message();
            try {
                /*这段注释掉的代码，是我想通过获取第一个cookie以获得验证码所做，但是后来发现没有这部分也可以成功登录
                String cookie1;
                URL getCookie=new URL(LOGIN_URL);
                HttpURLConnection gcconn = (HttpURLConnection)getCookie.openConnection();
                gcconn.setDoInput(true);
                gcconn.connect();
                //cookie1=gcconn.getHeaderField("Set-Cookie");
                */
                URL imgUrl = new URL("http://210.42.121.241/servlet/GenImg");//验证码图片的URL
                HttpURLConnection conn = (HttpURLConnection)imgUrl.openConnection();
                //conn.setRequestProperty("Cookie",cookie1);
                conn.setDoInput(true);
                conn.connect();
                bitmap = BitmapFactory.decodeStream(conn.getInputStream());
                //responseCookie = conn.getHeaderField("Set-Cookie");
                System.out.println(responseCookie);
                msg.what = GET_CODE_SUCCESS;
            } catch (Exception e) {
                msg.what = GET_CODE_ERROR;
                e.printStackTrace();
            }

            handler.sendMessage(msg);
        }
    }

    //跳转至课程表
    private void toShowSchedule(){
        Intent it = new Intent();
        it.setClass(this, ScheduleActivity.class);
        it.putExtra("schedule",schedule);
        startActivity(it);
    }
    //保存用户名和密码
    public static void saveUserInfo(String username, String password) {
        try {
            // 使用当前项目的绝对路径
            File file = new File("data/data/com.almighty.webtask/webtaskinfo.txt");
            // 创建输出流对象
            FileOutputStream fos = new FileOutputStream(file);
            // 向文件中写入信息
            fos.write((username + "##" + password).getBytes());
            // 关闭输出流对象
            fos.close();
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }
    public static void getTxtFileInfo(Context context) {
        try {
            // 创建FIle对象
            File file = new File(context.getFilesDir(), "webtaskinfo.txt");
            // 创建FileInputStream对象
            FileInputStream fis = new FileInputStream(file);
            // 创建BufferedReader对象
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            // 获取文件中的内容
            String content = br.readLine();
            // 使用保存信息使用的##将内容分割出来
            String[] contents = content.split("##");
            userid=contents[0];
            userpassword=contents[1];
            // 关闭流对象
            fis.close();
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
