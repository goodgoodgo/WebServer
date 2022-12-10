package org.example;

import org.apache.http.client.methods.HttpGet;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.*;

/**
 * @program: HttpClientUtil.java
 * @description:
 * @author: ma
 * @create: 2022-12-08 20:21
 */
public class Test {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        String[] str={"https://www.ximalaya.com/revision/metadata/v2/channel/albums?pageNum=1&pageSize=50&sort=3&metadata=&groupId=11"};
        task(str);
    }
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
                msg=executors
                        .submit(new HttpClientUtil.GetRunnable(urisToGet[i], countDownLatch));
            }
            System.out.println(msg.get());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            System.out.println("线程" + Thread.currentThread().getName() + ","
                    +" 所有线程已完成，开始进入下一步！");
        }
    }
}
