package org.example;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.client.methods.HttpGet;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private  volatile static List<String> ids=new ArrayList<>();

    private static List<String> mediaUri =new ArrayList<>();

    private static  JSONArray list = new JSONArray();

    private static String dataJSON="";

    private static  String indexId;

    private static final String INDEX_URI="https://www.ximalaya.com/revision/album?albumId=";

    private static final String PAGE_URI="https://www.ximalaya.com/revision/album/v1/getTracksList?albumId";

    private static final String MEDIA_URI="https://www.ximalaya.com/revision/play/v1/audio?id";

    private static final String MAIN_URI="https://www.ximalaya.com/album/";

    private static  Integer pageSize;

    private static String filePath="/home/Test";

    private volatile static Boolean flag=true;

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        indexId="30787319";
        File fileP = new File(filePath);
        if(!fileP.exists()){
            fileP.mkdir();
        }
        filePath=filePath+"/"+indexId;
        //获取页面信息生成json
        String[] urisMsgToGet = {
                "https://www.ximalaya.com/album/"+indexId};
        task(urisMsgToGet);

        //获取pageSize
        String[] urisToGet = {
                INDEX_URI+indexId};
        task(urisToGet);

        //获取所有音频id,并对每一页信息生成json数据
        String[] trackIdToGet=new String[pageSize];
        for (int i = 0; i < pageSize; i++) {
            trackIdToGet[i]=PAGE_URI+"="+indexId+"&pageNum="+(i+1)+"&sort=0";
        }
        task(trackIdToGet);
        for (int i = 0; i < list.size(); i++) {
            if (i==list.size()-1){
                dataJSON+=list.get(i);
            }else {
                dataJSON+=list.get(i)+",";
            }
        }
        dataJSON+="]"+"}";
        File dir = new File(filePath);
        File data = new File(filePath+"/"+"data");
        File file = new File(filePath+"/"+"data.json");
        File audio = new File(filePath+"/"+"audio");
        if(!dir.exists()){
            dir.mkdir();
            dir.setWritable(true, false);
        }
        if(!data.exists()){
            data.mkdir();
            data.setWritable(true, false);
        }
        if (!file.exists()){
            file.createNewFile();
            file.setWritable(true, false);
        }
        if(!audio.exists()){
            audio.mkdir();
            audio.setWritable(true, false);
        }
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write(dataJSON);
        fileWriter.flush();
        fileWriter.close();
        //获取所有下载链接
        String[] urisToGetMedia=new String[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            urisToGetMedia[i]="https://www.ximalaya.com/revision/play/v1/audio?id="+ids.get(i)+"&ptype=1";
        }
        task(urisToGetMedia);
        //下载
        String[] urisToGetInstall=new String[mediaUri.size()];
        for (int i = 0; i < mediaUri.size(); i++) {
            urisToGetInstall[i]=mediaUri.get(i).substring(1,mediaUri.get(i).length()-1);
        }
        flag=false;
        task(urisToGetInstall);
    }


    /***
     * 处理对应页面数据
     * @param msg
     * @return
     */
    public synchronized static void idsMsg(String msg){
        Pattern pattern = Pattern.compile("\"trackId\"(.*?),");
        Matcher matcher = pattern.matcher(msg);
        JSONObject jsonObject;
        while (matcher.find())
        {
            jsonObject=JSON.parseObject("{"+msg.substring(matcher.start(),matcher.end())+"}");
            String id = JSON.toJSONString(jsonObject.get("trackId"));
            ids.add(id);
        }
        JSONObject json = JSONObject.parseObject(msg);
        JSONObject data = json.getJSONObject("data");
        JSONArray jsonArray = data.getJSONArray("tracks");
        String name="";
        String subscribe="";
        String num="";
        String date="";
        for (int i = 0; i < jsonArray.size(); i++) {
            name="\"name\": "+"\""+jsonArray.getJSONObject(i).get("title")+"\""+",";
            num=""+jsonArray.getJSONObject(i).get("playCount");
            subscribe="\"subscribe\": "+"\""+ String.format("%.1f", Double.parseDouble(num)/10000) +"\""+",";
            date="\"date\": "+"\""+jsonArray.getJSONObject(i).get("createDateFormat")+"\"";
            list.add(JSON.parseObject("{"+name+subscribe+date+"}"));
        }
    }

    /**
     *获取分页数
     * @param msg
     */
    public static void maxPage(String msg){
        Pattern pattern = Pattern.compile("\"pageSize\"(.*?),");
        Matcher matcher = pattern.matcher(msg);
        JSONObject jsonObject;
        matcher.find();
        jsonObject=JSON.parseObject("{"+msg.substring(matcher.start(),matcher.end())+"}");
        pageSize= (Integer) jsonObject.get("pageSize");
    }

    /**
     *获取下载链接
     * @param msg
     */
    public synchronized static void mediaUri(String msg){
        Pattern pattern = Pattern.compile("\"src\"(.*?),");
        Matcher matcher = pattern.matcher(msg);
        JSONObject jsonObject;
        while (matcher.find())
        {
            jsonObject=JSON.parseObject("{"+msg.substring(matcher.start(),matcher.end())+"}");
            String uri = JSON.toJSONString(jsonObject.get("src"));
            mediaUri.add(uri);
        }
    }

    /**
     * 获取音频数据
     * @param msg
     */
    public static void dataJson(String msg){
        Pattern pattern = Pattern.compile("\"channelName\"(.*?),");
        Matcher matcher = pattern.matcher(msg);
        String label = "";
        String title = "";
        String mark="";
        String str = "";
        String subscribe="";
        String shortIntro="";
        while (matcher.find()){
            str=msg.substring(matcher.start(),matcher.end());
            if(str.split("\"")[3].length()>0){
                label+="\""+str.split("\"")[3]+"\",";
            }
        }
        if (label.length()<=1){
            label="\"label\": ["+""+"]";
        }else {
            label="\"label\": ["+label.substring(0,label.length()-2)+"\""+"]";
        }


        title=dataFind("albumTitle",msg,"title");

        pattern = Pattern.compile("\"albumScore\":...,");
        Matcher matcherMark = pattern.matcher(msg);
        matcherMark.find();
        str=msg.substring(matcherMark.start(),matcherMark.end());
        mark="\"mark\" "+str.split("\"")[2];

        pattern = Pattern.compile("([0-9])*.([0-9])*万");
        Matcher matcherSub = pattern.matcher(msg);
        matcherSub.find();
        str=msg.substring(matcherSub.start(),matcherSub.end());
        str=str.substring(0,str.length()-1);
        subscribe="\"subscribe\": "+"\""+str+"\"";

        shortIntro=dataFind("shortIntro",msg,"desc");

        dataJSON="{"+title+","+mark+subscribe+","+label+","+shortIntro+","+"\"list\": [";
    }

    /**
     * 正则提取信息
     * @param str
     * @param msg
     * @param title
     * @return
     */
    private static String dataFind(String str,String msg,String title){
        Pattern pattern = Pattern.compile("\""+Pattern.quote(str)+"\"(.*?),");
        Matcher matcher = pattern.matcher(msg);
        matcher.find();
        str=msg.substring(matcher.start(),matcher.end());
        return "\""+title+"\": \""+str.split("\"")[3]+"\"";
    }

    /**
     * 请求任务
     */
    public static void task(String @NotNull [] urisToGet ) throws ExecutionException, InterruptedException {
        Future<String> msg=null;
        try {
            int pageCount = urisToGet.length;
            ExecutorService executors = Executors.newFixedThreadPool(100);
            CountDownLatch countDownLatch = new CountDownLatch(pageCount);
            for (int i = 0; i < pageCount; i++) {
                HttpGet httpget = new HttpGet(urisToGet[i]);
                HttpClientUtil.config(httpget);
                // 启动线程抓取
               if (flag==true){
                   msg=executors
                           .submit(new HttpClientUtil.GetRunnable(urisToGet[i], countDownLatch));
               }else {
                   msg=executors
                           .submit(new HttpClientUtil.GetRunnable(urisToGet[i], countDownLatch,filePath+"/audio/"+i+".m4a"));
               }
                if(urisToGet[0].length()>=64&& urisToGet[0].startsWith(PAGE_URI)){
                    idsMsg(msg.get());
                }else if(urisToGet[i].length()>50&& urisToGet[i].startsWith(MEDIA_URI)){
                    mediaUri(msg.get());
                }else if(urisToGet[i].startsWith(MAIN_URI)){
                    dataJson(msg.get());
                }
            }
            countDownLatch.await();
            executors.shutdown();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            System.out.println("线程" + Thread.currentThread().getName() + ","
                    +" 所有线程已完成，开始进入下一步！");
        }
        if (urisToGet[0].equals(INDEX_URI+indexId)) {
            maxPage(msg.get());
        }
    }
}



