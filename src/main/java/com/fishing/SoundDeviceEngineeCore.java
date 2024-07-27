package com.fishing;

import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.*;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author admin
 */
@Slf4j
public class SoundDeviceEngineeCore {
    private final AtomicBoolean flag = new AtomicBoolean(true);
    private TargetDataLine targetDataLine;
    private AudioFormat audioFormat;
    /**
     * 监听钓鱼时长
     */
    private static final int timePastSecond = 16;
    private static final int dbthreshold = 75;

    public SoundDeviceEngineeCore() throws Exception {
        log.info("SoundDeviceEngineeCore init...");
        // 获取所有可用的Mixer
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        // 查找具有特定名称的Mixer
        Mixer mixer = findMixerByName(mixerInfos, "麦克风 (1080P USB Camera-Audio)");
        if (mixer == null) {
            log.info("未找到指定的音频设备.");
            throw new Exception("未找到指定的音频设备.");
        }
        audioFormat = getAudioFormat();
        DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
        // 检查线是否可用
        if (!AudioSystem.isLineSupported(dataLineInfo)) {
            log.info("不支持的音频格式.");
            throw new Exception("未找到指定的音频设备.");
        }
        // 获取并打开目标数据线
        targetDataLine = (TargetDataLine) mixer.getLine(dataLineInfo);
        targetDataLine.open(audioFormat);
    }

    public static void main(String args[]) throws Exception {
        boolean flag = true;
        SoundDeviceEngineeCore engineeCore = new SoundDeviceEngineeCore();
        while (flag) {
            try {
                flag = engineeCore.startRecognize();
            } catch (Exception e) {
                log.error("Error in sound recognition", e);
                try {
                    Thread.sleep(1000); // 重试间隔
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt(); // 恢复中断状态
                }
            }
        }
    }

    private void stopRecognize() {
        flag.set(false);
        if (targetDataLine != null) {
            log.info("stopRecognize");
            targetDataLine.stop();
            targetDataLine.close();
        }
    }

    public boolean startRecognize() throws Exception {
        targetDataLine.start();
        flag.set(true);
        byte[] fragment = new byte[4096];
        Date date = new Date();
        int count = 0;
        try {
            while (flag.get()) {
                if (count > 4) {
                    break;
                }
                Date newDate = timePastSecond(date);
                if (newDate != null && newDate.before(new Date())) {
                    flag.set(false);
                    log.info("监听声音超过{}s,停止监听", timePastSecond);
                    break;
                }
                int read = targetDataLine.read(fragment, 0, fragment.length);
                double db = bytesToDecibels(fragment, read);
                if (db > dbthreshold) {
                    log.info("检测到声音,当前声音分贝:{}", db);
                    count++;
                }
            }
        } finally {
            stopRecognize();
        }
        return false;
    }

    private static AudioFormat getAudioFormat() {
        return new AudioFormat(44100, 16, 1, true, false);
    }

    private static Mixer findMixerByName(Mixer.Info[] mixerInfos, String name) {
        for (Mixer.Info info : mixerInfos) {
            if (info.getName().equals(name)) {
                return AudioSystem.getMixer(info);
            }
        }
        return null;
    }

    private static double bytesToDecibels(byte[] bytes, int length) {
        // 将字节数据转换为短整型数组
        short[] shorts = new short[length / 2];
        for (int i = 0; i < length; i += 2) {
            shorts[i / 2] = (short) (((bytes[i + 1] & 0xFF) << 8) | (bytes[i] & 0xFF));
        }
        // 计算RMS
        double sumSquared = 0;
        for (int i = 0; i < shorts.length; i++) {
            sumSquared += (double) shorts[i] * (double) shorts[i];
        }
        double rms = Math.sqrt(sumSquared / shorts.length);
        // 转换为分贝
        return 20 * Math.log10(rms);
    }

    public static Date timePastSecond(Date date) {
        try {
            Calendar newTime = Calendar.getInstance();
            newTime.setTime(date);
            // 根据 forcedStop 的值调整
            newTime.add(Calendar.SECOND, timePastSecond);
            return newTime.getTime();
        } catch (Exception ex) {
            log.error("Error calculating future date", ex);
            return null;
        }
    }
}
