package com.shopguide.config;

import com.shopguide.utils.AliyunOSSUtil;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 存储配置类
 *
 * 职责：
 *   1. @EnableConfigurationProperties 让 StorageProperties 生效（把 yml 的 storage.* 读进对象）；
 *   2. 用 @Bean 方法 + 有参构造函数 new AliyunOSSUtil(properties)，
 *      把配置对象注入给工具类，工具类内部不再使用 @Value 逐项读取。
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    /**
     * 创建 OSS 上传工具类 Bean
     * 参数 storageProperties 由 Spring 自动注入（即 yml 中 storage 段的内容）
     */
    @Bean
    public AliyunOSSUtil aliyunOSSUtil(StorageProperties storageProperties) {
        return new AliyunOSSUtil(storageProperties);
    }
}
