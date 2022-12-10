package org.example;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.client.methods.HttpGet;
import org.jetbrains.annotations.NotNull;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @program: HttpClientUtil.java
 * @description:
 * @author: ma
 * @create: 2022-12-08 14:42
 */
public class MainTwo {
    private static Map<String, String> channelMap = new HashMap<>();
    private static Map<String, String> channelSonMap = new HashMap<>();
    private static Map<Integer, List<String>> bookMap = new HashMap<>();
    private static List<Integer> count= Arrays.asList(11,6,12,8,18,15,14,5,5,17,18,19,19,13,11,14,6,7,8,9,12,10,18,8,7,12);
    private static List<String> channel = new ArrayList<>();
    private static List<String> channelSonId = new ArrayList<>();
    private static Integer isFinish = 0;
    private static Integer isVip = 0;
    private static Integer pageSize = 50;
    private static Integer channelCount = 0;
    private static String dataJson = "";
    private static String totalJson="";
    private static Integer finish = 0;
    private static Integer vip = 0;

    private static String json = "";

    private static Integer idCount=7;

    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {

        for (int idG = 7; idG <=32; idG++) {


            channelSonId.clear();
            channel.clear();
            channelSonMap.clear();
            channelMap.clear();
            isFinish = 0;
            isVip = 0;
            finish = 0;

            String groupId = String.valueOf(idG);

            String finishRate = "";

            String vipRate = "";


            String[] sortUri=new String[26];
            for (int i = 7; i <= 32; i++) {
                sortUri[i-7]="https://www.ximalaya.com/revision/metadata/v2/channel/albums?pageNum=1&pageSize=50&sort=3&metadata=&groupId="+i;
            }
            task(sortUri);

            String[] urisMsgToGet = {
                    "https://www.ximalaya.com/channel/"};
            task(urisMsgToGet);
            if (groupId.equals("23")) {
                pageSize = 29;
            } else {
                pageSize = 50;
            }
            String[] uriProgram = new String[pageSize];
            for (int i = 0; i < pageSize; i++) {
                uriProgram[i] = "https://www.ximalaya.com/revision/metadata/v2/channel/albums?pageNum=" + (i + 1) + "&pageSize=" + pageSize + "&sort=1&metadata=&groupId=" + groupId;
            }
            task(uriProgram);
            vipRate = String.format("%.1f", isVip * 1.0 / (50 * pageSize) * 100) + "%";
            finishRate = String.format("%.1f", isFinish * 1.0 / (50 * pageSize) * 100) + "%";


            String[] uriChannelGroup = {
                    "https://www.ximalaya.com/revision/metadata/v2/group/channels?groupId=" + groupId
            };
            task(uriChannelGroup);
            dataJson = "{" + "\"name\": " + "\"" + channelMap.get(groupId) + "\","
                    + "\"vip" + "\": " + "\"" + vipRate + "\","
                    + "\"over" + "\": " + "\"" + finishRate + "\","
                    + "\"channelCount" + "\": " + "\"" + channelCount + "\","
                    + "\"channelList\": [";
            String[] channelSon = new String[pageSize];
            //每个子频道50页
            for (int i = 0; i < channelCount; i++) {
                for (int j = 0; j < pageSize; j++) {
                    channelSon[j] = ("https://www.ximalaya.com/revision/metadata/v2/channel/albums?pageNum="
                            + (1 + j) + "&pageSize=50&sort=1&metadataValueId=" + channelSonId.get(i) + "&metadata=%7B%7D");
                }
                vip=0;
                finish=0;
                task(channelSon);
                vipRate = String.format("%.1f", vip * 1.0 / (50 * pageSize) * 100) + "%";
                finishRate = String.format("%.1f", finish * 1.0 / (50 * pageSize) * 100) + "%";
                json = "{\"name\": " + "\"" + channelSonMap.get(channelSonId.get(i)) + "\","
                        + "\"vip" + "\": " + "\"" + vipRate + "\","
                        + "\"over" + "\": " + "\"" + finishRate + "\"}";
                dataJson += json + ",";
            }
            dataJson=dataJson.substring(0,dataJson.length()-1);
            dataJson+="]}";
            /*dataJson=dataJson.substring(0,dataJson.length()-1);
            dataJson += "]}";
            creatJsonFile(dataJson, groupId);
            totalJson="[";
            for (int i = 7; i <= 32; i++) {
                totalJson+="{\""+"name"+"\": \""+channelMap.get(String.valueOf(i))+"\""+","+
                        "\"id"+"\": "+i+","+
                        "\"channelCount"+"\": "+count.get(i-7)+","+
                        "\"firstBook"+"\": \""+bookMap.get(i).get(0)+"\""+","+
                        "\"secondBook"+"\": \""+bookMap.get(i).get(1)+"\""+","+
                        "\"thirdBook"+"\": \""+bookMap.get(i).get(2)+"\""+"},";
            }
            totalJson=totalJson.substring(0,totalJson.length()-1);
            totalJson+="]";*/
            creatJsonFile(dataJson,idG+"");
        }
    }


