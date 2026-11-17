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
         * 中文 Windows 的 JVM 默认字符集是 GBK，而 LangChain4j 的 JDK HTTP 客户端在读取
         * DeepSeek 的 SSE 流时用的是 InputStreamReader 默认字符集，会把模型返回的中文
         * （尤其是工具参数，如“生活用品”）解码成乱码。这里在 Spring 容器启动前强制 UTF-8。
         */
        if (!"UTF-8".equalsIgnoreCase(System.getProperty("file.encoding"))) {
            System.setProperty("file.encoding", "UTF-8");
        }
        SpringApplication.run(ShopGuideApplication.class, args);
        System.out.println("===== 智能电商导购平台后端启动成功 =====");
        System.out.println("当前 JVM 默认字符集：" + Charset.defaultCharset());
    }
}
