package com.blood.bilibili;

/*
 *  @项目名：  AudioVideoDemo
 *  @包名：    com.blood.bilibili
 *  @文件名:   ThreadPoolUtil
 *  @创建者:   bloodsoul
 *  @创建时间:  2021/1/16 18:55
 *  @描述：    TODO
 */

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPoolUtil {

    private final ExecutorService mExecutorService;

    private static ThreadPoolUtil sThreadPoolUtil;

    public static ThreadPoolUtil getInstance() {
        if (sThreadPoolUtil == null) {
            synchronized (ThreadPoolUtil.class) {
                if (sThreadPoolUtil == null) {
                    sThreadPoolUtil = new ThreadPoolUtil();
                }
            }
        }
        return sThreadPoolUtil;
    }

    private ThreadPoolUtil() {
        mExecutorService = Executors.newCachedThreadPool();
    }

    public void start(Runnable runnable) {
        mExecutorService.execute(runnable);
    }

}