    /**
     *
     * @param msg
     */
    private static void getMaxCountBookName(String msg) {
        String firstBook;
        String secondBook;
        String thirdBook;
        JSONObject data = JSONObject.parseObject(msg).getJSONObject("data");
        JSONArray jsonArray = data.getJSONArray("albums");
        firstBook=jsonArray.getJSONObject(0).get("albumTitle")+"";
        secondBook=jsonArray.getJSONObject(1).get("albumTitle")+"";
        thirdBook=jsonArray.getJSONObject(2).get("albumTitle")+"";
        List<String> values = Arrays.asList(firstBook,secondBook,thirdBook);
        bookMap.put(idCount, values);
        idCount++;
    }


    /**
     * 获取子频道数和信息
     *
     * @param msg
     */
    private static void getChannelList(String msg) {
        JSONObject data = JSONObject.parseObject(msg).getJSONObject("data");
        JSONArray jsonArray = data.getJSONArray("channels");
        String relationMetadataValueId = "";
        String name = "";
        channelCount = jsonArray.size();
        for (int i = 0; i < jsonArray.size(); i++) {
            relationMetadataValueId = jsonArray.getJSONObject(i).get("relationMetadataValueId") + "";
            name = jsonArray.getJSONObject(i).get("channelName") + "";
            channelSonId.add(relationMetadataValueId);
            channelSonMap.put(relationMetadataValueId, name);
        }
    }


    /**
     * 获取所有频道和id
     *
     * @param msg
     */
    private static void channelAll(String msg) {
        String str = "";
        String id = "";
        Pattern pattern = Pattern.compile("<a class=\"channel q_X\" href=\"/channel/(.*?)/\">(.*?)<");
        Matcher matcher = pattern.matcher(msg);
        Matcher matcherStr;
        Matcher matcherId;
        while (matcher.find()) {
            str = msg.substring(matcher.start(), matcher.end());
            pattern = Pattern.compile("channel/(.*?)/");

            matcherId = pattern.matcher(str);
            matcherId.find();
            id = str.substring(matcherId.start(), matcherId.end());
            id = id.substring(8, id.length() - 1);
            pattern = Pattern.compile(">(.*?)<");
            matcherStr = pattern.matcher(str);
            matcherStr.find();
            str = str.substring(matcherStr.start(), matcherStr.end());
            channelMap.put(id, str.substring(1, str.length() - 1));
            channel.add(str.substring(1, str.length() - 1));
        }
    }


    /**
     * 获取频道的vip占比和完本率
     *
     * @param msg
     */
    private static void vipAndFinish(String msg) {
        JSONObject data = JSONObject.parseObject(msg).getJSONObject("data");
        JSONArray jsonArray = data.getJSONArray("albums");
        for (int i = 0; i < jsonArray.size(); i++) {
            if ((jsonArray.getJSONObject(i).get("isFinished") + "").equals("2")) {
                isFinish++;
            }
            if ((jsonArray.getJSONObject(i).get("vipType") + "").equals("2")) {
                isVip++;
            }
        }
    }

    private static void vipAndFinishSon(String msg, String id) {
        String finishRate = "";
        String vipRate = "";
        String json = "";
        JSONObject data = JSONObject.parseObject(msg).getJSONObject("data");
        JSONArray jsonArray = data.getJSONArray("albums");
        for (int i = 0; i < jsonArray.size(); i++) {
            if ((jsonArray.getJSONObject(i).get("isFinished") + "").equals("2")) {
                finish++;
            }
            if ((jsonArray.getJSONObject(i).get("vipType") + "").equals("2")) {
                vip++;
            }
        }
    }

    private static void creatJsonFile(String json, String id) throws IOException {
        File file = new File("src/main/resources/" + id + ".json");
        if (!file.exists()) {
            file.createNewFile();
        }
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write(json);
        fileWriter.flush();
        fileWriter.close();
    }

    public static synchronized void task(String @NotNull [] urisToGet) throws ExecutionException, InterruptedException {
        Future<String> msg = null;
        try {
            int pageCount = urisToGet.length;
            ExecutorService executors = Executors.newFixedThreadPool(100);
            CountDownLatch countDownLatch = new CountDownLatch(pageCount);
            for (int i = 0; i < pageCount; i++) {
                HttpGet httpget = new HttpGet(urisToGet[i]);
                HttpClientUtil.config(httpget);
                // 启动线程抓取
                msg = executors
                        .submit(new HttpClientUtil.GetRunnable(urisToGet[i], countDownLatch));
                if (urisToGet[0].length() >= 64 && urisToGet[0].startsWith("https://www.ximalaya.com/revision/metadata/v2/channel/albums?pageNum=")
                        && !urisToGet[i].contains("metadataValueId")&&!urisToGet[i].contains("sort=3")) {
                    vipAndFinish(msg.get());
                } else if (urisToGet[0].length() >= 64 && urisToGet[0].startsWith("https://www.ximalaya.com/revision/metadata/v2/group/channels?groupId=")) {
                    getChannelList(msg.get());
                } else if (urisToGet[i].contains("metadataValueId")) {
                    vipAndFinishSon(msg.get(), urisToGet[i].split("&")[3].substring(16));
                }else if (urisToGet[i].contains("sort=3")){
                    getMaxCountBookName(msg.get());
                }
            }
            countDownLatch.await();
            executors.shutdown();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            System.out.println("线程" + Thread.currentThread().getName() + ","
                    + " 所有线程已完成，开始进入下一步！");
        }
        channelAll(msg.get());
    }
}
