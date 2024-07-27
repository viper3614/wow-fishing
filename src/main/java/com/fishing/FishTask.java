package com.fishing;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.Callable;

/**
 * @author viper
 * @create 2024-07-26-0:22
 */
public class FishTask implements Callable<Boolean> {
    private String filepath;

    public FishTask(String filepath) {
        this.filepath = filepath;
    }

    public String getFilepath() {
        return filepath;
    }

    @Override
    public Boolean call() throws Exception {
        Path rootPath = Paths.get(this.getFilepath());
        Files.walkFileTree(rootPath,
                new SimpleFileVisitor<>() {
                    // 先去遍历删除文件
                    @Override
                    public FileVisitResult visitFile(Path file,
                                                     BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        System.out.printf("文件被删除 : %s%n", file);
                        return FileVisitResult.CONTINUE;
                    }

                    // 再去遍历删除目录
                    @Override
                    public FileVisitResult postVisitDirectory(Path dir,
                                                              IOException exc) throws IOException {
                        Files.delete(dir);
                        System.out.printf("文件夹被删除: %s%n", dir);
                        return FileVisitResult.CONTINUE;
                    }
                }
        );
        return false;
    }
}
