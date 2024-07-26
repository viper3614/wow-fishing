package com.fishing;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author viper
 * @create 2024-07-26-0:22
 */
public class FishIsSuccessTask implements Callable<Boolean> {
    ThreadPoolExecutor executor = new ThreadPoolExecutor(6, 6, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(20));
    List<Callable<Integer>> tasks = new LinkedList<Callable<Integer>>();
    private Mat sourceImage;
    private Mat templateImage;
    private int count;
    private String filapath;
    private LinkedList<Mat> mats;
    private Integer times = 0;
    private int index;
    private AtomicInteger successNum = new AtomicInteger(0);


    public FishIsSuccessTask(String filapath, int count, LinkedList<Mat> mats) {
        this.count = count;
        this.filapath = filapath;
        this.mats = mats;
    }

    public FishIsSuccessTask(String filapath, int loopCount, LinkedList<Mat> mats, int times, int index, AtomicInteger successNum) {
        this.filapath = filapath;
        this.count = loopCount;
        this.mats = mats;
        this.times = times;
        this.index = index;
        this.successNum = successNum;
    }

    @Override
    public Boolean call() throws Exception {
        // 创建一个Mat对象来存储匹配结果
        File file = new File(filapath);
        File[] files = file.listFiles();
        if (files == null || files.length == 0) {
            return false;
        }
        int length = files.length;

        for (int i = 0; i < length; i++) {
            Mat imread = Imgcodecs.imread(files[i].getAbsolutePath());
            Mat result = new Mat();
            for (Mat mat : mats) {
                SingleTask singleTask = new SingleTask(imread, mat, result, Imgproc.TM_SQDIFF_NORMED);
                tasks.add(singleTask);
            }
        }
        tasks.stream().forEach(task -> {
            Future<Integer> submit = executor.submit(task);
            try {
                submit.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        System.out.printf("successNum.get():", successNum.get());
        if (successNum.get() >= 8) {
            return true;
        }
        return false;
    }

    class SingleTask implements Callable<Integer> {
        Mat imread;
        Mat mat;
        Mat result;
        int type;

        public SingleTask(Mat imread, Mat mat, Mat result, int type) {
            this.imread = imread;
            this.mat = mat;
            this.result = result;
            this.type = type;
        }

        @Override
        public Integer call() throws Exception {
            Imgproc.matchTemplate(imread, mat, result, Imgproc.TM_SQDIFF_NORMED);
            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
            double minVal = mmr.minVal;
            if (minVal <= 0.01110) {
                successNum.incrementAndGet();
            }
            System.out.println("times:" + times + ",successNum:" + successNum);
            return successNum.get();
        }
    }
}
