package com.fishing;

import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author viper
 * @create 2024-07-26-0:22
 */
@Slf4j
public class FishIsSuccessTask implements Callable<Integer> {
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

    @Override
    public Integer call() {
        // 创建一个Mat对象来存储匹配结果
        Mat imread = Imgcodecs.imread(filapath);
        Mat result = new Mat();
        for (Mat mat : mats) {
            SingleTask singleTask = new SingleTask(imread, mat, result, Imgproc.TM_SQDIFF_NORMED);
            tasks.add(singleTask);
        }
        tasks.forEach(task -> {
            try {
                Integer taskRes = executor.submit(task).get();
                successNum.addAndGet(taskRes);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        });
        log.info("FishIsSuccessTask.call() successNum.get():{}", successNum.get());
        return successNum.get();
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
        public Integer call() {
            Imgproc.matchTemplate(imread, mat, result, Imgproc.TM_SQDIFF_NORMED);
            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
            double minVal = mmr.minVal;
            log.info("FishIsSuccessTask filepath:{}, minVal:{}", filapath, minVal);
            if (minVal <= 0.001689) {
                log.info("FishIsSuccessTask match!filepath:{}, minVal:{}", filapath, minVal);
                successNum.incrementAndGet();
            }
            log.info("FishIsSuccessTask.call() successNum:{}", successNum);
            return successNum.get();
        }
    }
}
