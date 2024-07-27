package com.fishing;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

/**
 * @author viper
 * @create 2024-07-25-12:01
 */
public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    public static ExecutorService executor = new ThreadPoolExecutor(4, 4, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10), new FinshThreadFactory());
    public static ExecutorService cutJudgeImg = new ThreadPoolExecutor(3, 3, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10), new FinshThreadFactory());
    /**
     * 鱼漂位置坐标x,y
     */
    public static Point floatCoordinate = null;
    /**
     * 样本图片：1.鱼漂图片 2.钓上鱼的鱼漂图片
     */
    public static LinkedList<Mat> mats = new LinkedList<>();
    /**
     * 钓鱼等级和钓鱼次数映射map
     */
    public static Map<Integer, Integer> levelMap = new LinkedHashMap<>();
    /**
     * 程序开始执行事件
     */
    private static long start = System.currentTimeMillis();
    /**
     * 截图保存路径
     */
    public static final String filePath = "E:\\wowTempImg\\";
    /**
     * 找鱼漂位置循环次数，超过N次没找到就开始新的甩竿
     */
    public static int findFloatCount = 5;
    /**
     * 总甩竿次数
     */
    public static int loopCount = 100;
    /**
     * 10分钟Millis
     */
    public static long tenMinutes = 600000l;
    /**
     * 设置程序执行时长，单位：小时
     */
    public static long executeHours = 2;
    /**
     * 鱼漂图样
     */
    public static Mat cutImtemplateImg;
    /**
     * 否钓到鱼判断方式：1通过图片对比 2通过监听麦克风声音数据
     */
    public static JudgeType judgeType = JudgeType.BySound;
    /**
     * 是否需要每隔十分钟使用提升钓鱼技能的物品
     */
    public static boolean needFishLevelUp = true;

    static {
        // 调用OpenCV库文件
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        Mat cutImtemplateImg1 = Imgcodecs.imread("D:\\Develop-Application\\Idea-projects\\wow-fishing\\src\\main\\resources\\floatsuccess1.png");
        Mat cutImtemplateImg2 = Imgcodecs.imread("D:\\Develop-Application\\Idea-projects\\wow-fishing\\src\\main\\resources\\floatsuccess2.png");
        Mat cutImtemplateImg3 = Imgcodecs.imread("D:\\Develop-Application\\Idea-projects\\wow-fishing\\src\\main\\resources\\floatsuccess3.png");
        cutImtemplateImg = Imgcodecs.imread("D:\\Develop-Application\\Idea-projects\\wow-fishing\\src\\main\\resources\\floatstart3.png");
        cutImtemplateImg1 = Imgcodecs.imread("D:\\Develop-Application\\Idea-projects\\wow-fishing\\src\\main\\resources\\floatstart2.png");
        mats.add(cutImtemplateImg3);
        levelMap.put(1, 1);
        levelMap.put(115, 2);
        levelMap.put(135, 3);
        levelMap.put(160, 4);
        levelMap.put(190, 5);
        levelMap.put(215, 6);
        levelMap.put(295, 9);
        levelMap.put(315, 10);
        levelMap.put(354, 11);
        levelMap.put(425, 12);
    }

    public static void init(Robot robot) throws Exception {
        long expectTime = executeHours * 60 * 60 * 1000;
        loopCount = Math.round((float) expectTime / 9000);
        log.info("init() loopCount:{} ", loopCount);

        if (needFishLevelUp) {
            Thread.sleep(1000);
            keyClick(robot, KeyEvent.VK_Q);
            Thread.sleep(3000);
        }
        FishTask fishTask = new FishTask(filePath);
        executor.submit(fishTask);
    }

    public static void main(String[] args) throws Exception {
        Robot robot = new Robot();
        init(robot);
        exec(robot);
    }

    private static void exec(Robot robot) throws Exception {
        for (; loopCount >= 0; loopCount--) {
            log.info("======================开始第{}次======================", loopCount);
            mouseInitCoordinate(robot);
            //甩干
            mouseClick(robot, InputEvent.BUTTON1_DOWN_MASK);
            //甩完竿需要等待2秒鱼漂画面稳定
            Thread.sleep(2000);
            castingRodFile(loopCount);
            //截图和对比float.png，将鼠标移动到鱼漂位置
            boolean b = goToFloat(robot, loopCount, findFloatCount);
            if (!b) {
                continue;
            }
            log.info("judge fishing");
            judgeFishing(loopCount);
            mouseClick(robot, InputEvent.BUTTON3_DOWN_MASK);
            calcTime(robot);
            Thread.sleep(new Random().nextInt(3000) + 1000);
            log.info("======================结束第{}次======================", loopCount);
        }
    }

    /**
     * 判断是否钓到鱼 判断方式[judgeType]：1.图片对比 2.声音对比
     */
    private static boolean judgeFishing(int loopCount) throws Exception {
        boolean result = false;
        if (JudgeType.ByImg.equals(judgeType)) {
            result = judgeFishingFromImg(loopCount);
        } else if (JudgeType.BySound.equals(judgeType)) {
            result = judgeFishingFromSound();
        }
        log.info("judge fishing {} result:{}", judgeType.getName(), result);
        return result;
    }


    /**
     * 每十分钟按一次q，清理截图文件目录
     */
    public static void calcTime(Robot robot) {
        executor.submit(() -> {
            long currented = System.currentTimeMillis();
            long spend = currented - start;
            if (spend > tenMinutes) {
                try {
                    Thread.sleep(1000);
                    keyClick(robot, KeyEvent.VK_Q);
                    Thread.sleep(2000);
                    FishTask fishTask = new FishTask(filePath);
                    executor.submit(fishTask);
                    start = currented;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void keyClick(Robot robot, int keyCode) {
        log.info("press key -> Q:{}", keyCode);
        robot.keyPress(keyCode);
        robot.delay(100);
        robot.keyRelease(keyCode);
    }

    /**
     * 甩竿后截图
     *
     * @param loopCount
     */
    private static void castingRodFile(int loopCount) {
        String name = FishType.START.getName() + loopCount + "fishing";
        String filename = filePath + loopCount + "\\" + name + System.currentTimeMillis() + ".png";
        captureScreenRegion(filename, FishType.START);
    }

    /**
     * 判断是否上钩
     */
    private static boolean judgeFishingFromImg(int loopCount) throws Exception {
        String type = "judge";
        int count = 140;
        int successCount = 0;
        // 截图，循环和float.png做图像对比进行判断
        while (count >= 0) {
            String path = filePath + loopCount + "\\";
            String filename = filePath + loopCount + "\\" + type + count + ".png";
            captureScreenRegion(filename, FishType.JUDGE);
            Thread.sleep(40);
            Future<Integer> future = executor.submit(new FishIsSuccessTask(filename, count, mats));
            count--;
            Integer result = future.get();
            successCount += result;
            log.info("isRiseToTheBait() successCount:{}", successCount);
            if (successCount >= 25) {
                return true;
            }
        }
        return false;
    }

    /**
     * 通过系统声音判断是否上钩
     */
    private static boolean judgeFishingFromSound() throws Exception {
        SoundDeviceEngineeCore engineeCore = new SoundDeviceEngineeCore();
        return engineeCore.startRecognize();
    }

    private static boolean goToFloat(Robot robot, int loopCount, int count) throws Exception {
        if (count <= 0) {
            return false;
        }
        floatCoordinate = compareWithFloat(loopCount, FishType.START);
        count--;
        if (floatCoordinate == null) {
            // 没找到鱼漂，可能甩竿的图片系统还没有保存好
            Thread.sleep(100);
            log.info("没有找到鱼漂位置,重新定位:{}", (findFloatCount++));
            goToFloat(robot, loopCount, count);
        }
        findFloatCount = 1;
        //移动鼠标位置
        return moveMouse(robot, floatCoordinate);
    }

    private static void mouseClick(Robot robot, int type) {
        robot.mousePress(type);
        robot.delay(100);
        robot.mouseRelease(type);
    }

    /**
     * 设置鼠标初始坐标位置
     *
     * @param robot
     */
    public static void mouseInitCoordinate(Robot robot) {
        robot.mouseMove(1820, 490);
    }

    /**
     * 移动鼠标位置
     */
    private static boolean moveMouse(Robot robot, Point floatCoordinate) {
        if (floatCoordinate == null) {
            return false;
        }
        int x = floatCoordinate.x;
        int y = floatCoordinate.y;
        log.info("x:{},y:{}", x, y);
        robot.mouseMove(x, y);
        return true;
    }

    /**
     * 通过OpenCV识别鱼漂位置
     */
    private static Point compareWithFloat(int loopCount, FishType type) {
        String path = filePath + loopCount + "\\";
        File file = new File(path);
        File[] files = file.listFiles();
        if (files == null) {
            return null;
        }
        for (File f : files) {
            String absolutePath = f.getAbsolutePath();
            if (!absolutePath.contains(FishType.START.getName())) {
                continue;
            }
            Mat cutImg = Imgcodecs.imread(absolutePath);
            Point matchedImg;
            matchedImg = detectObject(cutImg, cutImtemplateImg, type);
            if (matchedImg == null) {
                break;
            } else {
                return matchedImg;
            }
        }
        return null;
    }

    // 物体识别函数，使用模板匹配
    public static Point detectObject(Mat sourceImage, Mat templateImage, FishType type) {
        Core.MinMaxLocResult mmr = matchImg(sourceImage, templateImage);
        org.opencv.core.Point minLoc;
        double minVal, maxVal;
        Point maxLoc;
        minVal = mmr.minVal;
        maxVal = mmr.maxVal;
        minLoc = mmr.minLoc; // 最小值是最佳匹配位置
        // 如果匹配值足够小（即匹配度高），则认为找到了物体,注意：这里的阈值0.1是一个示例值，需要根据实际情况调整
        log.info("detectObject() type:{}, minVal:{}", type.getName(), minVal);
        if (minVal < 0.05689) {
            // 返回匹配到的物体的左上角点,如果需要中心点，可以计算得到：(minLoc.x + templateImage.width() / 2, minLoc.y + templateImage.height() / 2)
            maxLoc = new Point((int) (minLoc.x + templateImage.width() / 2), (int) (minLoc.y + templateImage.height() / 2));
            return maxLoc;
        }
        return null;
    }

    /**
     * 图像匹配
     */
    public static Core.MinMaxLocResult matchImg(Mat sourceImage, Mat templateImage) {
        // 创建一个Mat对象来存储匹配结果
        Mat result = new Mat();
        // 执行模板匹配,这里使用SQDIFF_NORMED，它会返回归一化的平方差，值越小表示匹配度越高
        Imgproc.matchTemplate(sourceImage, templateImage, result, Imgproc.TM_SQDIFF_NORMED);
        // 定义一个阈值来判定匹配是否成功,这个阈值需要根据实际情况进行调整
        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
        return mmr;
    }

    public static void captureScreenRegion(String filename, FishType type) {
        try {
            // 创建Robot实例
            Robot robot = new Robot();
            // 使用指定的屏幕区域进行截图
            BufferedImage capturedImage = null;
            if (type == FishType.START) {
                Rectangle startScreenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
                capturedImage = robot.createScreenCapture(startScreenRect);
            } else if (type == FishType.JUDGE) {
                if (floatCoordinate == null) {
                    return;
                }
                Rectangle screenRect = new Rectangle(floatCoordinate.x - 300, floatCoordinate.y - 300, 570, 570); // x, y, width, height
                capturedImage = robot.createScreenCapture(screenRect);
            }
            // 将截图保存到文件
            File imgFile = new File(filename);
            if (!imgFile.exists()) {
                // 如果目录不存在，则创建它
                boolean success = imgFile.mkdirs();
                if (!success) {
                    throw new RuntimeException("Failed to create directory: " + filename);
                }
            }
            ImageIO.write(capturedImage, "jpg", imgFile);
        } catch (AWTException | IOException ex) {
            ex.printStackTrace();
        }
    }
}

enum FishType {
    JUDGE(0, "judge"),
    START(1, "start");
    int code;
    String name;

    FishType(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}

enum JudgeType {
    BySound(0, "BySound"), ByImg(1, "ByImg");
    int code;
    String name;

    JudgeType(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
