package com.fishing;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
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
    public static ExecutorService executor = new ThreadPoolExecutor(3, 3, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10), new FinshThreadFactory());
    public static Rectangle screenRect = new Rectangle(990, 20, 1700, 1200); // x, y, width, height
    // 设定要截取的屏幕区域
    public static LinkedList<Mat> mats = new LinkedList<>();
    public static Map<Integer, Integer> levelMap = new LinkedHashMap<>();
    private static long start = System.currentTimeMillis();
    public static final String filePath = "E:\\wowTempImg\\";
    public static int findFloatCount = 1;
    public static int loopCount = 100;
    public static long tenMinutes = 600000l;
    public static long executeHours = 2;
    public static Mat cutImtemplateImg;


    static {
        new Mat();
        // 调用OpenCV库文件
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        Mat cutImtemplateImg3 = Imgcodecs.imread("D:\\Develop-Application\\Idea-projects\\wow-fishing\\src\\main\\resources\\floatsuccess3.png");
        cutImtemplateImg = Imgcodecs.imread("D:\\Develop-Application\\Idea-projects\\wow-fishing\\src\\main\\resources\\floatstart3.png");
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
        System.out.println("init() loopCount: " + loopCount);

        Thread.sleep(1000);
        keyClick(robot, KeyEvent.VK_Q);
        Thread.sleep(3000);

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
            System.out.println("======================开始第" + loopCount + "次======================");
            //设置鼠标初始坐标位置
            mouseInitCoordinate(robot);
            //甩干
            mouseClick(robot, InputEvent.BUTTON1_DOWN_MASK);
            Thread.sleep(1700);
            //甩鱼钩后截图
            castingRodFile(loopCount);
            //截图和对比float.png，将鼠标移动到鱼漂位置
            Thread.sleep(500);
            boolean b = goToFloat(robot, loopCount, 5);
            if (!b) {
                continue;
            }
            System.out.println("judge fishing");
            Thread.sleep(2000);
            // 这里应加入图像识别逻辑来判断何时收杆
            boolean isRiseToTheBait = isRiseToTheBait(loopCount);
            // 模拟右键点击（收杆）
            if (isRiseToTheBait) {
                mouseClick(robot, InputEvent.BUTTON3_DOWN_MASK);
            } else {
                mouseClick(robot, InputEvent.BUTTON3_DOWN_MASK);
            }
            calcTime(robot);
            int s = new Random().nextInt(4000) + 1000;
            Thread.sleep(s);
            System.out.println("======================结束第" + loopCount + "次======================\r\n");
        }
    }


    /**
     * 每十分钟按一次q，清理截图文件目录
     *
     * @param robot
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
        System.out.println("press key -> Q:" + keyCode);
        robot.keyPress(keyCode);
        robot.delay(100);
        robot.keyRelease(keyCode);
    }


    private static boolean cleanFiles(String path) throws IOException {
        Path rootPath = Paths.get(filePath);
        // 检查路径是否存在
        if (!Files.exists(rootPath)) {
            // 尝试创建目录，包括所有不存在的父目录
            try {
                Files.createDirectories(rootPath);
                System.out.println(rootPath + " 已被创建。");
            } catch (IOException e) {
                System.err.println("创建目录时发生错误: " + e.getMessage());
            }
        } else if (Files.isDirectory(rootPath)) {
            System.out.println(rootPath + " 已经存在。");
        } else {
            // 这种情况不太可能发生，除非filePath不是一个目录而是一个文件的路径
            System.out.println(rootPath + " 存在，但它不是一个目录。");
        }
        Files.walkFileTree(rootPath,
                new SimpleFileVisitor<Path>() {
                    // 先去遍历删除文件
                    @Override
                    public FileVisitResult visitFile(Path file,
                                                     BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        System.out.printf("cleanFiles() 文件被删除 : %s%n", file);
                        return FileVisitResult.CONTINUE;
                    }

                    // 再去遍历删除目录
                    @Override
                    public FileVisitResult postVisitDirectory(Path dir,
                                                              IOException exc) throws IOException {
                        Files.delete(dir);
                        System.out.printf("cleanFiles() 文件夹被删除: %s%n", dir);
                        return FileVisitResult.CONTINUE;
                    }
                }
        );
        return false;
    }

    private static void castingRodFile(int loopCount) {
        String name = FishType.START.getName() + loopCount + "fishing";
        String filename = filePath + loopCount + "\\" + name + System.currentTimeMillis() + ".png";
        captureScreenRegion(filename, FishType.START);
    }

    /**
     * 判断是否上钩
     *
     * @param loopCount
     */
    private static boolean isRiseToTheBait(int loopCount) throws Exception {
        String type = "judge";
        int count = 40;
        // 截图，循环和float.png做图像对比进行判断
        int total = 0;
        while (count >= 0) {
            String filename = filePath + loopCount + "\\" + type + count + ".png";
            captureScreenRegion(filename, FishType.JUDGE);
            Thread.sleep(50);
            Mat fishingImg = Imgcodecs.imread(filename);
            Future<Boolean> future = executor.submit(new FishIsSuccessTask(filename, count, mats));
            count--;
            if (future.get()) {
                return true;
            }
        }
        return false;
    }

    private static boolean goToFloat(Robot robot, int loopCount, int count) throws Exception {
        if (count <= 0) {
            return false;
        }
        Thread.sleep(100);
        Point floatCoordinate = compareWithFloat(loopCount, FishType.START);
        count--;
        if (floatCoordinate == null) {
            System.out.println("没有找到鱼漂位置,重新定位" + (findFloatCount++));
            goToFloat(robot, loopCount, count);
        }
        //移动鼠标位置
        return moveMouse(robot, floatCoordinate);
    }

    private static void mouseClick(Robot robot, int type) {
        robot.mousePress(type);
        robot.delay(100);
        robot.mouseRelease(type);
    }

    public static void mouseInitCoordinate(Robot robot) {
        robot.mouseMove(1820, 490);
    }

    /**
     * 移动鼠标位置
     *
     * @param robot
     * @param floatCoordinate
     */
    private static boolean moveMouse(Robot robot, Point floatCoordinate) {
        if (floatCoordinate == null) {
            return false;
        }
        int x = (int) floatCoordinate.x;
        int y = (int) floatCoordinate.y;
        System.out.println("x:" + x + " y:" + y);
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
        LinkedList<Mat> mats = new LinkedList<>();
        for (File f : files) {
            String absolutePath = f.getAbsolutePath();
            System.out.println("\r\ncompare use move mouse:" + absolutePath);
            Mat cutImg = Imgcodecs.imread(absolutePath);
            Point matchedImg = detectObject(cutImg, cutImtemplateImg, type);
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
        System.out.println("detectObject() minVal:" + minVal);
        if (minVal < 0.022) {
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
            System.out.printf("captureScreenRegion() %s的截图\r\n", type);
        } catch (AWTException | IOException ex) {
            System.err.println(ex);
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

