package com.shopguide;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.charset.Charset;

/**
 * 智能电商导购平台 启动类
 */
@SpringBootApplication
@MapperScan("com.shopguide.mapper")
public class ShopGuideApplication {
    public static void main(String[] args) {
        /*
         * 中文 Windows 的 JVM 默认字符集是 GBK，容易导致中文请求/响应乱码。
         * 这里在 Spring 容器启动前统一强制 UTF-8，保证商品名、分类名等中文不乱码。
         */
        if (!"UTF-8".equalsIgnoreCase(System.getProperty("file.encoding"))) {
            System.setProperty("file.encoding", "UTF-8");
        }
        SpringApplication.run(ShopGuideApplication.class, args);
        System.out.println("===== 智能电商导购平台后端启动成功 =====");
        System.out.println("当前 JVM 默认字符集：" + Charset.defaultCharset());
    }
}
