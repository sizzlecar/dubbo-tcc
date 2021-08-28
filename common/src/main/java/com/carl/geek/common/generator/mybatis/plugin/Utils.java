package com.carl.geek.common.generator.mybatis.plugin;

import org.mybatis.generator.api.GeneratedFile;
import org.mybatis.generator.exception.ShellException;
import org.mybatis.generator.internal.DefaultShellCallback;

import java.io.File;

/**
 * @author carl.che
 */
public class Utils {

    /**
     * 检查将要生成的文件是否已存在
     *
     * @param gxf java 文件
     * @return 存在: true, 不存在: false
     */
    public static boolean checkFileExist(GeneratedFile gxf) {
        DefaultShellCallback shellCallback = new DefaultShellCallback(true);
        File directory;
        try {
            directory = shellCallback.getDirectory(gxf.getTargetProject(), gxf.getTargetPackage());
        } catch (ShellException e) {
            throw new RuntimeException(e);
        }
        File targetFile = new File(directory, gxf.getFileName());
        return targetFile.exists();
    }
}
