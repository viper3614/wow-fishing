package com.fishing;

import java.util.concurrent.ThreadFactory;

/**
 * @author viper
 * @create 2024-07-26-7:49
 */
public class FinshThreadFactory implements ThreadFactory {
    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, "Finsh Thread");
        t.setDaemon(true);
        return t;
    }
}
