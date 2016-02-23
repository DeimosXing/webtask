package com.almighty.webtask;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScheduleActivity extends AppCompatActivity {
    String originalSchedule;
    List<String> lnlist=new ArrayList<>();
    List<String> dlist=new ArrayList<>();
    List<String> btlist=new ArrayList<>();
    List<String> etlist=new ArrayList<>();
    List<String> crlist=new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        Intent it=getIntent();
        originalSchedule=it.getStringExtra("schedule");
        String scriptText=this.getScriptText(originalSchedule);//解析html
        String finalSchedule=this.getScheduleText(scriptText);//解析script文本
        TextView textView=(TextView)findViewById(R.id.a);
        textView.setText(finalSchedule);
    }
    //使用Jsoup从html文件中解析出课表所在的script文本
    private String getScriptText(String html){
        Document doc = Jsoup.parse(html);
        Elements datas = doc.getElementsByTag("script");
        Element data = datas.get(4);
        return data.toString();
    }

    //使用正则表达式从script文本中解析出课表
    private String getScheduleText(String script) {
        String re="下面是强者的课程表(⊙v⊙)\n============================\n";
        String regexlessonName = "lessonName\\s=\\s\".+";
        String regexDay = "day\\s=\\s\"[0-9]\"";
        String regexbeginTime = "beginTime\\s=\\s\"[0-9]+\"";
        String regexendTime = "endTime\\s=\\s\"[0-9]+\"";
        String regexclassRoom = "classRoom\\s=\\s\".+";
        Pattern patternlessonName = Pattern.compile(regexlessonName);
        Pattern patternDay = Pattern.compile(regexDay);
        Pattern patternbeginTime = Pattern.compile(regexbeginTime);
        Pattern patternendTime = Pattern.compile(regexendTime);
        Pattern patternclassRoom = Pattern.compile(regexclassRoom);
        Matcher mlessonName = patternlessonName.matcher(script);
        Matcher mDay = patternDay.matcher(script);
        Matcher mbeginTime = patternbeginTime.matcher(script);
        Matcher mendTime = patternendTime.matcher(script);
        Matcher mclassRoom = patternclassRoom.matcher(script);
        this.formList(mlessonName,lnlist);
        this.formList(mDay,dlist);
        this.formList(mbeginTime,btlist);
        this.formList(mendTime,etlist);
        this.formList(mclassRoom,crlist);
        re+=this.formScheduleFromList(lnlist,dlist,btlist,etlist,crlist);
        return re;
    }
    private void formList(Matcher m,List l){
        while (m.find()) {
            int i=0;
            l.add(m.group().replaceAll(".+\\s=\\s\"","").replaceAll("\".*", ""));
            i++;
        }
    }

    private String formScheduleFromList(List l,List d,List b,List e,List c){
        String re="";
        for (int i=0;i<l.size();i++){
            String a="";
            a+="课程名:"+l.get(i)+"\n"+"课程时间：每周"+d.get(i)+"\n"+"从第"+b.get(i)+"节至第"+e.get(i)+"节\n"+"教室："+c.get(2*i)+
            "\n===============================\n";
            re+=a;
        }
        return re;
    }




}
