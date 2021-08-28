package com.carl.geek.common.generator;

import org.mybatis.generator.api.MyBatisGenerator;
import org.mybatis.generator.config.Configuration;
import org.mybatis.generator.config.xml.ConfigurationParser;
import org.mybatis.generator.internal.DefaultShellCallback;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Generator {


    public static void generate() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("conf/dao-config.xml");
        for (Resource resource : resources) {
            generate1(resource.getFile()).forEach(System.out::println);
        }
    }



    private static List<String> generate1(File configurationFile) throws Exception {
        List<String> warnings = new ArrayList<>();
        ConfigurationParser cp = new ConfigurationParser(warnings);

        Configuration config;

        try {
            config = cp.parseConfiguration(configurationFile);
        } catch (Exception e) {
            throw new IllegalStateException(String.format("转化 XML %s 文件出现异常！", configurationFile.getAbsolutePath()), e);
        }

        DefaultShellCallback shellCallback = new DefaultShellCallback(true);
        MyBatisGenerator myBatisGenerator = new MyBatisGenerator(config, shellCallback, warnings);
        myBatisGenerator.generate(null);
        return warnings;
    }

    public static void main(String[] args) throws Exception{
        Generator.generate();
    }
}
